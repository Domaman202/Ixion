package com.kingmang.ixion.parser

object Precedence {
    const val ASSIGNMENT: Int = 1
    const val XOR: Int = 2
    const val OR: Int = 3
    const val AND: Int = 4
    const val COMPARISON: Int = 5
    const val SUM: Int = 6
    const val PRODUCT: Int = 7
    const val EXPONENT: Int = 8
    const val PREFIX: Int = 9
    const val POSTFIX: Int = 10
    const val PRIMARY: Int = 11
}
