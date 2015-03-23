package org.onosproject.store.consistent.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.Serializer;

/**
 * Default Consistent Map builder.
 *
 * @param <K> type for map key
 * @param <V> type for map value
 */
public class DefaultConsistentMapBuilder<K, V> implements ConsistentMapBuilder<K, V> {

    private Serializer serializer;
    private String name;
    private boolean partitionsEnabled = true;
    private final Database partitionedDatabase;
    private final Database inMemoryDatabase;

    public DefaultConsistentMapBuilder(Database inMemoryDatabase, Database partitionedDatabase) {
        this.inMemoryDatabase = inMemoryDatabase;
        this.partitionedDatabase = partitionedDatabase;
    }

    @Override
    public ConsistentMapBuilder<K, V> withName(String name) {
        checkArgument(name != null && !name.isEmpty());
        this.name = name;
        return this;
    }

    @Override
    public ConsistentMapBuilder<K, V> withSerializer(Serializer serializer) {
        checkArgument(serializer != null);
        this.serializer = serializer;
        return this;
    }

    @Override
    public ConsistentMapBuilder<K, V> withPartitionsDisabled() {
        partitionsEnabled = false;
        return this;
    }

    private boolean validInputs() {
        return name != null && serializer != null;
    }

    @Override
    public ConsistentMap<K, V> build() {
        checkState(validInputs());
        return new DefaultConsistentMap<>(
                name,
                partitionsEnabled ? partitionedDatabase : inMemoryDatabase,
                serializer);
    }

    @Override
    public AsyncConsistentMap<K, V> buildAsyncMap() {
        checkState(validInputs());
        return new DefaultAsyncConsistentMap<>(
                name,
                partitionsEnabled ? partitionedDatabase : inMemoryDatabase,
                serializer);
    }
}