package com.kingmang.ixion.parser.prefix

import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.ast.LiteralExpression
import com.kingmang.ixion.ast.LiteralListExpression
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.lexer.TokenType
import com.kingmang.ixion.parser.Parser

data class LiteralParser(val isList: Boolean) : PrefixParselet {
    override fun parse(parser: Parser, token: Token): Expression {
        val pos = parser.pos
        if (token.type == TokenType.LBRACK) {
            val args = ArrayList<Expression>()
            if (!parser.match(TokenType.RBRACK)) {
                do {
                    args.add(parser.expression())
                } while (parser.match(TokenType.COMMA))
                parser.optional(TokenType.COMMA)
                parser.consume(TokenType.RBRACK, "Expected closing ']' after list literal.")
            }
            return LiteralListExpression(pos, args)
        }

        if (token.type == TokenType.CHAR) {
            val source = token.source
            val charValue = source!![0]
            return LiteralExpression(pos, Token(TokenType.CHAR, token.line, token.col, charValue.toString()))
        }

        return LiteralExpression(pos, token)
    }
}