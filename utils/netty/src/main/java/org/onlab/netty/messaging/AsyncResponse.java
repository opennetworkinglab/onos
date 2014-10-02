package org.onlab.netty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An asynchronous response.
 * This class provides a base implementation of Response, with methods to retrieve the
 * result and query to see if the result is ready. The result can only be retrieved when
 * it is ready and the get methods will block if the result is not ready yet.
 * @param <T> type of response.
 */
public class AsyncResponse<T> implements Response<T> {

    private T value;
    private boolean done = false;
    private final long start = System.nanoTime();

    @Override
    public T get(long timeout, TimeUnit tu) throws TimeoutException {
        timeout = tu.toNanos(timeout);
        boolean interrupted = false;
        try {
            synchronized (this) {
                while (!done) {
                    try {
                        long timeRemaining = timeout - (System.nanoTime() - start);
                        if (timeRemaining <= 0) {
                            throw new TimeoutException("Operation timed out.");
                        }
                        TimeUnit.NANOSECONDS.timedWait(this, timeRemaining);
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        return value;
    }

    @Override
    public T get() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReady() {
        return done;
    }

    /**
     * Sets response value and unblocks any thread blocking on the response to become
     * available.
     * @param data response data.
     */
    @SuppressWarnings("unchecked")
    public synchronized void setResponse(Object data) {
        if (!done) {
            done = true;
            value = (T) data;
            this.notifyAll();
        }
    }
}
