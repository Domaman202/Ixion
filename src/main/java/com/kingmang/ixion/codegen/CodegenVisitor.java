package com.kingmang.ixion.codegen;

import com.kingmang.ixion.Visitor;
import com.kingmang.ixion.api.Context;
import com.kingmang.ixion.api.IxApi;
import com.kingmang.ixion.api.IxFile;
import com.kingmang.ixion.api.IxionConstant;
import com.kingmang.ixion.ast.*;
import com.kingmang.ixion.exception.IdentifierNotFoundException;
import com.kingmang.ixion.exception.ImplementationException;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.lexer.TokenType;
import com.kingmang.ixion.runtime.*;
import com.kingmang.ixion.typechecker.TypeResolver;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

public class CodegenVisitor implements Visitor<Optional<ClassWriter>> {
    public final static int flags = ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS;
    public final static int CLASS_VERSION = 61;
    public final Context rootContext;
    public final IxFile source;
    public final File file;
    public final IxApi api;
    public final ClassWriter cw;
    public final Map<StructType, ClassWriter> structWriters = new HashMap<>();
    private final Stack<DefType> functionStack = new Stack<>();

    public Context currentContext;

    public CodegenVisitor(IxApi api, Context rootEnvironment, IxFile source, ClassWriter cw) {
        this.api = api;
        this.rootContext = rootEnvironment;
        this.source = source;
        this.currentContext = this.rootContext;
        this.cw = cw;
        this.file = source.file;
    }

    @NotNull
    @Override
    public Optional<ClassWriter> visit(Statement stmt) {
        return stmt.accept(this);
    }

    /**
     * Обрабатывает объявление псевдонима типа.
     * Поскольку псевдонимы не требуют генерации байт-кода, метод возвращает пустой Optional.
     * @param stmt Выражение псевдонима типа
     * @return Пустой Optional
     */
    @NotNull
    @Override
    public Optional<ClassWriter> visitTypeAlias(TypeAliasStatement stmt) {
        return Optional.empty();
    }

    /**
     * Генерирует байт-код для операции присваивания значения переменной или полю объекта.
     * Для идентификаторов сохраняет значение в локальную переменную, для доступа к полям объекта - использует putField.
     * @param expr Выражение присваивания
     * @return Пустой Optional
     */
    @NotNull
    @Override
    public Optional<ClassWriter> visitAssignExpr(AssignExpression expr) {
        var funcType = functionStack.peek();
        var ga = funcType.getGa();

        if (expr.left instanceof IdentifierExpression id) {
            expr.right.accept(this);
            var index = funcType.getLocalMap().get(id.identifier.source());
            ga.storeLocal(index);
        } else if (expr.left instanceof PropertyAccessExpression pa) {
            var lType = expr.left.getRealType();
            var rType = expr.right.getRealType();

            var root = pa.expression;
            root.accept(this);

            var typeChain = pa.typeChain;
            var identifiers = pa.identifiers;
            for (int i = 0; i < typeChain.size() - 2; i++) {
                var current = typeChain.get(i);
                var next = typeChain.get(i + 1);
                var fieldName = identifiers.get(i).identifier.source();
                ga.getField(Type.getType(current.getDescriptor()), fieldName, Type.getType(next.getDescriptor()));
            }

            expr.right.accept(this);

            if (rType instanceof BuiltInType btArg) {
                if (lType instanceof UnionType) {
                    btArg.doBoxing(ga);
                }
            }

            ga.putField(Type.getType(typeChain.get(typeChain.size() - 2).getDescriptor()),
                    identifiers.get(identifiers.size() - 1).identifier.source(),
                    Type.getType(typeChain.get(typeChain.size() - 1).getDescriptor()));

        } else {
            new ImplementationException().send(api, file, expr, "Assignment not implemented for any recipient but identifier yet");
        }
        return Optional.empty();
    }

    /**
     * Обрабатывает некорректное выражение. Возвращает пустой результат, так как такое выражение не должно встречаться на этапе генерации кода.
     * @param expr Некорректное выражение
     * @return Пустой Optional
     */
    @NotNull
    @Override
    public Optional<ClassWriter> visitBad(BadExpression expr) {
        return Optional.empty();
    }

    /**
     * Генерирует байт-код для бинарных операций, включая арифметические, сравнения, логические операции и конкатенацию строк.
     * Для строк используется StringBuilder, для логических операций - short-circuit evaluation.
     * @param expr Бинарное выражение
     * @return Пустой Optional
     */
    @NotNull
    @Override
    public Optional<ClassWriter> visitBinaryExpr(BinaryExpression expr) {
        var funcType = functionStack.peek();
        var ga = funcType.getGa();
        var left = expr.left;
        var right = expr.right;
        if (expr.getRealType().equals(BuiltInType.STRING)) {
            ga.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
            ga.visitInsn(Opcodes.DUP);
            ga.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", IxionConstant.getInit(), "()V", false);

            expr.left.accept(this);

            String leftExprDescriptor = expr.left.getRealType().getDescriptor();
            String descriptor = "(" + leftExprDescriptor + ")Ljava/lang/StringBuilder;";
            ga.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", descriptor, false);

            expr.right.accept(this);

            String rightExprDescriptor = expr.right.getRealType().getDescriptor();
            descriptor = "(" + rightExprDescriptor + ")Ljava/lang/StringBuilder;";
            ga.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", descriptor, false);
            ga.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        } else {
            switch (expr.operator.type()) {
                case AND -> {
                    Label falseLabel = new Label();
                    Label successLabel = new Label();

                    left.accept(this);
                    ga.ifZCmp(GeneratorAdapter.EQ, falseLabel);

                    right.accept(this);
                    ga.ifZCmp(GeneratorAdapter.EQ, falseLabel);
                    ga.push(true);
                    ga.goTo(successLabel);

                    ga.mark(falseLabel);
                    ga.push(false);

                    ga.mark(successLabel);
                }
                case OR -> {
                    Label falseLabel = new Label();
                    Label successLabel = new Label();
                    Label endLabel = new Label();

                    left.accept(this);
                    ga.ifZCmp(GeneratorAdapter.NE, successLabel);

                    right.accept(this);
                    ga.ifZCmp(GeneratorAdapter.NE, successLabel);
                    ga.goTo(falseLabel);

                    ga.mark(successLabel);
                    ga.push(true);
                    ga.goTo(endLabel);

                    ga.mark(falseLabel);
                    ga.push(false);

                    ga.mark(endLabel);
                }
                case XOR -> {
                    left.accept(this);
                    right.accept(this);
                    ga.visitInsn(Opcodes.IXOR);
                }
                case EQUAL, NOTEQUAL, LT, GT, LE, GE -> {
                    var cmpType = castAndAccept(ga, left, right, this);

                    Label endLabel = new Label();
                    Label falseLabel = new Label();

                    int opcode = switch (expr.operator.type()) {
                        case EQUAL -> GeneratorAdapter.NE;
                        case NOTEQUAL -> GeneratorAdapter.EQ;
                        case LT -> GeneratorAdapter.GT;
                        case GT -> GeneratorAdapter.LT;
                        case LE -> GeneratorAdapter.GE;
                        case GE -> GeneratorAdapter.LE;
                        default -> throw new IllegalStateException("Unexpected value: " + expr.operator.type());
                    };
                    ga.ifCmp(cmpType, opcode, falseLabel);
                    ga.push(true);
                    ga.goTo(endLabel);

                    ga.mark(falseLabel);
                    ga.push(false);
                    ga.mark(endLabel);
                }
                case MOD -> {
                    var cmpType = castAndAccept(ga, left, right, this);

                    if (cmpType == Type.DOUBLE_TYPE) {
                        ga.visitInsn(Opcodes.DREM);
                    } else if (cmpType == Type.INT_TYPE) {
                        ga.visitInsn(Opcodes.IREM);
                    } else if (cmpType == Type.FLOAT_TYPE) {
                        ga.visitInsn(Opcodes.FREM);
                    }
                }
                case POW -> {
                }
                case ADD, SUB, MUL, DIV -> arithmetic(ga, left, right, expr.operator, expr.getRealType(), this);
                case null, default -> throw new IllegalStateException("Unexpected value: " + expr.operator.type());
            }

        }
        return Optional.empty();
    }

    /**
     * Обрабатывает блок операторов, посещая каждый оператор в блоке последовательно.
     * @param block Блок операторов
     * @return Пустой Optional
     */
    @NotNull
    @Override
    public Optional<ClassWriter> visitBlockStmt(BlockStatement block) {
        for (var stmt : block.statements) stmt.accept(this);
        return Optional.empty();
    }

    /**
     * Генерирует байт-код для вызова функции или создания экземпляра структуры.
     * Обрабатывает как встроенные (glue) функции, так и пользовательские, включая специализацию дженериков.
     * @param expr Выражение вызова
     * @return Пустой Optional
     */
    @NotNull
    @Override
    public Optional<ClassWriter> visitCall(CallExpression expr) {

        var funcType = functionStack.peek();

        if (expr.item instanceof IdentifierExpression identifier) {
            expr.item.setRealType(currentContext.getVariable(identifier.identifier.source()));
        }

        if (expr.item.getRealType() instanceof DefType callType) {
            if (callType.glue) {
                String owner = callType.owner;
                String name = callType.name;
                if (callType.isPrefixed) name = "_" + name;

                var params = callType.parameters.stream().map(arg -> Pair.with(arg.getValue1().getName(), arg.getValue1())).collect(Collectors.toList());

                IxType returnType = callType.returnType;
                String methodDescriptor = CollectionUtil.getMethodDescriptor(params, returnType);

                CollectionUtil.zip(params, expr.arguments, (param, arg) -> {
                    arg.accept(this);
                    if (arg.getRealType() != null && arg.getRealType() instanceof BuiltInType btArg) {
                        if (param.getValue1() instanceof ExternalType et && et.foundClass.equals(Object.class)) {
                            btArg.doBoxing(funcType.getGa());
                        }
                    }
                });

                funcType.getGa().visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, methodDescriptor, false);
            } else {
                var ga = funcType.getGa();
                CollectionUtil.zip(callType.parameters, expr.arguments, (param, arg) -> {
                    arg.accept(this);
                    if (arg.getRealType() != null && arg.getRealType() instanceof BuiltInType btArg) {
                        switch (param.getValue1()) {
                            case ExternalType et when et.foundClass.equals(Object.class) -> btArg.doBoxing(ga);
                            case UnionType ut -> btArg.doBoxing(funcType.getGa());
                            default -> {
                            }
                        }
                    }
                });

                var specialization = callType.buildSpecialization(expr.arguments);
                var returnType = callType.returnType;
                if (returnType instanceof GenericType gt) {
                    returnType = DefType.Companion.getSpecializedType(specialization, gt.key());

                }

                var parameters = callType.buildParametersFromSpecialization(specialization);

                String descriptor = CollectionUtil.getMethodDescriptor(parameters, returnType);

                String methodDescriptor = CollectionUtil.getMethodDescriptor(callType.parameters, callType.returnType);
                methodDescriptor = descriptor;
                String name = "_" + callType.name;
                String owner = FilenameUtils.removeExtension(source.getFullRelativePath());

                if (callType.external != null) {
                    owner = callType.external.getFullRelativePath();
                }

                funcType.getGa().visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, methodDescriptor, false);
            }

        } else if (expr.item.getRealType() instanceof StructType st) {
            var ga = funcType.getGa();

            ga.newInstance(Type.getType("L" + st.qualifiedName + ";"));
            ga.visitInsn(Opcodes.DUP);

            StringBuilder typeDescriptor = new StringBuilder();
            CollectionUtil.zip(st.parameters, expr.arguments, (param, arg) -> {
                arg.accept(this);
                var paramType = param.getValue1();
                if (paramType instanceof UnionType ut || paramType instanceof GenericType) {
                    typeDescriptor.append(paramType.getDescriptor());
                    if (arg.getRealType() instanceof BuiltInType btArg) {
                        btArg.doBoxing(ga);
                    }

                } else {
                    typeDescriptor.append(arg.getRealType().getDescriptor());
                }

            });

            ga.invokeConstructor(Type.getType("L" + st.qualifiedName + ";"), new Method(IxionConstant.getInit(), "(" + typeDescriptor + ")V"));


        } else {
            System.err.println("Bad!");
            System.exit(43);
        }

        return Optional.empty();
    }

    /**
     * Обрабатывает пустое выражение. Не генерирует байт-код.
     * @param empty Пустое выражение
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitEmpty(EmptyExpression empty) {
        return Optional.empty();
    }

    /**
     * Генерирует байт-код для создания пустого списка, используя конструктор ArrayList.
     * @param emptyList Выражение пустого списка
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitEmptyList(EmptyListExpression emptyList) {
        var ga = functionStack.peek().getGa();

        ga.newInstance(IxionConstant.getArrayListType());
        ga.dup();

        ga.invokeConstructor(IxionConstant.getArrayListType(), new Method(IxionConstant.getInit(), "()V"));
        return Optional.empty();
    }

    /**
     * Обрабатывает объявление перечисления. В данный момент не реализовано.
     * @param stmt Оператор перечисления
     * @return Выбрасывает NotImplementedException
     */
    @Override
    public Optional<ClassWriter> visitEnum(EnumStatement stmt) {
        throw new NotImplementedException("method not implemented");
    }

    /**
     * Обрабатывает экспорт оператора, посещая внутренний оператор.
     * @param stmt Оператор экспорта
     * @return Результат посещения внутреннего оператора
     */
    @Override
    public Optional<ClassWriter> visitExport(ExportStatement stmt) {
        return stmt.stmt.accept(this);
    }

    /**
     * Генерирует байт-код для выражения, используемого как оператор.
     * @param stmt Оператор выражения
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitExpressionStmt(ExpressionStatement stmt) {
        stmt.expression.accept(this);
        return Optional.empty();
    }

    /**
     * Генерирует байт-код для цикла for. Поддерживает итерацию по идентификатору или вызову, возвращающему итератор.
     * @param stmt Оператор цикла for
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitFor(ForStatement stmt) {
        var funcType = functionStack.peek();
        var ga = functionStack.peek().getGa();
        currentContext = stmt.block.context;
        var startLabel = new Label();
        var endLabel = new Label();

        if (stmt.expression instanceof IdentifierExpression id) {
            ga.mark(startLabel);
            stmt.expression.accept(this);

        } else {
            stmt.expression.accept(this);
            stmt.setLocalExprIndex(ga.newLocal(IxionConstant.getIteratorType()));
            funcType.getLocalMap().put("______", stmt.getLocalExprIndex());
            ga.storeLocal(stmt.getLocalExprIndex(), IxionConstant.getIteratorType());
            ga.mark(startLabel);
            ga.loadLocal(stmt.getLocalExprIndex());
        }

        ga.invokeInterface(IxionConstant.getIteratorType(), new Method("hasNext", "()Z"));
        ga.visitJumpInsn(Opcodes.IFEQ, endLabel);

        if (stmt.expression instanceof IdentifierExpression id) {
            stmt.expression.accept(this);
        } else {
            ga.loadLocal(stmt.getLocalExprIndex());
        }
        ga.invokeInterface(IxionConstant.getIteratorType(), new Method("next", "()Ljava/lang/Object;"));
        BuiltInType.INT.doUnboxing(ga);
        stmt.setLocalExprIndex(ga.newLocal(Type.getType(BuiltInType.INT.getDescriptor())));
        funcType.getLocalMap().put(stmt.name.source(), stmt.getLocalExprIndex());
        funcType.getGa().storeLocal(stmt.getLocalExprIndex(), Type.getType(BuiltInType.INT.getDescriptor()));

        stmt.block.accept(this);

        ga.goTo(startLabel);
        ga.mark(endLabel);
        currentContext = currentContext.parent;
        return Optional.empty();
    }

    /**
     * Генерирует байт-код для объявления функции, включая специализации для дженериков.
     * Создает метод в текущем ClassWriter с соответствующим дескриптором и генерирует тело функции.
     * @param stmt Оператор объявления функции
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitFunctionStmt(DefStatement stmt) {
        var funcType = currentContext.getVariableTyped(stmt.name.source(), DefType.class);
        functionStack.add(funcType);
        var childEnvironment = stmt.body.context;
        String name = "_" + funcType.name;
        var access = Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC;

        if (funcType.hasGenerics()) {

            for (Map<String, IxType> specialization : funcType.specializations) {
                funcType.setCurrentSpecialization(specialization);
                var returnType = funcType.returnType;
                if (returnType instanceof GenericType gt) {
                    returnType = DefType.Companion.getSpecializedType(specialization, gt.key());
                }

                var parameters = funcType.buildParametersFromSpecialization(specialization);

                String descriptor = CollectionUtil.getMethodDescriptor(parameters, returnType);

                var mv = cw.visitMethod(access, name, descriptor, null, null);
                funcType.setGa(new GeneratorAdapter(mv, access, name, descriptor));
                for (int i = 0; i < funcType.parameters.size(); i++) {
                    var param = funcType.parameters.get(i);
                    funcType.getArgMap().put(param.getValue0(), i);
                }

                currentContext = childEnvironment;
                stmt.body.accept(this);
                funcType.getGa().endMethod();
                currentContext = currentContext.parent;
            }
            functionStack.pop();
        } else {

            String descriptor = CollectionUtil.getMethodDescriptor(funcType.parameters, funcType.returnType);
            if (funcType.name.equals("main")) {
                name = "main";
                descriptor = "([Ljava/lang/String;)V";
            }
            var mv = cw.visitMethod(access, name, descriptor, null, null);
            funcType.setGa(new GeneratorAdapter(mv, access, name, descriptor));
            for (int i = 0; i < funcType.parameters.size(); i++) {
                var param = funcType.parameters.get(i);
                funcType.getArgMap().put(param.getValue0(), i);
            }

            currentContext = childEnvironment;
            stmt.body.accept(this);
            funcType.getGa().endMethod();
            currentContext = currentContext.parent;
            functionStack.pop();

        }
        return Optional.empty();
    }

    /**
     * Обрабатывает группирующее выражение, посещая внутреннее выражение.
     * @param expr Группирующее выражение
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitGroupingExpr(GroupingExpression expr) {
        expr.expression.accept(this);
        return Optional.empty();
    }

    /**
     * Генерирует байт-код для загрузки значения идентификатора из локальной переменной или аргумента метода.
     * @param expr Выражение идентификатора
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitIdentifierExpr(IdentifierExpression expr) {
        var funcType = functionStack.peek();
        var ga = funcType.getGa();
        var type = currentContext.getVariable(expr.identifier.source());

        if (type instanceof GenericType gt) {
            type = funcType.getCurrentSpecialization().get(gt.key());
        }

        expr.setRealType(type);

        int index;
        String source = expr.identifier.source();
        if (funcType.getLocalMap().containsKey(source)) {
            index = funcType.getLocalMap().get(source);
            ga.loadLocal(index, Type.getType(type.getDescriptor()));
        } else {
            index = funcType.getArgMap().getOrDefault(source, -1);
            if (index == -1) {
                new IdentifierNotFoundException().send(api, file, expr, source);
                return Optional.empty();
            }
            ga.loadArg(index);
        }

        return Optional.empty();
    }

    /**
     * Генерирует байт-код для условного оператора if, включая ветку else если она присутствует.
     * @param stmt Оператор if
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitIf(IfStatement stmt) {
        var funcType = functionStack.peek();
        var ga = funcType.getGa();
        Label endLabel = new Label();
        Label falseLabel = new Label();

        stmt.condition.accept(this);

        currentContext = stmt.trueBlock.context;
        ga.ifZCmp(GeneratorAdapter.EQ, falseLabel);
        stmt.trueBlock.accept(this);
        ga.goTo(endLabel);
        ga.mark(falseLabel);
        if (stmt.falseStatement != null) stmt.falseStatement.accept(this);
        ga.mark(endLabel);
        currentContext = currentContext.parent;
        return Optional.empty();
    }

    /**
     * Обрабатывает оператор использования (use). Не требует генерации байт-кода.
     * @param stmt Оператор use
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitUse(UseStatement stmt) {
        return Optional.empty();
    }

    /**
     * Обрабатывает доступ по индексу. В данный момент не реализовано.
     * @param expr Выражение доступа по индексу
     * @return Выбрасывает NotImplementedException
     */
    @Override
    public Optional<ClassWriter> visitIndexAccess(IndexAccessExpression expr) {
        throw new NotImplementedException("method not implemented");
    }

    /**
     * Генерирует байт-код для литералов встроенных типов (int, float, double, boolean, string).
     * @param expr Выражение литерала
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitLiteralExpr(LiteralExpression expr) {
        if (expr.getRealType() instanceof BuiltInType bt) {
            var transformed = TypeResolver.getValueFromString(expr.literal.source(), BuiltInType.Companion.getFromToken(expr.literal.type()));
            var ga = functionStack.peek().getGa();
            switch (bt) {
                case INT -> ga.push((int) transformed);
                case FLOAT -> ga.push((float) transformed);
                case DOUBLE -> ga.push((double) transformed);
                case BOOLEAN -> ga.push((boolean) transformed);
                case STRING -> ga.push((String) transformed);
            }
        } else {
            new ImplementationException().send(api, source.file, expr, "This should never happen. All literals should be builtin, for now.");
        }
        return Optional.empty();
    }

    /**
     * Генерирует байт-код для создания списка литералов с помощью ListWrapper.
     * Каждый элемент добавляется через метод add ArrayList.
     * @param expr Выражение списка литералов
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitLiteralList(LiteralListExpression expr) {
        var ga = functionStack.peek().getGa();

        ga.newInstance(IxionConstant.getListWrapperType());
        ga.dup();
        ga.push(((ListType) expr.getRealType()).contentType().getName());
        ga.invokeConstructor(IxionConstant.getListWrapperType(), new Method(IxionConstant.getInit(), "(Ljava/lang/String;)V"));
        ga.dup();
        ga.invokeVirtual(IxionConstant.getListWrapperType(), new Method("list", "()Ljava/util/ArrayList;"));

        for (var entry : expr.entries) {
            ga.dup();

            entry.accept(this);
            var rt = entry.getRealType();
            if (rt instanceof BuiltInType bt) {
                bt.doBoxing(ga);
            }
            ga.invokeVirtual(IxionConstant.getArrayListType(), new Method("add", "(Ljava/lang/Object;)Z"));
            ga.pop();
        }
        ga.pop();
        return Optional.empty();
    }

    /**
     * Генерирует байт-код для оператора match, преобразуя его в последовательность проверок instanceof и условных переходов.
     * Поддерживает сопоставление с типами ListType, BuiltInType и StructType.
     * @param casee Оператор match
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitMatch(final CaseStatement casee) {

        var funcType = functionStack.peek();
        var ga = funcType.getGa();

        casee.expression.accept(this);
        int localExprIndex = ga.newLocal(IxionConstant.getObjectType());
        var s = "bruh";
        funcType.getLocalMap().put(s, localExprIndex);
        ga.storeLocal(localExprIndex);

        for (TypeStatement typeStmt : casee.cases.keySet()) {
            var pair = casee.cases.get(typeStmt);
            var scopedName = pair.getValue0();
            var block = pair.getValue1();
            var t = casee.types.get(typeStmt);
            if (t instanceof UnknownType ukt) {
                var attempt = currentContext.getVariable(ukt.typeName);
                if (attempt != null) {
                    t = attempt;
                }
            }

            var end = new Label();
            ga.loadLocal(localExprIndex);

            if (t instanceof ListType lt) {
                ga.instanceOf(IxionConstant.getListWrapperType());
                ga.visitJumpInsn(Opcodes.IFEQ, end);
                ga.loadLocal(localExprIndex);
                ga.checkCast(IxionConstant.getListWrapperType());
                ga.storeLocal(localExprIndex);

                ga.loadLocal(localExprIndex);
                funcType.getGa().invokeVirtual(IxionConstant.getListWrapperType(), new Method("name", "()Ljava/lang/String;"));
                ga.push(lt.contentType().getName());
                funcType.getGa().invokeVirtual(Type.getType(String.class), new Method("equals", "(Ljava/lang/Object;)Z"));
                ga.visitJumpInsn(Opcodes.IFEQ, end);
                ga.loadLocal(localExprIndex);

            } else {
                Type typeClass;
                if (t instanceof BuiltInType bt) {
                    typeClass = Type.getType(t.getTypeClass());
                } else if (t instanceof StructType st) {
                    typeClass = Type.getType("L" + st.qualifiedName + ";");
                } else {
                    typeClass = Type.getType(t.getDescriptor());
                }
                ga.instanceOf(typeClass);
                ga.visitJumpInsn(Opcodes.IFEQ, end);
                ga.loadLocal(localExprIndex);
                ga.checkCast(typeClass);
            }

            if (t instanceof BuiltInType bt && bt.isNumeric()) {
                bt.unboxNoCheck(ga);

                int localPrimitiveType = ga.newLocal(Type.getType(CollectionUtil.convert(bt.getTypeClass())));
                ga.storeLocal(localPrimitiveType);
                funcType.getLocalMap().put(scopedName, localPrimitiveType);
            } else {
                int localObjectType = ga.newLocal(IxionConstant.getObjectType());
                ga.storeLocal(localObjectType);
                funcType.getLocalMap().put(scopedName, localObjectType);
            }

            currentContext = block.context;
            block.accept(this);
            currentContext = currentContext.parent;

            ga.mark(end);
        }

        return Optional.empty();
    }

    /**
     * Обрабатывает доступ к модулю. Не требует генерации байт-кода.
     * @param expr Выражение доступа к модулю
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitModuleAccess(ModuleAccessExpression expr) {
        return Optional.empty();
    }

    /**
     * Обрабатывает оператор параметра. В данный момент не реализовано.
     * @param stmt Оператор параметра
     * @return Выбрасывает NotImplementedException
     */
    @Override
    public Optional<ClassWriter> visitParameterStmt(ParameterStatement stmt) {

        throw new NotImplementedException("method not implemented");
    }

    /**
     * Генерирует байт-код для постфиксных операций (инкремент/декремент) над переменными встроенных типов.
     * @param expr Постфиксное выражение
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitPostfixExpr(PostfixExpression expr) {

        var ga = functionStack.peek().getGa();
        expr.expression.accept(this);
        if (expr.getRealType() instanceof BuiltInType bt) {
            bt.pushOne(ga);
            int op = switch (expr.operator.type()) {
                case PLUSPLUS -> bt.getAddOpcode();
                case MINUSMINUS -> bt.getSubtractOpcode();
                default -> throw new IllegalStateException("Unexpected value: " + expr.operator.type());
            };

            ga.visitInsn(op);
            if (expr.expression instanceof IdentifierExpression eid) {

                ga.storeLocal(functionStack.peek().getLocalMap().get(eid.identifier.source()));
            }
        } else {
            IxApi.Companion.exit("postfix only works with builtin types", 49);
        }
        return Optional.empty();
    }

    /**
     * Генерирует байт-код для префиксных операций. В настоящее время поддерживает только унарный минус для числовых типов.
     * @param expr Префиксное выражение
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitPrefix(PrefixExpression expr) {
        var ga = functionStack.peek().getGa();

        expr.right.accept(this);

        var t = expr.right.getRealType();
        if (expr.operator.type() == TokenType.SUB && t instanceof BuiltInType bt) {
            ga.visitInsn(bt.getNegOpcode());
            expr.setRealType(t);
        }
        return Optional.empty();
    }

    /**
     * Генерирует байт-код для доступа к полям объекта, включая поддержку монотипизированных структур.
     * @param expr Выражение доступа к свойству
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitPropertyAccess(PropertyAccessExpression expr) {
        var ga = functionStack.peek().getGa();
        var t = expr.getRealType();

        var root = expr.expression;
        var rootType = root.getRealType();

        if (rootType instanceof MonomorphizedStruct mst) {

            root.accept(this);

            for (int i = 0; i < expr.typeChain.size() - 1; i++) {
                var current = expr.typeChain.get(i);
                var next = expr.typeChain.get(i + 1);

                var key = ((GenericType) next).key();

                var r = mst.resolved.get(key);

                var fieldName = expr.identifiers.get(i).identifier.source();
                ga.getField(Type.getType(current.getDescriptor()), fieldName, Type.getType(next.getDescriptor()));

                ga.checkCast(Type.getType(r.getDescriptor()));


            }
        } else {

            root.accept(this);

            for (int i = 0; i < expr.typeChain.size() - 1; i++) {
                var current = expr.typeChain.get(i);
                var next = expr.typeChain.get(i + 1);
                var fieldName = expr.identifiers.get(i).identifier.source();
                ga.getField(Type.getType(current.getDescriptor()), fieldName, Type.getType(next.getDescriptor()));


            }
        }

        return Optional.empty();
    }

    /**
     * Обрабатывает лямбда-выражение. В данный момент не реализовано.
     * @param expression Лямбда-выражение
     * @return Пустой Optional
     */
    @NotNull
    @Override
    public Optional<ClassWriter> visitLambda(@NotNull LambdaExpression expression) {
        return Optional.empty();
    }

    /**
     * Обрабатывает доступ к перечислению. В данный момент не реализовано.
     * @param expression Выражение доступа к перечислению
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitEnumAccess(EnumAccessExpression expression) {
        return Optional.empty();
    }

    /**
     * Генерирует байт-код для оператора return, включая упаковку примитивных значений для функций с UnionType.
     * @param stmt Оператор return
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitReturnStmt(ReturnStatement stmt) {
        var funcType = functionStack.peek();
        if (!(stmt.expression instanceof EmptyExpression)) {
            stmt.expression.accept(this);

            if (funcType.returnType instanceof UnionType && stmt.expression.getRealType() instanceof BuiltInType bt) {
                bt.doBoxing(funcType.getGa());
            }

            var returnType = funcType.returnType;
            if (returnType instanceof GenericType gt) {
                returnType = DefType.Companion.getSpecializedType(funcType.getCurrentSpecialization(), gt.key());
            }

            funcType.getGa().visitInsn(returnType.getReturnOpcode());
        } else {
            funcType.getGa().visitInsn(Opcodes.RETURN);
        }
        return Optional.empty();
    }

    /**
     * Генерирует байт-код для объявления структуры: создает внутренний класс с полями, конструктором и методом toString.
     * @param struct Оператор структуры
     * @return Optional с ClassWriter внутреннего класса
     */
    @Override
    public Optional<ClassWriter> visitStruct(StructStatement struct) {
        var innerCw = new ClassWriter(CodegenVisitor.flags);
        var structType = currentContext.getVariableTyped(struct.name.source(), StructType.class);

        String name = structType.name;
        String innerName = source.getFullRelativePath() + "$" + name;

        innerCw.visit(CodegenVisitor.CLASS_VERSION, IxionConstant.getPublicStatic(), innerName, null, "java/lang/Object", null);
        cw.visitInnerClass(innerName, source.getFullRelativePath(), name, IxionConstant.getPublicStatic());
        innerCw.visitOuterClass(source.getFullRelativePath(), name, "()V");

        StringBuilder constructorDescriptor = new StringBuilder();

        for (var pair : structType.parameters) {
            IxType type = pair.getValue1();
            var descriptor = type.getDescriptor();
            String n = pair.getValue0();

            var fieldVisitor = innerCw.visitField(Opcodes.ACC_PUBLIC, n, descriptor, null, null);
            fieldVisitor.visitEnd();

            constructorDescriptor.append(descriptor);
        }

        var descriptor = "(" + constructorDescriptor + ")V";
        MethodVisitor _mv = innerCw.visitMethod(Opcodes.ACC_PUBLIC, IxionConstant.getInit(), descriptor, null, null);
        var ga = new GeneratorAdapter(_mv, Opcodes.ACC_PUBLIC, IxionConstant.getInit(), descriptor);

        String ownerInternalName = source.getFullRelativePath() + "$" + name;

        ga.loadThis();
        ga.invokeConstructor(IxionConstant.getObjectType(), new Method(IxionConstant.getInit(), "()V"));

        for (int i = 0; i < structType.parameters.size(); i++) {
            IxType type = structType.parameters.get(i).getValue1();
            descriptor = type.getDescriptor();
            String n = structType.parameters.get(i).getValue0();
            ga.visitVarInsn(Opcodes.ALOAD, 0);
            ga.loadArg(i);

            ga.visitFieldInsn(Opcodes.PUTFIELD, ownerInternalName, n, descriptor);
        }

        ga.returnValue();
        ga.endMethod();

        BytecodeGenerator.addToString(innerCw, structType, constructorDescriptor.toString(), ownerInternalName);

        structWriters.put(structType, innerCw);

        return Optional.of(innerCw);
    }

    /**
     * Обрабатывает оператор типа. В данный момент не реализовано.
     * @param statement Оператор типа
     * @return Выбрасывает NotImplementedException
     */
    @Override
    public Optional<ClassWriter> visitTypeAlias(TypeStatement statement) {
        throw new NotImplementedException("method not implemented");
    }

    /**
     * Обрабатывает объявление union-типа. Не требует генерации байт-кода.
     * @param unionTypeStmt Оператор union-типа
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitUnionType(UnionTypeStatement unionTypeStmt) {
        return Optional.empty();
    }

    /**
     * Генерирует байт-код для объявления переменной: вычисляет значение выражения и сохраняет в локальную переменную.
     * @param stmt Оператор переменной
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitVariable(VariableStatement stmt) {
        var funcType = functionStack.peek();
        stmt.expression.accept(this);

        var type = currentContext.getVariable(stmt.identifier());
        if (type instanceof GenericType gt) {
            type = funcType.getCurrentSpecialization().get(gt.key());
        }
        stmt.setLocalIndex(funcType.getGa().newLocal(Type.getType(type.getDescriptor())));
        funcType.getLocalMap().put(stmt.identifier(), stmt.getLocalIndex());
        funcType.getGa().storeLocal(stmt.getLocalIndex(), Type.getType(type.getDescriptor()));

        if (stmt.expression instanceof PostfixExpression pe) {
            pe.setLocalIndex(stmt.getLocalIndex());
        }

        return Optional.empty();
    }

    /**
     * Генерирует байт-код для цикла while с проверкой условия в начале каждой итерации.
     * @param w Оператор while
     * @return Пустой Optional
     */
    @Override
    public Optional<ClassWriter> visitWhile(WhileStatement w) {

        var funcType = functionStack.peek();
        var ga = funcType.getGa();
        Label endLabel = new Label();
        Label startLabel = new Label();

        ga.mark(startLabel);
        w.condition.accept(this);

        currentContext = w.block.context;
        ga.ifZCmp(GeneratorAdapter.EQ, endLabel);
        w.block.accept(this);
        ga.goTo(startLabel);
        ga.mark(endLabel);
        currentContext = currentContext.parent;
        return Optional.empty();
    }

    /**
     * Приводит два выражения к общему типу и генерирует код для их вычисления.
     * Используется для операций сравнения и арифметики.
     * @param ga GeneratorAdapter для генерации байт-кода
     * @param left Левое выражение
     * @param right Правое выражение
     * @param visitor Посетитель для рекурсивного посещения выражений
     * @return Тип, к которому были приведены выражения
     */
    private static Type castAndAccept(GeneratorAdapter ga, Expression left, Expression right, CodegenVisitor visitor) {
        int lWide = BuiltInType.Companion.getWidenings().getOrDefault((BuiltInType) left.getRealType(), -1);
        int rWide = BuiltInType.Companion.getWidenings().getOrDefault((BuiltInType) right.getRealType(), -1);
        var lType = Type.getType(left.getRealType().getDescriptor());
        var rType = Type.getType(right.getRealType().getDescriptor());

        var cmpType = lType;

        if (lWide != -1 && rWide != -1) {
            if (lWide > rWide) {
                left.accept(visitor);
                right.accept(visitor);
                ga.cast(rType, lType);
            } else if (lWide < rWide) {
                left.accept(visitor);
                ga.cast(lType, rType);
                right.accept(visitor);
                cmpType = rType;
            } else {
                left.accept(visitor);
                right.accept(visitor);
            }
        } else {
            left.accept(visitor);
            right.accept(visitor);
        }
        return cmpType;
    }

    /**
     * Генерирует байт-код для арифметических операций, включая приведение типов при необходимости.
     * @param ga GeneratorAdapter для генерации байт-кода
     * @param left Левое выражение
     * @param right Правое выражение
     * @param operator Токен оператора
     * @param goalType Ожидаемый тип результата
     * @param visitor Посетитель для рекурсивного посещения выражений
     */
    public static void arithmetic(GeneratorAdapter ga, Expression left, Expression right, Token operator, IxType goalType, CodegenVisitor visitor) {
        if (left.getRealType().equals(right.getRealType())) {
            left.accept(visitor);
            right.accept(visitor);
        } else {
            if (left.getRealType() == BuiltInType.INT && right.getRealType() == BuiltInType.FLOAT) {
                left.accept(visitor);
                ga.visitInsn(Opcodes.I2F);
                right.accept(visitor);
                goalType = BuiltInType.FLOAT;
            } else if (left.getRealType() == BuiltInType.FLOAT && right.getRealType() == BuiltInType.INT) {
                left.accept(visitor);
                right.accept(visitor);
                ga.visitInsn(Opcodes.I2F);
                goalType = BuiltInType.FLOAT;
            } else if (left.getRealType() == BuiltInType.INT && right.getRealType() == BuiltInType.DOUBLE) {
                left.accept(visitor);
                ga.visitInsn(Opcodes.I2D);
                right.accept(visitor);
                goalType = BuiltInType.DOUBLE;
            } else if (left.getRealType() == BuiltInType.DOUBLE && right.getRealType() == BuiltInType.INT) {
                left.accept(visitor);
                right.accept(visitor);
                ga.visitInsn(Opcodes.I2D);
                goalType = BuiltInType.DOUBLE;
            }

        }
        if (goalType instanceof BuiltInType bt) {

            int op = switch (operator.type()) {
                case ADD -> bt.getAddOpcode();
                case SUB -> bt.getSubtractOpcode();
                case MUL -> bt.getMultiplyOpcode();
                case DIV -> bt.getDivideOpcode();
                case LT, GT, LE, GE -> 0;
                default -> throw new IllegalStateException("Unexpected value: " + operator.type());
            };
            ga.visitInsn(op);
        } else {
            IxApi.exit("need a test case here", 452);
        }
    }
}