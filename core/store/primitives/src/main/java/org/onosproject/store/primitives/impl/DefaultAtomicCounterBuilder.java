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
package org.onosproject.store.primitives.impl;

import org.onosproject.store.service.AsyncAtomicCounter;
import org.onosproject.store.service.AtomicCounterBuilder;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of AtomicCounterBuilder.
 */
public class DefaultAtomicCounterBuilder extends AtomicCounterBuilder {

    private final Database partitionedDatabase;
    private final Database inMemoryDatabase;

    public DefaultAtomicCounterBuilder(Database inMemoryDatabase, Database partitionedDatabase) {
        this.inMemoryDatabase = inMemoryDatabase;
        this.partitionedDatabase = partitionedDatabase;
    }

    @Override
    public AsyncAtomicCounter build() {
        Database database = partitionsDisabled() ? inMemoryDatabase : partitionedDatabase;
        return new DefaultAsyncAtomicCounter(checkNotNull(name()), database, meteringEnabled());
    }
}
