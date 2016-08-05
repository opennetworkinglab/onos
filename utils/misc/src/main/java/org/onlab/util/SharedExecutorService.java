/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.codahale.metrics.Timer;
import com.google.common.base.Throwables;
import org.onlab.metrics.MetricsComponent;
import org.onlab.metrics.MetricsFeature;
import org.onlab.metrics.MetricsService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Executor service wrapper for shared executors with safeguards on shutdown
 * to prevent inadvertent shutdown.
 */
class SharedExecutorService implements ExecutorService {

    private static final String NOT_ALLOWED = "Shutdown of shared executor is not allowed";
    private final Logger log = getLogger(getClass());

    private ExecutorService executor;

    private MetricsService metricsService = null;

    private MetricsComponent executorMetrics;
    private Timer queueMetrics = null;
    private Timer delayMetrics = null;


    /**
     * Creates a wrapper for the given executor service.
     *
     * @param executor executor service to wrap
     */
    SharedExecutorService(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Returns the backing executor service.
     *
     * @return backing executor service
     */
    ExecutorService backingExecutor() {
        return executor;
    }

    /**
     * Swaps the backing executor with a new one and shuts down the old one.
     *
     * @param executor new executor service
     */
    void setBackingExecutor(ExecutorService executor) {
        ExecutorService oldExecutor = this.executor;
        this.executor = executor;
        oldExecutor.shutdown();
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
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        Counter taskCounter = new Counter();
        taskCounter.reset();
        return executor.submit(() -> {
                   T t = null;
                   long queueWaitTime = (long) taskCounter.duration();
                   Class className;
                   if (task instanceof CallableExtended) {
                       className = ((CallableExtended) task).getRunnable().getClass();
                   } else {
                       className = task.getClass();
                   }
                   if (queueMetrics != null) {
                       queueMetrics.update(queueWaitTime, TimeUnit.SECONDS);
                   }
                   taskCounter.reset();
                   try {
                       t = task.call();
                   } catch (Exception e) {
                       getLogger(className).error("Uncaught exception on " + className, e);
                   }
                   long taskwaittime = (long) taskCounter.duration();
                   if (delayMetrics != null) {
                       delayMetrics.update(taskwaittime, TimeUnit.SECONDS);
                   }
                   return t;
               });
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executor.submit(wrap(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executor.submit(wrap(task));
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
                           long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return executor.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }

    /**
     * Enables or disables calculation of the pool performance metrics. If
     * the metrics service is not null metric collection will be enabled;
     * otherwise it will be disabled.
     *
     * @param metricsService optional metric service
     */
    public void setMetricsService(MetricsService metricsService) {
        if (this.metricsService == null && metricsService != null) {
            // If metrics service was newly introduced, initialize metrics.
            executorMetrics = metricsService.registerComponent("SharedExecutor");
            MetricsFeature mf = executorMetrics.registerFeature("*");
            queueMetrics = metricsService.createTimer(executorMetrics, mf, "Queue");
            delayMetrics = metricsService.createTimer(executorMetrics, mf, "Delay");
        } else if (this.metricsService != null && metricsService == null) {
            // If the metrics service was newly withdrawn, tear-down metrics.
            queueMetrics = null;
            delayMetrics = null;
        } // Otherwise just record the metrics service
        this.metricsService = metricsService;
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

    /**
     * CallableExtended class is used to get Runnable Object
     * from Callable Object.
     */
    class CallableExtended implements Callable {

        private Runnable runnable;

        /**
         * Wrapper for Callable object .
         *
         * @param runnable Runnable object
         */
        public CallableExtended(Runnable runnable) {
            this.runnable = runnable;
        }

        public Runnable getRunnable() {
            return runnable;
        }

        @Override
        public Object call() throws Exception {
            runnable.run();
            return null;
        }
    }

}
