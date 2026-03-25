package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.lexer.Position
import java.util.*

class UnionTypeStatement(
    pos: Position?,
    val types: MutableList<TypeStatement>
) : TypeStatement(pos, null, Optional.empty(), true) {
    override fun <R> accept(visitor: StatementVisitor<R>): R {
        return visitor.visitUnionType(this)
    }
}