package com.kingmang.ixion.runtime

import org.objectweb.asm.Opcodes
import java.util.stream.Collectors

class UnionType(var types: MutableSet<IxType?>) : IxType {
    override val defaultValue: Any?
        get() = null

    override val descriptor: String
        get() = "Ljava/lang/Object;"

    override val internalName: String?
        get() = null

    override val loadVariableOpcode: Int
        get() = Opcodes.ALOAD

    override val name: String?
        get() = types
            .stream()
            .map { obj: IxType? -> obj!!.name }
            .collect(Collectors.joining(" | "))

    override val returnOpcode: Int
        get() = Opcodes.ARETURN

    override val typeClass: Class<*>?
        get() = null

    override val isNumeric: Boolean
        get() = false

    override fun kind(): String? {
        return null
    }

    override fun toString(): String {
        return types
            .stream()
            .map { obj: IxType? -> obj!!.name }
            .collect(Collectors.joining(" | "))
    }
}
