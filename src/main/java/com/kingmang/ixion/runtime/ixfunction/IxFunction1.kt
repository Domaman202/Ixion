package com.kingmang.ixion.runtime.ixfunction

fun interface IxFunction1<A, R> {
    fun apply(a: A?): R?
}