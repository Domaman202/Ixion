package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position

class EnumAccessExpression(pos: Position?, @JvmField val enumType: Expression?, @JvmField val enumValue: IdentifierExpression?) : Expression(pos) {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitEnumAccess(this)
    }
}