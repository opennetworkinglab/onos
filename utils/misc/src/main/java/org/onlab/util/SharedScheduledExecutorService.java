/*
 * Copyright 2016 Open Networking Laboratory
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
class SharedScheduledExecutorService implements ScheduledExecutorService {

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
     * @param executor new scheduled executor service
     */
    void setBackingExecutor(ScheduledExecutorService executor) {
        ScheduledExecutorService oldExecutor = this.executor;
        this.executor = executor;
        oldExecutor.shutdown();
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return executor.schedule(wrap(command), delay, unit);
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

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                                                  long period, TimeUnit unit) {
        return executor.scheduleAtFixedRate(wrap(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                                                     long delay, TimeUnit unit) {
        return executor.scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit);
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

    private Runnable wrap(Runnable command) {
        return new LoggableRunnable(command);
    }

    /**
     * A runnable class that allows to capture and log the exceptions.
     */
    private class LoggableRunnable implements Runnable {
        private Runnable runnable;

        public LoggableRunnable(Runnable runnable) {
            super();
            this.runnable = runnable;
        }

        @Override
        public void run() {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("Uncaught exception on " + runnable.getClass().getSimpleName(), e);
                throw Throwables.propagate(e);
            }
        }
    }
}
