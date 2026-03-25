package com.kingmang.ixion;

import com.kingmang.ixion.ast.*;
import org.jetbrains.annotations.NotNull;

public interface ExprVisitor<R> {
    @NotNull R visitAssignExpr(AssignExpression expression);
    @NotNull R visitBad(BadExpression expression);
    @NotNull R visitBinaryExpr(BinaryExpression expression);
    @NotNull R visitCall(CallExpression expression);
    @NotNull R visitEmpty(EmptyExpression expression);
    @NotNull R visitEmptyList(EmptyListExpression expression);
    @NotNull R visitGroupingExpr(GroupingExpression expression);
    @NotNull R visitIdentifierExpr(IdentifierExpression expression);
    @NotNull R visitIndexAccess(IndexAccessExpression expression);
    @NotNull R visitLiteralExpr(LiteralExpression expression);
    @NotNull R visitLiteralList(LiteralListExpression expression);
    @NotNull R visitModuleAccess(ModuleAccessExpression expression);
    @NotNull R visitPostfixExpr(PostfixExpression expression);
    @NotNull R visitPrefix(PrefixExpression expr);
    @NotNull R visitPropertyAccess(PropertyAccessExpression expression);
    @NotNull R visitLambda(@NotNull LambdaExpression expression);

    R visitEnumAccess(EnumAccessExpression expression);
}
