package net.onrc.onos.of.ctl.debugcounter;

public interface IDebugCounter {
    /**
     * Increments the counter by 1 thread-locally, and immediately flushes to
     * the global counter storage. This method should be used for counters that
     * are updated outside the OF message processing pipeline.
     */
    void updateCounterWithFlush();

    /**
     * Increments the counter by 1 thread-locally. Flushing to the global
     * counter storage is delayed (happens with flushCounters() in IDebugCounterService),
     * resulting in higher performance. This method should be used for counters
     * updated in the OF message processing pipeline.
     */
    void updateCounterNoFlush();

    /**
     * Increments the counter thread-locally by the 'incr' specified, and immediately
     * flushes to the global counter storage. This method should be used for counters
     * that are updated outside the OF message processing pipeline.
     */
    void updateCounterWithFlush(int incr);

    /**
     * Increments the counter thread-locally by the 'incr' specified. Flushing to the global
     * counter storage is delayed (happens with flushCounters() in IDebugCounterService),
     * resulting in higher performance. This method should be used for counters
     * updated in the OF message processing pipeline.
     */
    void updateCounterNoFlush(int incr);

    /**
     * Retrieve the value of the counter from the global counter store.
     */
    long getCounterValue();
}
