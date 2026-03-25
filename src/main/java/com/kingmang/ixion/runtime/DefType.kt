package com.kingmang.ixion.runtime

import com.kingmang.ixion.api.IxFile
import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.typechecker.TypeUtils
import org.objectweb.asm.commons.GeneratorAdapter

class DefType : StructType {
    val localMap: MutableMap<String?, Int?> = HashMap()
    val argMap: MutableMap<String?, Int?> = HashMap()
    override var name: String
    var returnType: IxType = BuiltInType.VOID
    var ga: GeneratorAdapter? = null
    var glue: Boolean = false
    var hasReturn2: Boolean = false
    var isPrefixed: Boolean = false
    var owner: String? = null

    var specializations: MutableList<MutableMap<String?, IxType?>?> = ArrayList()

    var currentSpecialization: MutableMap<String?, IxType?>? = null
    @JvmField
    var external: IxFile? = null

    constructor(name: String, parameters: MutableList<Pair<String, IxType>>) : super(
        name,
        parameters,
        ArrayList<String?>()
    ) {
        this.name = name
    }

    constructor(
        name: String,
        parameters: MutableList<Pair<String, IxType>>,
        generics: MutableList<String?>
    ) : super(name, parameters, generics) {
        this.name = name
    }

    fun buildParametersFromSpecialization(specialization: MutableMap<String, IxType>): MutableList<Pair<String, IxType>> {
        val p = ArrayList<Pair<String, IxType>>()
        for (pair in parameters) {
            val pt = pair.second
            if (pt is GenericType) {
                p.add(Pair(pair.first, specialization[pt.key]!!))
            } else {
                p.add(pair)
            }
        }
        return p
    }

    fun buildSpecialization(arguments: MutableList<Expression?>): MutableMap<String?, IxType?> {
        val argTypes = arguments.map { ex: Expression? -> ex!!.realType }
        val specialization = HashMap<String?, IxType?>()
        for (i in parameters.indices) {
            val p = parameters[i]
            val pt = p.second
            if (pt is GenericType) {
                specialization[pt.key] = argTypes[i]
            }
        }
        return specialization
    }

    override val defaultValue: Any?
        get() = null

    override val descriptor: String?
        get() = null

    override val internalName: String?
        get() = null

    override val loadVariableOpcode: Int
        get() = 0

    override val returnOpcode: Int
        get() = 0

    override val typeClass: Class<*>?
        get() = null

    override val isNumeric: Boolean
        get() = false

    override fun kind(): String {
        return "function"
    }

    override fun toString(): String {
        return "def $name(${TypeUtils.parameterString(parameters)}) $returnType"
    }

    companion object {
        fun getSpecializedType(specialization: MutableMap<String?, IxType?>, key: String?): IxType? {
            return specialization[key]
        }
    }
}
