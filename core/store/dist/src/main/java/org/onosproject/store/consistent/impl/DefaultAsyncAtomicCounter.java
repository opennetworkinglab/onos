/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.consistent.impl;

import org.onosproject.store.service.AsyncAtomicCounter;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Default implementation for a distributed AsyncAtomicCounter backed by
 * partitioned Raft DB.
 * <p>
 * The initial value will be zero.
 */
public class DefaultAsyncAtomicCounter implements AsyncAtomicCounter {

    private final String name;
    private final Database database;
    private final boolean retryOnFailure;
    private final ScheduledExecutorService retryExecutor;
    // TODO: configure delay via builder
    private static final int DELAY_BETWEEN_RETRY_SEC = 1;
    private final Logger log = getLogger(getClass());
    private final MeteringAgent monitor;

    private static final String PRIMITIVE_NAME = "atomicCounter";
    private static final String INCREMENT_AND_GET = "incrementAndGet";
    private static final String GET_AND_INCREMENT = "getAndIncrement";
    private static final String GET_AND_ADD = "getAndAdd";
    private static final String ADD_AND_GET = "addAndGet";
    private static final String GET = "get";

    public DefaultAsyncAtomicCounter(String name,
                                     Database database,
                                     boolean retryOnException,
                                     boolean meteringEnabled,
                                     ScheduledExecutorService retryExecutor) {
        this.name = checkNotNull(name);
        this.database = checkNotNull(database);
        this.retryOnFailure = retryOnException;
        this.retryExecutor = retryExecutor;
        this.monitor = new MeteringAgent(PRIMITIVE_NAME, name, meteringEnabled);
    }

    @Override
    public CompletableFuture<Long> incrementAndGet() {
        final MeteringAgent.Context timer = monitor.startTimer(INCREMENT_AND_GET);
        return addAndGet(1L)
                .whenComplete((r, e) -> timer.stop());
    }

    @Override
    public CompletableFuture<Long> get() {
        final MeteringAgent.Context timer = monitor.startTimer(GET);
        return database.counterGet(name)
                .whenComplete((r, e) -> timer.stop());
    }

    @Override
    public CompletableFuture<Long> getAndIncrement() {
        final MeteringAgent.Context timer = monitor.startTimer(GET_AND_INCREMENT);
        return getAndAdd(1L)
                .whenComplete((r, e) -> timer.stop());
    }

    @Override
    public CompletableFuture<Long> getAndAdd(long delta) {
        final MeteringAgent.Context timer = monitor.startTimer(GET_AND_ADD);
        CompletableFuture<Long> result = database.counterGetAndAdd(name, delta);
        if (!retryOnFailure) {
            return result
                    .whenComplete((r, e) -> timer.stop());
        }

        CompletableFuture<Long> future = new CompletableFuture<>();
        return result.whenComplete((r, e) -> {
            timer.stop();
            // TODO : Account for retries
            if (e != null) {
                log.warn("getAndAdd failed due to {}. Will retry", e.getMessage());
                retryExecutor.schedule(new RetryTask(database::counterGetAndAdd, delta, future),
                        DELAY_BETWEEN_RETRY_SEC,
                        TimeUnit.SECONDS);
            } else {
                future.complete(r);
            }
        }).thenCompose(v -> future);
    }

    @Override
    public CompletableFuture<Long> addAndGet(long delta) {
        final MeteringAgent.Context timer = monitor.startTimer(ADD_AND_GET);
        CompletableFuture<Long> result = database.counterAddAndGet(name, delta);
        if (!retryOnFailure) {
            return result
                    .whenComplete((r, e) -> timer.stop());
        }

        CompletableFuture<Long> future = new CompletableFuture<>();
        return result.whenComplete((r, e) -> {
            timer.stop();
            // TODO : Account for retries
            if (e != null) {
                log.warn("addAndGet failed due to {}. Will retry", e.getMessage());
                retryExecutor.schedule(new RetryTask(database::counterAddAndGet, delta, future),
                        DELAY_BETWEEN_RETRY_SEC,
                        TimeUnit.SECONDS);
            } else {
                future.complete(r);
            }
        }).thenCompose(v -> future);
    }

    private class RetryTask implements Runnable {

        private final BiFunction<String, Long, CompletableFuture<Long>> function;
        private final Long delta;
        private final CompletableFuture<Long> result;

        public RetryTask(BiFunction<String, Long, CompletableFuture<Long>> function,
                Long delta,
                CompletableFuture<Long> result) {
            this.function = function;
            this.delta = delta;
            this.result = result;
        }

        @Override
        public void run() {
            function.apply(name, delta).whenComplete((r, e) -> {
                if (e == null) {
                    result.complete(r);
                } else {
                    log.warn("{} retry failed due to {}. Will try again...", function, e.getMessage());
                    // TODO: Exponential backoff
                    // TODO: limit retries
                    retryExecutor.schedule(this, DELAY_BETWEEN_RETRY_SEC, TimeUnit.SECONDS);
                }
            });
        }
    }
}