package com.kingmang.ixion.exception

import com.kingmang.ixion.api.IxApi.Companion.exit

class Panic(private val message: String?) {
    companion object {
        private const val R = "\u001B[31m"
        private const val RESET = "\u001B[0m"
    }

    fun send() {
        exit(R + ("panic: $message") + RESET, 1)
    }
}
