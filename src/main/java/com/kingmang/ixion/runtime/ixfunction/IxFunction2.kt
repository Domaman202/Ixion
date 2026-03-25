package com.kingmang.ixion.runtime.ixfunction

fun interface IxFunction2<A, B, R> {
    fun apply(a: A?, b: B?): R?
}