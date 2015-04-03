package org.onosproject.store.consistent.impl;

import org.onosproject.store.service.AsyncAtomicCounter;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.AtomicCounterBuilder;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation of AtomicCounterBuilder.
 */
public class DefaultAtomicCounterBuilder implements AtomicCounterBuilder {

    private String name;
    private boolean partitionsEnabled = true;
    private final Database partitionedDatabase;
    private final Database inMemoryDatabase;

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
        Database database = partitionsEnabled ? partitionedDatabase : inMemoryDatabase;
        return new DefaultAtomicCounter(name, database);
    }

    @Override
    public AsyncAtomicCounter buildAsyncCounter() {
        Database database = partitionsEnabled ? partitionedDatabase : inMemoryDatabase;
        return new DefaultAsyncAtomicCounter(name, database);
    }
}
