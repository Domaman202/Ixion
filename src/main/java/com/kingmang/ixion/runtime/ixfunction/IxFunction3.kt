package com.kingmang.ixion.runtime.ixfunction

fun interface IxFunction3<A, B, C, R> {
    fun apply(a: A?, b: B?, c: C?): R?
}