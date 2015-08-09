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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import org.onosproject.core.ApplicationId;
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
    private ApplicationId applicationId;
    private boolean purgeOnUninstall = false;
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
    public ConsistentMapBuilder<K, V> withApplicationId(ApplicationId id) {
        checkArgument(id != null);
        this.applicationId = id;
        return this;
    }

    @Override
    public ConsistentMapBuilder<K, V> withPurgeOnUninstall() {
        purgeOnUninstall = true;
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

    private void validateInputs() {
        checkState(name != null, "name must be specified");
        checkState(serializer != null, "serializer must be specified");
        if (purgeOnUninstall) {
            checkState(applicationId != null, "ApplicationId must be specified when purgeOnUninstall is enabled");
        }
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
        validateInputs();
        DefaultAsyncConsistentMap<K, V> asyncMap = new DefaultAsyncConsistentMap<>(
                name,
                applicationId,
                partitionsEnabled ? manager.partitionedDatabase : manager.inMemoryDatabase,
                serializer,
                readOnly,
                purgeOnUninstall,
                event -> manager.clusterCommunicator.<MapEvent<K, V>>broadcast(event,
                        DatabaseManager.mapUpdatesSubject(name),
                        serializer::encode));
        return manager.registerMap(asyncMap);
    }
}