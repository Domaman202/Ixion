package com.kingmang.ixion

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.AutoCloseable

/**
 * A utility to temporarily redirect both System.out and System.err to a single
 * in-memory buffer, capturing all output as one string.
 * 
 * 
 * Example usage (try-with-resources):
 * <pre>`try (CombinedOutputCapture capture = new CombinedOutputCapture()) {     System.out.println("Hello");     System.err.println("Error");     System.out.println("World");     String all = capture.getOutput();  // "Hello\nError\nWorld\n" } `</pre>
 * 
 * 
 * Static convenience methods are also provided.
 */
class CombinedOutputCapture : AutoCloseable {
    private val originalOut: PrintStream?
    private val originalErr: PrintStream?
    private val buffer: ByteArrayOutputStream
    private val combinedStream: PrintStream
    private var closed = false

    /**
     * Creates a new capture session. Both System.out and System.err are redirected
     * to a single shared buffer.
     */
    init {
        this.originalOut = System.out
        this.originalErr = System.err
        this.buffer = ByteArrayOutputStream()
        this.combinedStream = PrintStream(buffer)
        System.setOut(combinedStream)
        System.setErr(combinedStream)
    }

    val output: String?
        /**
         * Returns all captured output (both stdout and stderr) as a string.
         * The captured output is flushed before retrieval.
         * 
         * @return the combined captured content
         */
        get() {
            combinedStream.flush()
            return buffer.toString()
        }

    /**
     * Clears the captured buffer.
     */
    fun reset() {
        buffer.reset()
    }

    /**
     * Restores the original System.out and System.err streams.
     */
    override fun close() {
        if (!closed) {
            System.setOut(originalOut)
            System.setErr(originalErr)
            closed = true
        }
    }

    companion object {
        // --- Static convenience methods -------------------------------------------------
        /**
         * Executes a task and returns the combined output (stdout + stderr) as a string.
         * 
         * @param task the code block to execute
         * @return the captured combined output
         */
        fun capture(task: Runnable): String? {
            CombinedOutputCapture().use { capture ->
                task.run()
                return capture.output
            }
        }
    }
}