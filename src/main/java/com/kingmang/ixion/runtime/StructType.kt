package com.kingmang.ixion.runtime

import org.objectweb.asm.Opcodes
import java.io.Serializable
import java.util.stream.Collectors

open class StructType(
    override var name: String,
    open val parameters: MutableList<Pair<String, IxType>>,
    open val generics: MutableList<String?>
) : IxType, Serializable {
    open var qualifiedName: String? = null
    open var parentName: String? = null

    override fun equals(other: Any?): Boolean {
        return when (other) {
            null -> false
            is StructType -> this.name == other.name
            else -> false
        }
    }

    override val defaultValue: Any?
        get() = null

    override val descriptor: String?
        get() = "L${qualifiedName!!.replace(":", "/")};"

    override val internalName: String?
        get() = name.replace(".", "/")

    override val loadVariableOpcode: Int
        get() = Opcodes.ALOAD

    override val returnOpcode: Int
        get() = Opcodes.ARETURN

    override val typeClass: Class<*>?
        get() = null

    fun hasGenerics(): Boolean {
        return !generics.isEmpty()
    }

    override val isNumeric: Boolean
        get() = false

    override fun kind(): String {
        return "struct"
    }

    override fun toString(): String {
        return "type $name= struct {" +
                parameters
                    .stream()
                    .map<String?> { m: Pair<String?, IxType?>? -> m!!.second!!.name }
                    .collect(Collectors.joining(", ")) + "}"
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + generics.hashCode()
        result = 31 * result + (qualifiedName?.hashCode() ?: 0)
        result = 31 * result + (parentName?.hashCode() ?: 0)
        return result
    }
}
