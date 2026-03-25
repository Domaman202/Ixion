package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.runtime.IxType

class PropertyAccessExpression(
    pos: Position?,
    val expression: Expression,
    val identifiers: MutableList<IdentifierExpression>
) : Expression(pos) {
    var typeChain: MutableList<IxType?> = ArrayList()

    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitPropertyAccess(this)
    }
}