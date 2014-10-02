package org.onlab.netty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Response object returned when making synchronous requests.
 * Can you used to check is a response is ready and/or wait for a response
 * to become available.
 *
 * @param <T> type of response.
 */
public interface Response<T> {

    /**
     * Gets the response waiting for a designated timeout period.
     * @param timeout timeout period (since request was sent out)
     * @param tu unit of time.
     * @return response
     * @throws TimeoutException if the timeout expires before the response arrives.
     */
    public T get(long timeout, TimeUnit tu) throws TimeoutException;

    /**
     * Gets the response waiting for indefinite timeout period.
     * @return response
     * @throws InterruptedException if the thread is interrupted before the response arrives.
     */
    public T get() throws InterruptedException;

    /**
     * Checks if the response is ready without blocking.
     * @return true if response is ready, false otherwise.
     */
    public boolean isReady();
}
