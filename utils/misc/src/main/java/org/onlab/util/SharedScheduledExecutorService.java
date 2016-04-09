/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onlab.util;

import com.google.common.base.Throwables;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A new scheduled executor service that does not eat exception.
 */
public class SharedScheduledExecutorService implements ScheduledExecutorService {

    private static final String NOT_ALLOWED = "Shutdown of scheduled executor is not allowed";
    private final Logger log = getLogger(getClass());

    private ScheduledExecutorService executor;

    /**
     * Creates a wrapper for the given scheduled executor service.
     *
     * @param executor executor service to wrap
     */
    SharedScheduledExecutorService(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Returns the backing scheduled executor service.
     *
     * @return backing executor service
     */
    ScheduledExecutorService backingExecutor() {
        return executor;
    }

    /**
     * Swaps the backing executor with a new one and shuts down the old one.
     *
     * @param executorService new scheduled executor service
     */
    void setBackingExecutor(ScheduledExecutorService executorService) {
        ScheduledExecutorService oldExecutor = this.executor;
        this.executor = executorService;
        oldExecutor.shutdown();
    }

    /**
     * Creates and executes a one-shot action that becomes enabled
     * after the given delay.
     *
     * @param command the task to execute
     * @param delay the time from now to delay execution
     * @param unit the time unit of the delay parameter
     * @param repeatFlag the flag to denote whether to restart a failed task
     * @return a ScheduledFuture representing pending completion of
     *         the task and whose {@code get()} method will return
     *         {@code null} upon completion
     */
    public ScheduledFuture<?> schedule(Runnable command, long delay,
                                       TimeUnit unit, boolean repeatFlag) {
        return executor.schedule(wrap(command, repeatFlag), delay, unit);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return schedule(command, delay, unit, false);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return executor.schedule(() -> {
            V v = null;
            try {
                v = callable.call();
            } catch (Exception e) {
                log.error("Uncaught exception on " + callable.getClass(), e);
            }
            return v;
        }, delay, unit);
    }

    /**
     * Creates and executes a periodic action that becomes enabled first
     * after the given initial delay, and subsequently with the given
     * period; that is executions will commence after
     * {@code initialDelay} then {@code initialDelay+period}, then
     * {@code initialDelay + 2 * period}, and so on.
     * Depends on the repeat flag that the user set, the failed tasks can be
     * either restarted or terminated. If the repeat flag is set to to true,
     * ant execution of the task encounters an exception, subsequent executions
     * are permitted, otherwise, subsequent executions are suppressed.
     * If any execution of this task takes longer than its period, then
     * subsequent executions may start late, but will not concurrently execute.
     *
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @param unit the time unit of the initialDelay and period parameters
     * @param repeatFlag the flag to denote whether to restart a failed task
     * @return a ScheduledFuture representing pending completion of
     *         the task, and whose {@code get()} method will throw an
     *         exception upon cancellation
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                  long initialDelay,
                                                  long period,
                                                  TimeUnit unit,
                                                  boolean repeatFlag) {
        return executor.scheduleAtFixedRate(wrap(command, repeatFlag),
                initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                                                  long period, TimeUnit unit) {
        return scheduleAtFixedRate(command, initialDelay, period, unit, false);
    }

    /**
     * Creates and executes a periodic action that becomes enabled first
     * after the given initial delay, and subsequently with the
     * given delay between the termination of one execution and the
     * commencement of the next.
     * Depends on the repeat flag that the user set, the failed tasks can be
     * either restarted or terminated. If the repeat flag is set to to true,
     * ant execution of the task encounters an exception, subsequent executions
     * are permitted, otherwise, subsequent executions are suppressed.
     *
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param delay the delay between the termination of one
     * execution and the commencement of the next
     * @param unit the time unit of the initialDelay and delay parameters
     * @param repeatFlag the flag to denote whether to restart a failed task
     * @return a ScheduledFuture representing pending completion of
     *         the task, and whose {@code get()} method will throw an
     *         exception upon cancellation
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                     long initialDelay,
                                                     long delay,
                                                     TimeUnit unit,
                                                     boolean repeatFlag) {
        return executor.scheduleWithFixedDelay(wrap(command, repeatFlag),
                initialDelay, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                                                     long delay, TimeUnit unit) {
        return scheduleWithFixedDelay(command, initialDelay, delay, unit, false);
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException(NOT_ALLOWED);
    }

    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException(NOT_ALLOWED);
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executor.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return executor.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                         long timeout, TimeUnit unit)
            throws InterruptedException {
        return executor.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return executor.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                           long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return executor.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }

    private Runnable wrap(Runnable command, boolean repeatFlag) {
        return new LoggableRunnable(command, repeatFlag);
    }

    /**
     * A runnable class that allows to capture and log the exceptions.
     */
    private class LoggableRunnable implements Runnable {
        private Runnable runnable;
        private boolean repeatFlag;

        public LoggableRunnable(Runnable runnable, boolean repeatFlag) {
            super();
            this.runnable = runnable;
            this.repeatFlag = repeatFlag;
        }

        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted()) {
                log.info("Task interrupted, quitting");
                return;
            }

            try {
                runnable.run();
            } catch (Exception e) {
                log.error("Uncaught exception on " + runnable.getClass().getSimpleName(), e);

                // if repeat flag set as false, we simply throw an exception to
                // terminate this task
                if (!repeatFlag) {
                    throw Throwables.propagate(e);
                }
            }
        }
    }
}
