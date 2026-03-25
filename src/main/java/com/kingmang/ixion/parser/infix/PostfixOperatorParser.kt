package com.kingmang.ixion.parser.infix

import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.ast.PostfixExpression
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.parser.Parser

data class PostfixOperatorParser(override val precedence: Int) : InfixParselet {
    override fun parse(parser: Parser, left: Expression, token: Token): Expression {
        val pos = parser.pos
        return PostfixExpression(pos, left, token)
    }
}