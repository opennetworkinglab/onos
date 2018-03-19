/*
 * Copyright 2016-present Open Networking Foundation
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

import org.onosproject.core.Version;
import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.Serializer;

/**
 * Default {@link AsyncConsistentMap} builder.
 *
 * @param <K> type for map key
 * @param <V> type for map value
 */
public class DefaultConsistentMapBuilder<K, V> extends ConsistentMapBuilder<K, V> {

    private final DistributedPrimitiveCreator primitiveCreator;
    private final Version version;

    public DefaultConsistentMapBuilder(DistributedPrimitiveCreator primitiveCreator, Version version) {
        this.primitiveCreator = primitiveCreator;
        this.version = version;
    }

    @Override
    public ConsistentMap<K, V> build() {
        return buildAsyncMap().asConsistentMap();
    }

    @Override
    public AsyncConsistentMap<K, V> buildAsyncMap() {
        AsyncConsistentMap<K, V> map;

        // If a compatibility function is defined, we don't assume CompatibleValue and Version is registered in
        // the user-provided serializer since it's an implementation detail. Instead, we use the user-provided
        // serializer to convert the CompatibleValue value to a raw byte[] and use a separate serializer to encode
        // the CompatibleValue to binary.
        if (compatibilityFunction != null) {
            Serializer serializer = serializer();

            // Convert the byte[] value to CompatibleValue<byte[]>
            AsyncConsistentMap<K, CompatibleValue<byte[]>> rawMap = primitiveCreator.newAsyncConsistentMap(
                withSerializer(Serializer.using(KryoNamespaces.API, CompatibleValue.class)));

            // Convert the CompatibleValue<byte[]> value to CompatibleValue<V> using the user-provided serializer.
            AsyncConsistentMap<K, CompatibleValue<V>> compatibleMap =
                DistributedPrimitives.newTranscodingMap(
                    rawMap,
                    key -> key,
                    key -> key,
                    value -> value == null ? null :
                        new CompatibleValue<byte[]>(serializer.encode(value.value()), value.version()),
                    value -> value == null ? null :
                        new CompatibleValue<V>(serializer.decode(value.value()), value.version()));
            map = DistributedPrimitives.newCompatibleMap(compatibleMap, compatibilityFunction, version());
        } else {
            map = primitiveCreator.newAsyncConsistentMap(name(), serializer());
        }

        map = nullValues() ? map : DistributedPrimitives.newNotNullMap(map);
        map = relaxedReadConsistency() ? DistributedPrimitives.newCachingMap(map) : map;
        map = readOnly() ? DistributedPrimitives.newUnmodifiableMap(map) : map;
        return meteringEnabled() ? DistributedPrimitives.newMeteredMap(map) : map;
    }
}