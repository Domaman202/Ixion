package com.kingmang.ixion.parser.prefix

import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.parser.Parser

interface PrefixParselet {
    fun parse(parser: Parser, token: Token): Expression
}
