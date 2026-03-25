package com.kingmang.ixion.modules

import com.kingmang.ixion.runtime.BuiltInType
import com.kingmang.ixion.runtime.DefType
import com.kingmang.ixion.runtime.ExternalType
import com.kingmang.ixion.runtime.IxType
import com.kingmang.ixion.typechecker.TypeUtils.getFromString
import java.lang.reflect.Method

object Modules {
    val modules: MutableMap<String?, Class<*>?> = HashMap()

    init {
        modules["prelude"]  = Prelude::class.java
        modules["gui"]      = Prelude::class.java
        modules["http"]     = HttpModule::class.java
    }

    @JvmStatic
    fun getExports(module: String?): MutableList<DefType?> {
        val result = ArrayList<DefType?>()
        if (modules.containsKey(module)) {
            val c: Class<*> = modules[module]!!
            val m = c.getMethods()
            for (method in m) {
                var name = method.name
                if (method.declaringClass == Any::class.java) {
                    continue
                }
                val parameters = getPairs(method)
                var isPrefixed = false
                if (name.startsWith("_")) {
                    name = name.substring(1)
                    isPrefixed = true
                }
                val funcType = DefType(name, parameters)
                funcType.isPrefixed = isPrefixed
                funcType.returnType = ExternalType(method.returnType)

                val bt = getFromString(method.returnType.getName())
                if (bt != null) funcType.returnType = bt

                funcType.glue = true
                funcType.owner = c.getName().replace('.', '/')
                result.add(funcType)
            }
        }
        return result
    }

    private fun getPairs(method: Method): ArrayList<Pair<String, IxType>> {
        val parameters = ArrayList<Pair<String, IxType>>()
        for (p in method.parameterTypes) {
            val t = when (p.getName()) {
                "int"       -> BuiltInType.INT
                "float"     -> BuiltInType.FLOAT
                "boolean"   -> BuiltInType.BOOLEAN
                else        -> ExternalType(p)
            }
            parameters.add(Pair("_", t))
        }
        return parameters
    }
}