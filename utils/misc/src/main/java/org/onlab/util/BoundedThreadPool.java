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

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.LoggerFactory;

/**
 * Implementation of ThreadPoolExecutor that bounds the work queue.
 * <p>
 * When a new job would exceed the queue bound, the job is run on the caller's
 * thread rather than on a thread from the pool.
 * </p>
 */
public final class BoundedThreadPool extends ThreadPoolExecutor {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(BoundedThreadPool.class);

    protected static int maxQueueSize = 80_000; //TODO tune this value
    //private static final RejectedExecutionHandler DEFAULT_HANDLER = new CallerFeedbackPolicy();
    private static final long STATS_INTERVAL = 5_000; //ms

    private final BlockingBoolean underHighLoad;

    private BoundedThreadPool(int numberOfThreads,
                              ThreadFactory threadFactory) {
        super(numberOfThreads, numberOfThreads,
              0L, TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<>(maxQueueSize),
              threadFactory,
              new CallerFeedbackPolicy());
        underHighLoad = ((CallerFeedbackPolicy) getRejectedExecutionHandler()).load();
    }

    /**
     * Returns a single-thread, bounded executor service.
     *
     * @param threadFactory thread factory for the worker thread.
     * @return the bounded thread pool
     */
    public static BoundedThreadPool newSingleThreadExecutor(ThreadFactory threadFactory) {
        return new BoundedThreadPool(1, threadFactory);
    }

    /**
     * Returns a fixed-size, bounded executor service.
     *
     * @param numberOfThreads number of threads in the pool
     * @param threadFactory   thread factory for the worker threads.
     * @return the bounded thread pool
     */
    public static BoundedThreadPool newFixedThreadPool(int numberOfThreads, ThreadFactory threadFactory) {
        return new BoundedThreadPool(numberOfThreads, threadFactory);
    }

    //TODO Might want to switch these to use Metrics class Meter and/or Gauge instead.
    private final Counter submitted = new Counter();
    private final Counter taken = new Counter();

    @Override
    public Future<?> submit(Runnable task) {
        submitted.add(1);
        return super.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        submitted.add(1);
        return super.submit(task, result);
    }

    @Override
    public void execute(Runnable command) {
        submitted.add(1);
        super.execute(command);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        submitted.add(1);
        return super.submit(task);
    }


    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        taken.add(1);
        periodicallyPrintStats();
        updateLoad();
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t == null && r instanceof Future<?>) {
            try {
                Future<?> future = (Future<?>) r;
                if (future.isDone()) {
                    future.get();
                }
            } catch (CancellationException ce) {
                t = ce;
            } catch (ExecutionException ee) {
                t = ee.getCause();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        if (t != null) {
            log.error("Uncaught exception on " + r.getClass().getSimpleName(), t);
        }
    }

    // TODO schedule this with a fixed delay from a scheduled executor
    private final AtomicLong lastPrinted = new AtomicLong(0L);

    private void periodicallyPrintStats() {
        long now = System.currentTimeMillis();
        long prev = lastPrinted.get();
        if (now - prev > STATS_INTERVAL) {
            if (lastPrinted.compareAndSet(prev, now)) {
                log.debug("queue size: {} jobs, submitted: {} jobs/s, taken: {} jobs/s",
                         getQueue().size(),
                         submitted.throughput(), taken.throughput());
                submitted.reset();
                taken.reset();
            }
        }
    }

    // TODO consider updating load whenever queue changes
    private void updateLoad() {
        underHighLoad.set(getQueue().remainingCapacity() / (double) maxQueueSize < 0.2);
    }

    /**
     * Feedback policy that delays the caller's thread until the executor's work
     * queue falls below a threshold, then runs the job on the caller's thread.
     */
    @java.lang.SuppressWarnings("squid:S1217") // We really do mean to call run()
    private static final class CallerFeedbackPolicy implements RejectedExecutionHandler {

        private final BlockingBoolean underLoad = new BlockingBoolean(false);

        public BlockingBoolean load() {
            return underLoad;
        }

        /**
         * Executes task r in the caller's thread, unless the executor
         * has been shut down, in which case the task is discarded.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                // Wait for up to 1 second while the queue drains...
                boolean notified = false;
                try {
                    notified = underLoad.await(false, 1, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    log.debug("Got exception waiting for notification:", exception);
                } finally {
                    if (!notified) {
                        log.info("Waited for 1 second on {}. Proceeding with work...",
                                 Thread.currentThread().getName());
                    } else {
                        log.info("FIXME we got a notice");
                    }
                }
                // Do the work on the submitter's thread
                r.run();
            }
        }
    }
}
