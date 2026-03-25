package com.kingmang.ixion.runtime

import org.objectweb.asm.Opcodes

data class ExternalType(val foundClass: Class<*>?) : IxType {
    override val defaultValue: Any?
        get() = null

    override val descriptor: String?
        get() = foundClass!!.descriptorString()

    override val internalName: String
        get() = name.replace(".", "/")

    override val loadVariableOpcode: Int
        get() = Opcodes.ALOAD

    override val name: String
        get() = foundClass!!.getName()

    override val returnOpcode: Int
        get() = Opcodes.ARETURN

    override val typeClass: Class<*>?
        get() = foundClass

    override val isNumeric: Boolean
        get() = false

    override fun kind(): String? {
        return null
    }

    override fun toString(): String {
        return name
    }
}
