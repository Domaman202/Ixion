package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.parser.Node

abstract class Statement protected constructor(override val position: Position?) : Node {
    abstract fun <R> accept(visitor: StatementVisitor<R>): R

    interface TopLevel
}