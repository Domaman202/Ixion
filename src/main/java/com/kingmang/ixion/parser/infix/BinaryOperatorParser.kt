package com.kingmang.ixion.parser.infix

import com.kingmang.ixion.ast.BinaryExpression
import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.parser.Parser

data class BinaryOperatorParser(override val precedence: Int, val isRight: Boolean) : InfixParselet {
    override fun parse(parser: Parser, left: Expression, token: Token): Expression {
        val pos = parser.pos
        val right = parser.expression(precedence - (if (isRight) 1 else 0))
        return BinaryExpression(pos, left, token, right)
    }
}