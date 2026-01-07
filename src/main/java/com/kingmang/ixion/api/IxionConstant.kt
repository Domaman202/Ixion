package com.kingmang.ixion.api

import com.kingmang.ixion.runtime.CollectionUtil.IxListWrapper
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

object IxionConstant {
    const val EXT: String = ".ix"
    const val OUT_DIR: String = ".out"
    @JvmStatic
    val ListWrapperType: Type? = Type.getType(IxListWrapper::class.java)
    @JvmStatic
    val ArrayListType: Type? = Type.getType(ArrayList::class.java)
    @JvmStatic
    val IteratorType: Type? = Type.getType(MutableIterator::class.java)
    @JvmStatic
    val ObjectType: Type? = Type.getType(Any::class.java)
    @JvmStatic
    val Init: String = "<init>"
    @JvmStatic
    val Clinit: String = "<clinit>"

    @JvmStatic
    val PublicStatic: Int = Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC


    enum class Mutability {
        IMMUTABLE,
        MUTABLE
    }
}
