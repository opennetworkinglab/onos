package org.onlab.netty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Response object returned when making synchronous requests.
 * Can you used to check is a response is ready and/or wait for a response
 * to become available.
 */
public interface Response {

    /**
     * Gets the response waiting for a designated timeout period.
     * @param timeout timeout period (since request was sent out)
     * @param tu unit of time.
     * @return response payload
     * @throws TimeoutException if the timeout expires before the response arrives.
     */
    public byte[] get(long timeout, TimeUnit tu) throws TimeoutException;

    /**
     * Gets the response waiting for indefinite timeout period.
     * @return response payload
     * @throws InterruptedException if the thread is interrupted before the response arrives.
     */
    public byte[] get() throws InterruptedException;

    /**
     * Checks if the response is ready without blocking.
     * @return true if response is ready, false otherwise.
     */
    public boolean isReady();
}
