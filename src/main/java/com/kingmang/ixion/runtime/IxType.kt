package com.kingmang.ixion.runtime

interface IxType {
    val defaultValue: Any?
    val descriptor: String?
    val internalName: String?
    val loadVariableOpcode: Int
    val name: String?
    val returnOpcode: Int
    val typeClass: Class<*>?
    val isNumeric: Boolean

    fun kind(): String?
}
