package com.kingmang.ixion.parser.prefix

import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.ast.PrefixExpression
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.parser.Parser

@JvmRecord
data class PrefixOperatorParser(val precedence: Int) : PrefixParselet {
    override fun parse(parser: Parser, token: Token): Expression {
        val pos = parser.pos
        val right = parser.expression(this.precedence)

        return PrefixExpression(pos, token, right)
    }
}