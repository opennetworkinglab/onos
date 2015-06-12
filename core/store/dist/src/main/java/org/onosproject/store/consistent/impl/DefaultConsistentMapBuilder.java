package org.onosproject.store.consistent.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.MapEvent;
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
    private boolean readOnly = false;
    private final DatabaseManager manager;

    public DefaultConsistentMapBuilder(DatabaseManager manager) {
        this.manager = manager;
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

    @Override
    public ConsistentMapBuilder<K, V> withUpdatesDisabled() {
        readOnly = true;
        return this;
    }

    private boolean validInputs() {
        return name != null && serializer != null;
    }

    @Override
    public ConsistentMap<K, V> build() {
        return new DefaultConsistentMap<>(buildAndRegisterMap());
    }

    @Override
    public AsyncConsistentMap<K, V> buildAsyncMap() {
        return buildAndRegisterMap();
    }

    private DefaultAsyncConsistentMap<K, V> buildAndRegisterMap() {
        checkState(validInputs());
        DefaultAsyncConsistentMap<K, V> asyncMap = new DefaultAsyncConsistentMap<>(
                name,
                partitionsEnabled ? manager.partitionedDatabase : manager.inMemoryDatabase,
                serializer,
                readOnly,
                event -> manager.clusterCommunicator.<MapEvent<K, V>>broadcast(event,
                        DatabaseManager.mapUpdatesSubject(name),
                        serializer::encode));
        return manager.registerMap(asyncMap);
    }
}