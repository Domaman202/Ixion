package com.kingmang.ixion.parser

object ReservedWords {
    val reserved: MutableSet<String?> = HashSet()

    init {
        reserved.add("int")
        reserved.add("float")
        reserved.add("bool")
        reserved.add("string")
        reserved.add("def")
        reserved.add("struct")
    }

    fun isReserved(word: String?): Boolean {
        return reserved.contains(word)
    }
}
