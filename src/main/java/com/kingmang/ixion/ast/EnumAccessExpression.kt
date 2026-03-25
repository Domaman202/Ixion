package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position

class EnumAccessExpression(
    pos: Position?,
    val enumType: Expression,
    val enumValue: IdentifierExpression
) : Expression(pos) {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitEnumAccess(this)
    }
}