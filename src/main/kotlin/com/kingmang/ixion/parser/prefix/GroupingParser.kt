package com.kingmang.ixion.parser.prefix

import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.ast.GroupingExpression
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.lexer.TokenType
import com.kingmang.ixion.parser.Parser

class GroupingParser : PrefixParselet {
    override fun parse(parser: Parser, token: Token): Expression {
        val pos = parser.pos
        val expression = parser.expression()
        parser.consume(TokenType.RPAREN, "Expected opening parentheses.")
        return GroupingExpression(pos, expression)
    }
}