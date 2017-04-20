/*
 * Copyright 2016-present Open Networking Laboratory
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

import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapBuilder;

/**
 * Default {@link AsyncConsistentMap} builder.
 *
 * @param <K> type for map key
 * @param <V> type for map value
 */
public class DefaultConsistentMapBuilder<K, V> extends ConsistentMapBuilder<K, V> {

    private final DistributedPrimitiveCreator primitiveCreator;

    public DefaultConsistentMapBuilder(DistributedPrimitiveCreator primitiveCreator) {
        this.primitiveCreator = primitiveCreator;
    }

    @Override
    public ConsistentMap<K, V> build() {
        return buildAsyncMap().asConsistentMap();
    }

    @Override
    public AsyncConsistentMap<K, V> buildAsyncMap() {
        AsyncConsistentMap<K, V> map = primitiveCreator.newAsyncConsistentMap(name(), serializer(), executorSupplier());
        map = relaxedReadConsistency() ? DistributedPrimitives.newCachingMap(map) : map;
        map = readOnly() ? DistributedPrimitives.newUnmodifiableMap(map) : map;
        return meteringEnabled() ? DistributedPrimitives.newMeteredMap(map) : map;
    }
}