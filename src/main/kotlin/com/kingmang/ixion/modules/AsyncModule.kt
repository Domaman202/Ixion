package com.kingmang.ixion.modules

import com.kingmang.ixion.runtime.ixfunction.IxFunction0
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Suppress("unused")
object AsyncModule {
    private val nextTaskId = AtomicInteger(1)
    private val tasks = ConcurrentHashMap<Int, Thread>()

    @JvmStatic
    fun run(task: IxFunction0<Any?>): Int {
        val taskId = nextTaskId.getAndIncrement()
        val thread = Thread {
            try {
                task.apply()
            } finally {
                tasks.remove(taskId)
            }
        }

        thread.name = "ixion-async-$taskId"
        tasks[taskId] = thread
        thread.start()
        return taskId
    }

    @JvmStatic
    fun runDelayed(delayMs: Int, task: IxFunction0<Any?>): Int {
        require(delayMs >= 0) { "delay must be >= 0" }
        return run(
            IxFunction0 {
                Thread.sleep(delayMs.toLong())
                task.apply()
            }
        )
    }

    @JvmStatic
    fun isRunning(taskId: Int): Boolean {
        val thread = tasks[taskId] ?: return false
        return thread.isAlive
    }

    @JvmStatic
    fun await(taskId: Int) {
        tasks[taskId]?.join()
    }

    @JvmStatic
    fun cancel(taskId: Int): Boolean {
        val thread = tasks[taskId] ?: return false
        thread.interrupt()
        return true
    }

    @JvmStatic
    fun sleep(ms: Int) {
        require(ms >= 0) { "sleep ms must be >= 0" }
        Thread.sleep(ms.toLong())
    }
}
