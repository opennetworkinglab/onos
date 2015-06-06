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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.onosproject.store.service.AsyncAtomicCounter;





import org.slf4j.Logger;

import static com.google.common.base.Preconditions.*;
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

    public DefaultAsyncAtomicCounter(String name,
            Database database,
            boolean retryOnException,
            ScheduledExecutorService retryExecutor) {
        this.name = checkNotNull(name);
        this.database = checkNotNull(database);
        this.retryOnFailure = retryOnException;
        this.retryExecutor = retryExecutor;
    }

    @Override
    public CompletableFuture<Long> incrementAndGet() {
        return addAndGet(1L);
    }

    @Override
    public CompletableFuture<Long> get() {
        return database.counterGet(name);
    }

    @Override
    public CompletableFuture<Long> getAndIncrement() {
        return getAndAdd(1L);
    }

    @Override
    public CompletableFuture<Long> getAndAdd(long delta) {
        CompletableFuture<Long> result = database.counterGetAndAdd(name, delta);
        if (!retryOnFailure) {
            return result;
        }

        CompletableFuture<Long> future = new CompletableFuture<>();
        return result.whenComplete((r, e) -> {
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
        CompletableFuture<Long> result = database.counterAddAndGet(name, delta);
        if (!retryOnFailure) {
            return result;
        }

        CompletableFuture<Long> future = new CompletableFuture<>();
        return result.whenComplete((r, e) -> {
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