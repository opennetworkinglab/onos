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
import org.onosproject.store.service.AsyncAtomicCounter;
import static com.google.common.base.Preconditions.*;

/**
 * Default implementation for a distributed AsyncAtomicCounter backed by
 * partitioned Raft DB.
 * <p>
 * The initial value will be zero.
 */
public class DefaultAsyncAtomicCounter implements AsyncAtomicCounter {

    private final String name;
    private final Database database;

    public DefaultAsyncAtomicCounter(String name, Database database) {
        this.name = checkNotNull(name);
        this.database = checkNotNull(database);
    }

    @Override
    public CompletableFuture<Long> incrementAndGet() {
        return database.nextValue(name);
    }

    @Override
    public CompletableFuture<Long> get() {
        return database.currentValue(name);
    }
}
