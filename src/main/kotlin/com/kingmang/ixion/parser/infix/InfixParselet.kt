package com.kingmang.ixion.parser.infix

import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.parser.Parser

interface InfixParselet {
    fun parse(parser: Parser, left: Expression, token: Token): Expression

    val precedence: Int
}
