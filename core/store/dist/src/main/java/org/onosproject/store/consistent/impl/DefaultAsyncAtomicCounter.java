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

import java.util.concurrent.CompletableFuture;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation for a distributed AsyncAtomicCounter backed by
 * partitioned Raft DB.
 * <p>
 * The initial value will be zero.
 */
public class DefaultAsyncAtomicCounter implements AsyncAtomicCounter {

    private final String name;
    private final Database database;
    private final MeteringAgent monitor;

    private static final String PRIMITIVE_NAME = "atomicCounter";
    private static final String INCREMENT_AND_GET = "incrementAndGet";
    private static final String GET_AND_INCREMENT = "getAndIncrement";
    private static final String GET_AND_ADD = "getAndAdd";
    private static final String ADD_AND_GET = "addAndGet";
    private static final String GET = "get";

    public DefaultAsyncAtomicCounter(String name,
                                     Database database,
                                     boolean meteringEnabled) {
        this.name = checkNotNull(name);
        this.database = checkNotNull(database);
        this.monitor = new MeteringAgent(PRIMITIVE_NAME, name, meteringEnabled);
    }

    @Override
    public CompletableFuture<Long> incrementAndGet() {
        final MeteringAgent.Context timer = monitor.startTimer(INCREMENT_AND_GET);
        return addAndGet(1L)
                .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Long> get() {
        final MeteringAgent.Context timer = monitor.startTimer(GET);
        return database.counterGet(name)
                .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Long> getAndIncrement() {
        final MeteringAgent.Context timer = monitor.startTimer(GET_AND_INCREMENT);
        return getAndAdd(1L)
                .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Long> getAndAdd(long delta) {
        final MeteringAgent.Context timer = monitor.startTimer(GET_AND_ADD);
        return database.counterGetAndAdd(name, delta)
                       .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Long> addAndGet(long delta) {
        final MeteringAgent.Context timer = monitor.startTimer(ADD_AND_GET);
        return database.counterAddAndGet(name, delta)
                       .whenComplete((r, e) -> timer.stop(e));
    }
}
