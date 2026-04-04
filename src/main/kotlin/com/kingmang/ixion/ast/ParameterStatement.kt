package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token

class ParameterStatement(
    pos: Position?,
    val name: Token,
    val type: TypeStatement?
) : Statement(pos) {
    override fun <R> accept(visitor: StatementVisitor<R>): R {
        return visitor.visitParameterStmt(this)
    }
}