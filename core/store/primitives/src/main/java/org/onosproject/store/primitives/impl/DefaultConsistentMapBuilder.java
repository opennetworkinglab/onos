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

import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapBuilder;

import static com.google.common.base.Preconditions.checkState;

/**
 * Default Consistent Map builder.
 *
 * @param <K> type for map key
 * @param <V> type for map value
 */
public class DefaultConsistentMapBuilder<K, V> extends ConsistentMapBuilder<K, V> {

    private final DatabaseManager manager;

    public DefaultConsistentMapBuilder(DatabaseManager manager) {
        this.manager = manager;
    }

    private void validateInputs() {
        checkState(name() != null, "name must be specified");
        checkState(serializer() != null, "serializer must be specified");
        if (purgeOnUninstall()) {
            checkState(applicationId() != null, "ApplicationId must be specified when purgeOnUninstall is enabled");
        }
    }

    @Override
    public ConsistentMap<K, V> build() {
        return buildAndRegisterMap().asConsistentMap();
    }

    @Override
    public AsyncConsistentMap<K, V> buildAsyncMap() {
        return buildAndRegisterMap();
    }

    private DefaultAsyncConsistentMap<K, V> buildAndRegisterMap() {
        validateInputs();
        Database database = partitionsDisabled() ? manager.inMemoryDatabase : manager.partitionedDatabase;
        if (relaxedReadConsistency()) {
            return manager.registerMap(
                    new AsyncCachingConsistentMap<>(name(),
                        applicationId(),
                        database,
                        serializer(),
                        readOnly(),
                        purgeOnUninstall(),
                        meteringEnabled()));
        } else {
            return manager.registerMap(
                    new DefaultAsyncConsistentMap<>(name(),
                        applicationId(),
                        database,
                        serializer(),
                        readOnly(),
                        purgeOnUninstall(),
                        meteringEnabled()));
        }
    }
}