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
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.AtomicCounterBuilder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Default implementation of AtomicCounterBuilder.
 */
public class DefaultAtomicCounterBuilder implements AtomicCounterBuilder {

    private String name;
    private boolean partitionsEnabled = true;
    private final Database partitionedDatabase;
    private final Database inMemoryDatabase;
    private boolean metering = true;

    public DefaultAtomicCounterBuilder(Database inMemoryDatabase, Database partitionedDatabase) {
        this.inMemoryDatabase = inMemoryDatabase;
        this.partitionedDatabase = partitionedDatabase;
    }

    @Override
    public AtomicCounterBuilder withName(String name) {
        checkArgument(name != null && !name.isEmpty());
        this.name = name;
        return this;
    }

    @Override
    public AtomicCounterBuilder withPartitionsDisabled() {
        partitionsEnabled = false;
        return this;
    }

    @Override
    public AtomicCounter build() {
        validateInputs();
        Database database = partitionsEnabled ? partitionedDatabase : inMemoryDatabase;
        return new DefaultAtomicCounter(name, database, metering);
    }

    @Override
    public AsyncAtomicCounter buildAsyncCounter() {
        validateInputs();
        Database database = partitionsEnabled ? partitionedDatabase : inMemoryDatabase;
        return new DefaultAsyncAtomicCounter(name, database, metering);
    }

    @Override
    public AtomicCounterBuilder withMeteringDisabled() {
        metering = false;
        return this;
    }

    private void validateInputs() {
        checkState(name != null, "name must be specified");
    }
}
