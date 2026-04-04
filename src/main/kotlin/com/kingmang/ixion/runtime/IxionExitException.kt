package com.kingmang.ixion.runtime

class IxionExitException : RuntimeException {
    val code: Int

    constructor(message: String?, code: Int) : super(message) {
        this.code = code
    }

    constructor(cause: Throwable?, code: Int) : super(cause) {
        this.code = code
    }
}