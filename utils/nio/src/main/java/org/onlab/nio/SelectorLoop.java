package org.onlab.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Selector;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Abstraction of an I/O processing loop based on an NIO selector.
 */
public abstract class SelectorLoop implements Runnable {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Selector used by this loop to pace the I/O operations.
     */
    protected final Selector selector;

    /**
     * Selection operations timeout; specified in millis.
     */
    protected long selectTimeout;

    /**
     * Retains the error that caused the loop to exit prematurely.
     */
    private Throwable error;

    // State indicator
    private enum State { STARTING, STARTED, STOPPING, STOPPED };
    private volatile State state = State.STOPPED;

    /**
     * Creates a new selector loop with the given selection timeout.
     *
     * @param selectTimeout selection timeout; specified in millis
     * @throws IOException if the backing selector cannot be opened
     */
    public SelectorLoop(long selectTimeout) throws IOException {
        checkArgument(selectTimeout > 0, "Timeout must be positive");
        this.selectTimeout = selectTimeout;
        this.selector = openSelector();
    }

    /**
     * Opens a new selector for the use by the loop.
     *
     * @return newly open selector
     * @throws IOException if the backing selector cannot be opened
     */
    protected Selector openSelector() throws IOException {
        return Selector.open();
    }

    /**
     * Indicates that the loop is marked to run.
     */
    protected boolean isRunning() {
        return state == State.STARTED || state == State.STARTING;
    }

    /**
     * Returns the error, if there was one, that caused the loop to terminate
     * prematurely.
     *
     * @return error or null if there was none
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Contains the body of the I/O selector loop.
     *
     * @throws IOException if an error is encountered while selecting I/O
     */
    protected abstract void loop() throws IOException;

    @Override
    public void run() {
        error = null;
        state = State.STARTING;
        try {
            loop();
        } catch (Throwable e) {
            error = e;
            log.error("Loop aborted", e);
        }
        notifyDone();
    }

    /**
     * Notifies observers waiting for loop to become ready.
     */
    protected synchronized void notifyReady() {
        state = State.STARTED;
        notifyAll();
    }

    /**
     * Triggers loop shutdown.
     */
    public void shutdown() {
        // Mark the loop as no longer running and wake up the selector.
        state = State.STOPPING;
        selector.wakeup();
    }

    /**
     * Notifies observers waiting for loop to fully stop.
     */
    private synchronized void notifyDone() {
        state = State.STOPPED;
        notifyAll();
    }

}
