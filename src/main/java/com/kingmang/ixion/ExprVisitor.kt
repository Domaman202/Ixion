package com.kingmang.ixion

import com.kingmang.ixion.ast.*

interface ExprVisitor<R> {
    fun visitAssignExpr(expression: AssignExpression?): R
    fun visitBad(expression: BadExpression?): R
    fun visitBinaryExpr(expression: BinaryExpression?): R
    fun visitCall(expression: CallExpression?): R
    fun visitEmpty(expression: EmptyExpression?): R
    fun visitEmptyList(expression: EmptyListExpression?): R
    fun visitGroupingExpr(expression: GroupingExpression?): R
    fun visitIdentifierExpr(expression: IdentifierExpression?): R
    fun visitIndexAccess(expression: IndexAccessExpression?): R
    fun visitLiteralExpr(expression: LiteralExpression?): R
    fun visitLiteralList(expression: LiteralListExpression?): R
    fun visitModuleAccess(expression: ModuleAccessExpression?): R
    fun visitPostfixExpr(expression: PostfixExpression?): R
    fun visitPrefix(expr: PrefixExpression?): R
    fun visitPropertyAccess(expression: PropertyAccessExpression?): R
    fun visitLambda(expression: LambdaExpression): R
    fun visitEnumAccess(expression: EnumAccessExpression?): R
}