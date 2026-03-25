package com.kingmang.ixion.runtime

data class GenericType(val key: String) : IxType {
    override val defaultValue: Any?
        get() = null

    override val descriptor: String
        get() = "Ljava/lang/Object;"

    override val internalName: String
        get() = key

    override val loadVariableOpcode: Int
        get() = throw IllegalCallerException("Don't use ALOAD on Generic Types directly. Specialization required.")

    override val name: String?
        get() = null

    override val returnOpcode: Int
        get() = throw IllegalCallerException("Don't use ARETURN on Generic Types directly. Specialization required.")

    override val typeClass: Class<*>
        get() = Any::class.java

    override val isNumeric: Boolean
        get() = false

    override fun kind(): String? {
        return null
    }
}
