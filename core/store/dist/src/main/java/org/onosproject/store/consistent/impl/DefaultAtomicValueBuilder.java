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

import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicValue;
import org.onosproject.store.service.AtomicValueBuilder;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.Serializer;

/**
 * Default implementation of AtomicValueBuilder.
 *
 * @param <V> value type
 */
public class DefaultAtomicValueBuilder<V> implements AtomicValueBuilder<V> {

    private Serializer serializer;
    private String name;
    private ConsistentMapBuilder<String, byte[]> mapBuilder;
    private boolean metering = true;

    public DefaultAtomicValueBuilder(DatabaseManager manager) {
        mapBuilder = manager.<String, byte[]>consistentMapBuilder()
                            .withName("onos-atomic-values")
                            .withMeteringDisabled()
                            .withSerializer(Serializer.using(KryoNamespaces.BASIC));
    }

    @Override
    public AtomicValueBuilder<V> withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public AtomicValueBuilder<V> withSerializer(Serializer serializer) {
        this.serializer = serializer;
        return this;
    }

    @Override
    public AtomicValueBuilder<V> withPartitionsDisabled() {
        mapBuilder.withPartitionsDisabled();
        return this;
    }

    @Override
    public AtomicValueBuilder<V> withMeteringDisabled() {
        metering = false;
        return this;
    }

    @Override
    public AtomicValue<V> build() {
        return new DefaultAtomicValue<>(mapBuilder.build(), name, metering, serializer);
    }
}