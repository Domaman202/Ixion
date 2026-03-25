package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.runtime.IxType

class CaseStatement(
    pos: Position?,
    val expression: Expression,
    val cases: MutableMap<TypeStatement, Pair<String, BlockStatement>>
) : Statement(pos) {
    val types: MutableMap<TypeStatement?, IxType?> = HashMap()

    override fun <R> accept(visitor: StatementVisitor<R>): R {
        return visitor.visitMatch(this)
    }
}