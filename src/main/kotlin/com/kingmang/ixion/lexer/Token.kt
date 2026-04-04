package com.kingmang.ixion.lexer

import org.apache.commons.text.StringEscapeUtils

data class Token(val type: TokenType, val line: Int, val col: Int, val source: String?) {
    val representation: String?
        get() = type.representation

    override fun toString(): String {
        return type.name + "{" + StringEscapeUtils.escapeJava(source) + "}@" + line + ":" + col
    }
}