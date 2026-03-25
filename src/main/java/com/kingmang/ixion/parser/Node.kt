package com.kingmang.ixion.parser

import com.kingmang.ixion.lexer.Position

interface Node {
    val position: Position?
}