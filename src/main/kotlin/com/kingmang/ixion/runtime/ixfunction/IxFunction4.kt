package com.kingmang.ixion.runtime.ixfunction

fun interface IxFunction4<A, B, C, D, R> {
    fun apply(a: A?, b: B?, c: C?, d: D?): R?
}