package com.kingmang.ixion.parser.infix

import com.kingmang.ixion.ast.AssignExpression
import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.parser.Parser
import com.kingmang.ixion.parser.Precedence

class AssignOperatorParser : InfixParselet {
    override fun parse(parser: Parser, left: Expression, token: Token): Expression {
        val pos = parser.pos
        val right = parser.expression(precedence - 1)
        return AssignExpression(pos, left, right)
    }

    override val precedence: Int
        get() = Precedence.ASSIGNMENT
}