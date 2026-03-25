package com.kingmang.ixion;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * A utility to temporarily redirect both System.out and System.err to a single
 * in-memory buffer, capturing all output as one string.
 *
 * <p>Example usage (try-with-resources):
 * <pre>{@code
 * try (CombinedOutputCapture capture = new CombinedOutputCapture()) {
 *     System.out.println("Hello");
 *     System.err.println("Error");
 *     System.out.println("World");
 *     String all = capture.getOutput();  // "Hello\nError\nWorld\n"
 * }
 * }</pre>
 *
 * <p>Static convenience methods are also provided.
 */
public class CombinedOutputCapture implements AutoCloseable {
    private final PrintStream originalOut;
    private final PrintStream originalErr;
    private final ByteArrayOutputStream buffer;
    private final PrintStream combinedStream;
    private boolean closed = false;

    /**
     * Creates a new capture session. Both System.out and System.err are redirected
     * to a single shared buffer.
     */
    public CombinedOutputCapture() {
        this.originalOut = System.out;
        this.originalErr = System.err;
        this.buffer = new ByteArrayOutputStream();
        this.combinedStream = new PrintStream(buffer);
        System.setOut(combinedStream);
        System.setErr(combinedStream);
    }

    /**
     * Returns all captured output (both stdout and stderr) as a string.
     * The captured output is flushed before retrieval.
     *
     * @return the combined captured content
     */
    public String getOutput() {
        combinedStream.flush();
        return buffer.toString();
    }

    /**
     * Clears the captured buffer.
     */
    public void reset() {
        buffer.reset();
    }

    /**
     * Restores the original System.out and System.err streams.
     */
    @Override
    public void close() {
        if (!closed) {
            System.setOut(originalOut);
            System.setErr(originalErr);
            closed = true;
        }
    }

    // --- Static convenience methods -------------------------------------------------

    /**
     * Executes a task and returns the combined output (stdout + stderr) as a string.
     *
     * @param task the code block to execute
     * @return the captured combined output
     */
    public static String capture(Runnable task) {
        try (CombinedOutputCapture capture = new CombinedOutputCapture()) {
            task.run();
            return capture.getOutput();
        }
    }
}