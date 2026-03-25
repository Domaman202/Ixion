package com.kingmang.ixion.typechecker;

import com.kingmang.ixion.Visitor;
import com.kingmang.ixion.api.Context;
import com.kingmang.ixion.api.IxApi;
import com.kingmang.ixion.api.IxFile;
import com.kingmang.ixion.ast.*;
import com.kingmang.ixion.exception.*;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.TokenType;
import com.kingmang.ixion.runtime.*;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Visitor for type checking and validation of AST nodes
 * Ensures type safety and resolves type information throughout the program
 */
public class TypeCheckVisitor implements Visitor<Optional<IxType>> {

    public final Context rootContext;
    public final File file;
    public final IxApi ixApi;
    private final Stack<DefType> functionStack = new Stack<>();
    public Context currentContext;

    /**
     * @param ixApi The API instance for error reporting
     * @param rootContext The root context for variable resolution
     * @param ixFile The source file being type checked
     */
    public TypeCheckVisitor(IxApi ixApi, Context rootContext, IxFile ixFile) {
        this.rootContext = rootContext;
        this.file = ixFile.file;
        this.currentContext = this.rootContext;
        this.ixApi = ixApi;
    }

    @Override
    public Optional<IxType> visit(Statement stmt) {
        return stmt.accept(this);
    }

    /**
     * @param statement Type alias statement to process
     * @return Empty optional as type aliases don't produce values
     */
    @Override
    public Optional<IxType> visitTypeAlias(TypeAliasStatement statement) {
        var a = currentContext.getVariable(statement.identifier.getSource());

        var resolvedTypes = new HashSet<IxType>();
        if (a instanceof UnionType ut) {
            extractedMethodForUnions(resolvedTypes, ut, statement);

            currentContext.setVariableType(statement.identifier(), ut);
        }

        return Optional.empty();
    }

    /**
     * @param expr Assignment expression to type check
     * @return Empty optional as assignments don't produce values
     */
    @NotNull
    @Override
    public Optional<IxType> visitAssignExpr(AssignExpression expr) {
        expr.left.accept(this);
        expr.right.accept(this);

        switch (expr.left) {
            case IdentifierExpression id -> {
                if (expr.left.getRealType() != expr.right.getRealType()) {
                    new BadAssignmentException().send(ixApi, file, expr, id.identifier.getSource());
                }
            }
            case PropertyAccessExpression pa -> {
                var lType = expr.left.getRealType();
                var rType = expr.right.getRealType();
                if (!TypeResolver.typesMatch(lType, rType)) {
                    new ParameterTypeMismatchException().send(ixApi, file, expr.left, rType.getName());
                }

            }
            default -> throw new IllegalStateException("Unexpected value: " + expr.left.getRealType());
        }

        return Optional.empty();
    }

    @NotNull
    @Override
    public Optional<IxType> visitBad(BadExpression expr) {
        return Optional.empty();
    }

    /**
     * @param expr Binary expression to type check
     * @return Optional containing the result type of the binary operation
     */
    @NotNull
    @Override
    public Optional<IxType> visitBinaryExpr(BinaryExpression expr) {
        var t1 = expr.left.accept(this);
        var t2 = expr.right.accept(this);

        if (t1.isEmpty() || t2.isEmpty()) {
            new ImplementationException().send(ixApi, file, expr, "Types in binary expression not determined.");
            return Optional.empty();
        }

        if (t1.get() == BuiltInType.ANY || t2.get() == BuiltInType.ANY) {
            new CannotApplyOperatorException().send(ixApi, file, expr, expr.operator.getSource());
            return Optional.empty();
        }

        var totalType = t1.get();

        switch (expr.operator.getType()) {
            case ADD, SUB, MUL, DIV, MOD -> {
                if (t1.get() instanceof BuiltInType bt1 && t2.get() instanceof BuiltInType bt2) {
                    totalType = BuiltInType.Companion.widen(bt1, bt2);
                } else {
                    new CannotApplyOperatorException().send(ixApi, file, expr, expr.operator.getSource());
                }
            }
            case EQUAL, NOTEQUAL, LT, GT, LE, GE -> {
                if (t1.get() instanceof BuiltInType bt1 && t2.get() instanceof BuiltInType bt2) {
                    if (expr.operator.getType() != TokenType.EQUAL && expr.operator.getType() != TokenType.NOTEQUAL) {
                        if (bt1 == BuiltInType.STRING || bt2 == BuiltInType.STRING) {
                            new CannotApplyOperatorException().send(ixApi, file, expr, expr.operator.getSource());
                        } else if (bt1 == BuiltInType.BOOLEAN || bt2 == BuiltInType.BOOLEAN) {
                            new CannotApplyOperatorException().send(ixApi, file, expr, expr.operator.getSource());

                        }
                    }
                    totalType = BuiltInType.BOOLEAN;
                }
            }
            case AND, OR, XOR -> {
                if (t1.get() instanceof BuiltInType bt1 && t2.get() instanceof BuiltInType bt2) {
                    if (bt1 != BuiltInType.BOOLEAN || bt2 != BuiltInType.BOOLEAN) {
                        new CannotApplyOperatorException().send(ixApi, file, expr, expr.operator.getSource());
                    }
                    totalType = BuiltInType.BOOLEAN;
                }
            }

            default -> {
            }
        }

        expr.left.setRealType(t1.get());
        expr.right.setRealType(t2.get());

        expr.setRealType(totalType);

        return Optional.of(totalType);
    }

    /**
     * @param statement Block statement to type check
     * @return Empty optional as blocks don't produce values
     */
    @Override
    public Optional<IxType> visitBlockStmt(BlockStatement statement) {
        for (var stmt : statement.statements) {
            stmt.accept(this);
        }
        return Optional.empty();
    }

    /**
     * @param expr Function call expression to type check
     * @return Optional containing the return type of the function call
     */
    @NotNull
    @Override
    public Optional<IxType> visitCall(CallExpression expr) {
        var e = expr.item.accept(this);
        if (e.isEmpty())
            IxApi.exit("Type checking failed to resolve function in ["
                    + expr.getPosition().getLine() + ":" + expr.getPosition().getCol()
                    + "]", 95);

        var t = e.orElseThrow();
        if (t instanceof StructType st) {
            if (st.getParameters().size() != expr.arguments.size()) {
                var params = st.getParameters().stream().map(s -> s.getSecond().getName()).collect(Collectors.joining(", "));
                new FunctionSignatureMismatchException().send(ixApi, file, expr.item, st.getName());
                return Optional.empty();
            }
            updateUnknownParameters(expr, st);

            CollectionUtil.zip(st.getParameters(), expr.arguments, (param, arg) -> {
                var at = arg.accept(this);
                at.ifPresent(type -> typecheckCallParameters(param, arg, type));
            });
        }

        if (t instanceof DefType ft) {
            var rt = ft.getReturnType();
            if (ft.hasGenerics()) {

                var specialization = ft.buildSpecialization(expr.arguments);

                ft.getSpecializations().add(specialization);

                if (rt instanceof GenericType) {
                    rt = specialization.get(((GenericType) rt).getKey());
                }
            }

            expr.setRealType(rt);

            return Optional.of(rt);
        } else if (t instanceof StructType structType) {
            expr.setRealType(structType);
            return Optional.of(structType);
        } else {
            new MethodNotFoundException().send(ixApi, file, expr.item, String.valueOf(expr.item));
        }

        return Optional.empty();
    }

    @NotNull
    @Override
    public Optional<IxType> visitEmpty(EmptyExpression empty) {
        return Optional.empty();
    }

    /**
     * @param emptyList Empty list expression to type check
     * @return Optional containing the type of the empty list
     */
    @NotNull
    @Override
    public Optional<IxType> visitEmptyList(EmptyListExpression emptyList) {
        if (emptyList.getRealType() != null) {
            return Optional.of(emptyList.getRealType());
        }
        if (!functionStack.isEmpty()) {
            var functionType = functionStack.peek();

            if (functionType.getReturnType() instanceof ListType) {
                emptyList.setRealType(functionType.getReturnType());
                return Optional.of(functionType.getReturnType());
            }
        }
        new TypeNotResolvedException().send(ixApi, file, emptyList, "Cannot determine type of empty list");
        return Optional.empty();
    }

    @Override
    public Optional<IxType> visitEnum(EnumStatement statement) {
        return Optional.empty();
    }

    /**
     * @param statement Export statement to process
     * @return Empty optional as exports don't produce values
     */
    @Override
    public Optional<IxType> visitExport(ExportStatement statement) {
        statement.stmt.accept(this);
        return Optional.empty();
    }

    /**
     * @param statement Expression statement to type check
     * @return Optional containing the type of the expression
     */
    @Override
    public Optional<IxType> visitExpressionStmt(ExpressionStatement statement) {
        return statement.expression.accept(this);
    }

    /**
     * @param statement For loop statement to type check
     * @return Empty optional as loops don't produce values
     */
    @Override
    public Optional<IxType> visitFor(ForStatement statement) {

        currentContext = statement.block.context;

        var b = statement.expression.accept(this);
        if (b.isPresent()) {
            switch (b.get()) {
                case ExternalType et -> {
                    if (et.getFoundClass().getName().equals("java.util.Iterator")) {
                        currentContext.setVariableType(statement.name.getSource(), BuiltInType.INT);
                    }
                }
                case ListType lt -> {
                    currentContext.setVariableType(statement.name.getSource(), lt.getContentType());
                }
                default -> new NotIterableException().send(ixApi, file, statement.expression, b.get().getName());
            }
        }

        statement.block.accept(this);

        currentContext = currentContext.parent;

        return Optional.empty();

    }

    /**
     * @param statement Function statement to type check
     * @return Empty optional as function definitions don't produce values
     */
    @Override
    public Optional<IxType> visitFunctionStmt(DefStatement statement) {

        var funcType = currentContext.getVariableTyped(statement.name.getSource(), DefType.class);
        if (funcType != null) {
            functionStack.add(funcType);
            var childEnvironment = statement.body.context;

            var parametersBefore = funcType.getParameters();
            var parametersAfter = new ArrayList<Pair<String, IxType>>();

            for (var param : parametersBefore) {
                if (param.getSecond() instanceof UnknownType ut) {
                    var attempt = currentContext.getVariable(ut.getTypeName());
                    if (attempt != null) {
                        childEnvironment.setVariableType(param.getFirst(), attempt);
                        var nt = new Pair<>(param.getFirst(), attempt);
                        parametersAfter.add(nt);
                    } else {
                        new IdentifierNotFoundException().send(ixApi, file, statement, ut.getTypeName());
                        parametersAfter.add(param);
                    }
                } else if (param.getSecond() instanceof UnionType ut) {
                    parametersAfter.add(param);
                    var resolvedTypes = new HashSet<IxType>();
                    extractedMethodForUnions(resolvedTypes, ut, statement);

                    currentContext.setVariableType(param.getFirst(), ut);
                } else {
                    parametersAfter.add(param);

                }
            }
            funcType.getParameters().clear();
            funcType.getParameters().addAll(parametersAfter);

            if (funcType.getReturnType() instanceof UnknownType ut) {
                var attempt = currentContext.getVariable(ut.getTypeName());
                if (attempt != null) {
                    funcType.setReturnType(attempt);
                } else {
                    new IdentifierNotFoundException().send(ixApi, file, statement, ut.getTypeName());
                }
            }

            currentContext = childEnvironment;

            statement.body.accept(this);

            if (!funcType.getHasReturn2()) {
                var returnStmt = new ReturnStatement(
                        new Position(0, 0),
                        new EmptyExpression(new Position(0, 0))
                );
                statement.body.statements.add(returnStmt);
            }

            currentContext = currentContext.parent;
            functionStack.pop();


        }
        return Optional.empty();
    }

    /**
     * @param expr Grouping expression to type check
     * @return Optional containing the type of the grouped expression
     */
    @NotNull
    @Override
    public Optional<IxType> visitGroupingExpr(GroupingExpression expr) {
        return expr.expression.accept(this);
    }

    /**
     * @param expr Identifier expression to resolve
     * @return Optional containing the type of the identifier
     */
    @NotNull
    @Override
    public Optional<IxType> visitIdentifierExpr(IdentifierExpression expr) {
        var t = currentContext.getVariable(expr.identifier.getSource());
        if (t != null) {
            if (t instanceof UnknownType ukt) {
                var attempt = currentContext.getVariable(ukt.getTypeName());
                if (attempt != null) {
                    t = attempt;
                }
            }
            expr.setRealType(t);
        } else {
            new IdentifierNotFoundException().send(ixApi, file, expr, expr.identifier.getSource());
        }
        return Optional.ofNullable(t);
    }

    /**
     * @param statement If statement to type check
     * @return Empty optional as if statements don't produce values
     */
    @Override
    public Optional<IxType> visitIf(IfStatement statement) {
        currentContext = statement.trueBlock.context;
        statement.condition.accept(this);
        statement.trueBlock.accept(this);
        if (statement.falseStatement != null) statement.falseStatement.accept(this);

        currentContext = currentContext.parent;
        return Optional.empty();
    }

    @Override
    public Optional<IxType> visitUse(UseStatement statement) {
        return Optional.empty();
    }

    @NotNull
    @Override
    public Optional<IxType> visitIndexAccess(IndexAccessExpression expr) {
        return Optional.empty();
    }

    /**
     * @param expr Literal expression to type check
     * @return Optional containing the type of the literal
     */
    @NotNull
    @Override
    public Optional<IxType> visitLiteralExpr(LiteralExpression expr) {
        var t = expr.getRealType();
        if (t == null) {
            new ImplementationException().send(ixApi, file, expr, "This should never happen. All literals should be builtin, for now.");
        }
        return Optional.ofNullable(t);
    }

    /**
     * @param expr List literal expression to type check
     * @return Optional containing the type of the list
     */
    @NotNull
    @Override
    public Optional<IxType> visitLiteralList(LiteralListExpression expr) {

        var firstType = expr.entries.get(0).accept(this);

        firstType.ifPresent(type -> {
            expr.setRealType(new ListType(type));

            for (int i = 0; i < expr.entries.size(); i++) {
                var t = expr.entries.get(i).accept(this);
                if (t.isPresent()) {
                    if (!(t.get().equals(type))) {
                        new ListTypeException().send(ixApi, file, expr.entries.get(i), t.get().getName());
                        break;
                    }
                }
            }
        });

        return Optional.of(expr.getRealType());
    }

    /**
     * @param statement Match statement to type check
     * @return Empty optional as match statements don't produce values
     */
    @Override
    public Optional<IxType> visitMatch(CaseStatement statement) {
        statement.expression.accept(this);

        if (statement.expression.getRealType() instanceof UnionType ut) {
            var typesToCover = new HashSet<>(ut.getTypes());
            statement.cases.forEach((keyTypeStmt, pair) -> {
                String id = pair.getValue0();
                BlockStatement block = pair.getValue1();
                var caseType = statement.types.get(keyTypeStmt);
                if (caseType instanceof UnknownType ukt) {
                    var attempt = currentContext.getVariable(ukt.getTypeName());
                    if (attempt != null) {
                        caseType = attempt;
                    }
                }

                typesToCover.remove(caseType);

                var childEnvironment = block.context;
                childEnvironment.parent = currentContext;
                childEnvironment.setVariableType(id, caseType);

                currentContext = childEnvironment;
                block.accept(this);
                currentContext = currentContext.parent;
            });
            if (!typesToCover.isEmpty()) {
                new MatchCoverageException().send(ixApi, file, statement, String.valueOf(ut));
            }


        } else {
            new TypeNotResolvedException().send(ixApi, file, statement.expression, "");
        }

        return Optional.empty();
    }

    @NotNull
    @Override
    public Optional<IxType> visitModuleAccess(ModuleAccessExpression expr) {
        expr.foreign.accept(this);
        return Optional.empty();
    }

    @Override
    public Optional<IxType> visitParameterStmt(ParameterStatement statement) {
        return Optional.empty();
    }

    /**
     * @param expr Postfix expression to type check
     * @return Empty optional as postfix expressions don't produce new values
     */
    @NotNull
    @Override
    public Optional<IxType> visitPostfixExpr(PostfixExpression expr) {
        expr.setRealType(expr.expression.accept(this).get());

        if (!(expr.getRealType() instanceof BuiltInType bt && bt.isNumeric())) {
            new CannotPostfixException().send(ixApi, file, expr.expression, expr.operator.getSource());
        }
        return Optional.empty();
    }

    /**
     * @param expr Prefix expression to type check
     * @return Optional containing the type of the prefixed expression
     */
    @NotNull
    @Override
    public Optional<IxType> visitPrefix(PrefixExpression expr) {
        return expr.right.accept(this);
    }

    /**
     * @param expr Property access expression to type check
     * @return Optional containing the type of the accessed property
     */
    @NotNull
    @Override
    public Optional<IxType> visitPropertyAccess(PropertyAccessExpression expr) {
        var t = expr.expression.accept(this);

        var typeChain = new ArrayList<IxType>();

        if (t.isPresent()) {
            var exprType = t.get();
            StructType pointer;
            IxType result = null;
            if (exprType instanceof MonomorphizedStruct mt) {
                pointer = mt.getStruct();
                typeChain.add(pointer);
                result = pointer;

                result = getTempMSTType(expr, typeChain, pointer, result);
                if (result instanceof GenericType gt) {
                    result = mt.getResolved().get(gt.getKey());
                }
            } else if (exprType instanceof StructType st) {
                pointer = st;
                typeChain.add(pointer);
                result = st;

                result = getTempMSTType(expr, typeChain, pointer, result);
            }
            expr.setRealType(result);
        } else {
            new MethodNotFoundException().send(ixApi, file, expr.expression, "ree");
        }
        expr.typeChain = typeChain;

        return Optional.ofNullable(expr.getRealType());
    }

    @NotNull
    @Override
    public Optional<IxType> visitLambda(@NotNull LambdaExpression expression) {
        return Optional.empty();
    }

    @NotNull
    @Override
    public Optional<IxType> visitEnumAccess(EnumAccessExpression expr) {
        return Optional.empty();
    }

    /**
     * @param statement Return statement to type check
     * @return Empty optional as return statements don't produce values
     */
    @Override
    public Optional<IxType> visitReturnStmt(ReturnStatement statement) {
        var t = statement.expression.accept(this);

        if (t.isPresent()) {
            if (!functionStack.isEmpty()) {
                var newType = t.get();
                var functionType = functionStack.peek();

                if (statement.expression instanceof EmptyListExpression && functionType.getReturnType() instanceof ListType) {
                    functionType.setHasReturn2(true);
                    return Optional.empty();
                }

                if (TypeResolver.typesMatch(functionType.getReturnType(), newType)) {
                }
                else if (functionType.getReturnType() instanceof UnionType ut) {
                    if (!ut.getTypes().contains(newType)) {
                        new ParameterTypeMismatchException().send(ixApi, file, statement.expression, String.valueOf(newType));
                    }
                }
                else if (functionType.getReturnType() == BuiltInType.VOID) {
                    if (newType != BuiltInType.VOID) {
                        new ReturnTypeMismatchException().send(ixApi, file, statement, functionType.getName());
                    }
                }
                else {
                    new ReturnTypeMismatchException().send(ixApi, file, statement, functionType.getName());
                }
            }
        }

        functionStack.peek().setHasReturn2(true);;
        return Optional.empty();
    }

    /**
     * @param statement Struct statement to type check
     * @return Empty optional as struct definitions don't produce values
     */
    @Override
    public Optional<IxType> visitStruct(StructStatement statement) {
        var structType = currentContext.getVariableTyped(statement.name.getSource(), StructType.class);
        if (structType != null) {
            var parametersAfter = new ArrayList<Pair<String, IxType>>();
            CollectionUtil.zip(statement.fields, structType.getParameters(), (a, b) -> {
                var bType = b.getSecond();
                if (bType instanceof UnknownType ut) {
                    var attempt = currentContext.getVariable(ut.getTypeName());
                    if (attempt != null) {
                        parametersAfter.add(new Pair<>(b.getFirst(), attempt));
                    } else if (structType.getGenerics().contains(ut.getTypeName())) {
                        parametersAfter.add(new Pair<>(b.getFirst(), new GenericType(ut.getTypeName())));
                    } else {
                        new IdentifierNotFoundException().send(ixApi, file, a, ut.getTypeName());
                        parametersAfter.add(b);
                    }
                } else {
                    parametersAfter.add(b);
                }
            });
            structType.getParameters().clear();
            structType.getParameters().addAll(parametersAfter);

        }

        return Optional.empty();
    }

    @Override
    public Optional<IxType> visitTypeAlias(TypeStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<IxType> visitUnionType(UnionTypeStatement statement) {
        return Optional.empty();
    }

    /**
     * @param statement Variable declaration statement to type check
     * @return Empty optional as variable declarations don't produce values
     */
    @Override
    public Optional<IxType> visitVariable(VariableStatement statement) {

        var expr = statement.expression;
        var t = expr.accept(this);

        if (t.isPresent()) {
            currentContext.setVariableType(statement.name.getSource(), t.get());
        } else {
            new TypeNotResolvedException().send(ixApi, file, expr, statement.name.getSource());
        }
        return Optional.empty();
    }

    /**
     * @param statement While loop statement to type check
     * @return Empty optional as loops don't produce values
     */
    @Override
    public Optional<IxType> visitWhile(WhileStatement statement) {
        var childEnvironment = statement.block.context;
        childEnvironment.parent = currentContext;

        currentContext = childEnvironment;
        statement.condition.accept(this);

        statement.block.accept(this);

        currentContext = currentContext.parent;
        return Optional.empty();
    }

    /**
     * Resolve unknown types within a union type
     * @param resolvedTypes Set to store resolved types
     * @param ut Union type containing potentially unknown types
     * @param node AST node for error reporting
     */
    private void extractedMethodForUnions(HashSet<IxType> resolvedTypes, UnionType ut, Statement node) {
        for (var type : ut.getTypes()) {
            if (type instanceof UnknownType ukt) {
                var attempt = currentContext.getVariable(ukt.getTypeName());
                if (attempt != null) {
                    resolvedTypes.add(attempt);
                } else {
                    new IdentifierNotFoundException().send(ixApi, file, node, ukt.getTypeName());
                }
            } else {
                resolvedTypes.add(type);
            }
        }
        ut.setTypes(resolvedTypes);
    }

    /**
     * Resolve property access chain types for struct types
     * @param expr Property access expression
     * @param typeChain Chain of types encountered during access
     * @param pointer Current struct type being accessed
     * @param result Current result type
     * @return Final resolved type after traversing the property chain
     */
    private IxType getTempMSTType(PropertyAccessExpression expr, ArrayList<IxType> typeChain, StructType pointer, IxType result) {
        for (IdentifierExpression identifier : expr.identifiers) {
            var foundField = pointer.getParameters().stream().filter(i -> i.getFirst().equals(identifier.identifier.getSource())).findAny();
            if (foundField.isPresent()) {
                var pointerCandidate = foundField.get().getSecond();
                if (pointerCandidate instanceof StructType pst) {
                    pointer = pst;
                    typeChain.add(pointer);
                    result = pointerCandidate;
                } else {
                    result = pointerCandidate;
                    typeChain.add(pointerCandidate);

                }

            } else {
                new FieldNotPresentException().send(ixApi, file, identifier, identifier.identifier.getSource());
                break;
            }
        }
        return result;
    }

    /**
     * Validate that function call arguments match parameter types
     * @param param Function parameter (name, type)
     * @param arg Argument expression
     * @param argType Resolved type of the argument
     */
    private void typecheckCallParameters(Pair<String, IxType> param, Expression arg, IxType argType) {
        if (argType == BuiltInType.VOID) {
            new VoidUsageException().send(ixApi, file, arg);
        }
        if (!TypeResolver.typesMatch(param.getSecond(), argType)) {
            new ParameterTypeMismatchException().send(ixApi, file, arg, argType.getName());
            TypeResolver.typesMatch(param.getSecond(), argType);
            arg.accept(this);
        } else {
            arg.setRealType(argType);
        }
    }

    /**
     * Update unknown types in function/struct parameters with resolved types
     * @param expr Function call expression
     * @param structType Struct or function type being called
     */
    private void updateUnknownParameters(CallExpression expr, StructType structType) {
        var parametersAfter = new ArrayList<Pair<String, IxType>>();
        CollectionUtil.zip(structType.getParameters(), expr.arguments, (param, arg) -> {
            if (param.getSecond() instanceof UnknownType ut) {
                var attempt = currentContext.getVariable(ut.getTypeName());
                if (attempt != null) {
                    parametersAfter.add(new Pair<>(param.getFirst(), attempt));
                } else {
                    new IdentifierNotFoundException().send(ixApi, file, arg, ut.getTypeName());
                    parametersAfter.add(param);
                }
            } else {
                parametersAfter.add(param);

            }
        });

        structType.getParameters().clear();
        structType.getParameters().addAll(parametersAfter);
    }
}