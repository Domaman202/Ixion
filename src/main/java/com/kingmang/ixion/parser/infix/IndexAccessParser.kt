package com.kingmang.ixion.parser.infix

import com.kingmang.ixion.ast.CallExpression
import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.ast.IdentifierExpression
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.lexer.TokenType
import com.kingmang.ixion.parser.Parser
import com.kingmang.ixion.parser.Precedence

class IndexAccessParser : InfixParselet {
    override fun parse(parser: Parser, left: Expression, token: Token): Expression {
        val pos = parser.pos
        val right = parser.expression(precedence)
        parser.consume(TokenType.RBRACK, "Expected ']' after index access.")

        val arguments = ArrayList<Expression?>()
        if (left is IdentifierExpression) {
            arguments.add(left)
            arguments.add(right)
        }
        return CallExpression(
            pos,
            IdentifierExpression(pos, Token(TokenType.IDENTIFIER, pos.line, pos.col, "at")),
            arguments
        )
    }

    override val precedence: Int
        get() = Precedence.POSTFIX
}