/*
 * Copyright 2016-present Open Networking Foundation
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

import com.google.common.util.concurrent.MoreExecutors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * (Somewhat) predictable ExecutorService.
 * <p>
 * ExecutorService which behaves similar to the one created by
 * {@link Executors#newFixedThreadPool(int, ThreadFactory)},
 * but assigns command to specific thread based on
 * it's {@link PickyTask#hint()}, {@link Object#hashCode()}, or hint value explicitly
 * specified when the command was passed to this {@link PredictableExecutor}.
 */
public class PredictableExecutor
        extends AbstractExecutorService
        implements ExecutorService {

    private final List<ExecutorService> backends;

    /**
     * Creates {@link PredictableExecutor} instance.
     *
     * @param buckets number of buckets or 0 to match available processors
     * @param threadFactory {@link ThreadFactory} to use to create threads
     * @return {@link PredictableExecutor}
     */
    public static PredictableExecutor newPredictableExecutor(int buckets, ThreadFactory threadFactory)  {
        return new PredictableExecutor(buckets, threadFactory);
    }

    /**
     * Creates {@link PredictableExecutor} instance.
     *
     * @param buckets number of buckets or 0 to match available processors
     * @param threadFactory {@link ThreadFactory} to use to create threads
     */
    public PredictableExecutor(int buckets, ThreadFactory threadFactory) {
        this(buckets, threadFactory, false);
    }

    /**
     * Creates {@link PredictableExecutor} instance.
     * Meant for testing purposes.
     *
     * @param buckets number of buckets or 0 to match available processors
     * @param threadFactory {@link ThreadFactory} to use to create threads
     * @param directExec direct executors
     */
    public PredictableExecutor(int buckets, ThreadFactory threadFactory, boolean directExec) {
        checkArgument(buckets >= 0, "number of buckets must be non zero");
        checkNotNull(threadFactory);
        if (buckets == 0) {
            buckets = Runtime.getRuntime().availableProcessors();
        }
        this.backends = new ArrayList<>(buckets);

        for (int i = 0; i < buckets; ++i) {
            this.backends.add(backendExecutorService(threadFactory, directExec));
        }
    }

    /**
     * Creates {@link PredictableExecutor} instance with
     * bucket size set to number of available processors.
     *
     * @param threadFactory {@link ThreadFactory} to use to create threads
     */
    public PredictableExecutor(ThreadFactory threadFactory) {
        this(0, threadFactory);
    }

    /**
     * Creates a single thread {@link ExecutorService} to use in the backend.
     *
     * @param threadFactory {@link ThreadFactory} to use to create threads
     * @param direct direct executors
     * @return single thread {@link ExecutorService} or direct executor
     */
    protected ExecutorService backendExecutorService(ThreadFactory threadFactory, boolean direct) {
        return direct ? MoreExecutors.newDirectExecutorService() : Executors.newSingleThreadExecutor(threadFactory);
    }


    /**
     * Executes given command at some time in the future.
     *
     * @param command the {@link Runnable} task
     * @param hint value to pick thread to run on.
     */
    public void execute(Runnable command, int hint) {
        int index = Math.abs(hint) % backends.size();
        backends.get(index).execute(command);
    }

    /**
     * Executes given command at some time in the future.
     *
     * @param command the {@link Runnable} task
     * @param hintFunction Function to compute hint value
     */
    public void execute(Runnable command, Function<Runnable, Integer> hintFunction) {
        execute(command, hintFunction.apply(command));
    }

    /**
     * Submits a value-returning task for execution and returns a
     * Future representing the pending results of the task. The
     * Future's {@code get} method will return the task's result upon
     * successful completion.
     *
     * @param command the {@link Runnable} task
     * @param hint value to pick thread to run on.
     * @return completable future representing the pending results
     */
    public CompletableFuture<Void> submit(Runnable command, int hint) {
        int index = Math.abs(hint) % backends.size();
        return CompletableFuture.runAsync(command, backends.get(index));
    }

    /**
     * Submits a value-returning task for execution and returns a
     * Future representing the pending results of the task. The
     * Future's {@code get} method will return the task's result upon
     * successful completion.
     *
     * @param command the {@link Runnable} task
     * @param hintFunction Function to compute hint value
     * @return completable future representing the pending results
     */
    public CompletableFuture<Void> submit(Runnable command, Function<Runnable, Integer> hintFunction) {
        int hint = hintFunction.apply(command);
        return submit(command, hint);
    }

    private static int hint(Runnable command) {
        if (command instanceof PickyTask) {
            return ((PickyTask) command).hint();
        } else {
            return Objects.hashCode(command);
        }
    }

    @Override
    public void execute(Runnable command) {
        execute(command, PredictableExecutor::hint);
    }

    @Override
    public void shutdown() {
        backends.forEach(ExecutorService::shutdown);
    }

    @Override
    public List<Runnable> shutdownNow() {
        return backends.stream()
                .map(ExecutorService::shutdownNow)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isShutdown() {
        return backends.stream().allMatch(ExecutorService::isShutdown);
    }

    @Override
    public boolean isTerminated() {
        return backends.stream().allMatch(ExecutorService::isTerminated);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: It'll try, but is not assured that the method will return by specified timeout.
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {

        final Duration timeoutD = Duration.of(unit.toMillis(timeout), ChronoUnit.MILLIS);
        final Instant start = Instant.now();

        return backends.parallelStream()
                .filter(es -> !es.isTerminated())
                .map(es -> {
                    long timeoutMs = timeoutD.minus(Duration.between(Instant.now(), start)).toMillis();
                    try {
                        return es.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                })
                .allMatch(result -> result);
    }

    @Override
    protected <T> PickyFutureTask<T> newTaskFor(Callable<T> callable) {
        return new PickyFutureTask<>(callable);
    }

    @Override
    protected <T> PickyFutureTask<T> newTaskFor(Runnable runnable, T value) {
        return new PickyFutureTask<>(runnable, value);
    }

    /**
     * {@link Runnable} also implementing {@link PickyTask}.
     */
    public static interface PickyRunnable extends PickyTask, Runnable { }

    /**
     * {@link Callable} also implementing {@link PickyTask}.
     *
     * @param <T> result type
     */
    public static interface PickyCallable<T> extends PickyTask, Callable<T> { }

    /**
     * Wraps the given {@link Runnable} into {@link PickyRunnable} returning supplied hint.
     *
     * @param runnable {@link Runnable}
     * @param hint hint value
     * @return {@link PickyRunnable}
     */
    public static PickyRunnable picky(Runnable runnable, int hint) {
        return picky(runnable, (r) -> hint);
    }

    /**
     * Wraps the given {@link Runnable} into {@link PickyRunnable} returning supplied hint.
     *
     * @param runnable {@link Runnable}
     * @param hint hint function
     * @return {@link PickyRunnable}
     */
    public static PickyRunnable picky(Runnable runnable, Function<Runnable, Integer> hint) {
        checkNotNull(runnable);
        checkNotNull(hint);
        return new PickyRunnable() {

            @Override
            public void run() {
                runnable.run();
            }

            @Override
            public int hint() {
                return hint.apply(runnable);
            }
        };
    }

    /**
     * Wraps the given {@link Callable} into {@link PickyCallable} returning supplied hint.
     *
     * @param callable {@link Callable}
     * @param hint hint value
     * @param <T> entity type
     * @return {@link PickyCallable}
     */
    public static <T> PickyCallable<T> picky(Callable<T> callable, int hint) {
        return picky(callable, (c) -> hint);
    }

    /**
     * Wraps the given {@link Callable} into {@link PickyCallable} returning supplied hint.
     *
     * @param callable {@link Callable}
     * @param hint hint function
     * @param <T> entity type
     * @return {@link PickyCallable}
     */
    public static <T> PickyCallable<T> picky(Callable<T> callable, Function<Callable<T>, Integer> hint) {
        checkNotNull(callable);
        checkNotNull(hint);
        return new PickyCallable<T>() {

            @Override
            public T call() throws Exception {
                return callable.call();
            }

            @Override
            public int hint() {
                return hint.apply(callable);
            }

        };
    }

    /**
     * Abstraction to give a task a way to express it's preference to run on
     * certain thread.
     */
    public static interface PickyTask {

        /**
         * Returns hint for choosing which Thread to run this task on.
         *
         * @return hint value
         */
        int hint();
    }

    /**
     * A {@link FutureTask} implementing {@link PickyTask}.
     * <p>
     * Note: if the wrapped {@link Callable} or {@link Runnable} was an instance of
     * {@link PickyTask}, it will use {@link PickyTask#hint()} value, if not use {@link Object#hashCode()}.
     *
     * @param <T> result type.
     */
    public static class PickyFutureTask<T>
        extends FutureTask<T>
        implements PickyTask {

        private final Object runnableOrCallable;

        /**
         * Same as {@link FutureTask#FutureTask(Runnable, Object)}.
         *
         * @param runnable work to do
         * @param value result
         */
        public PickyFutureTask(Runnable runnable, T value) {
            super(runnable, value);
            runnableOrCallable = checkNotNull(runnable);
        }

        /**
         * Same as {@link FutureTask#FutureTask(Callable)}.
         *
         * @param callable work to be done
         */
        public PickyFutureTask(Callable<T> callable) {
            super(callable);
            runnableOrCallable = checkNotNull(callable);
        }

        @Override
        public int hint() {
            if (runnableOrCallable instanceof PickyTask) {
                return ((PickyTask) runnableOrCallable).hint();
            } else {
                return runnableOrCallable.hashCode();
            }
        }
    }
}
