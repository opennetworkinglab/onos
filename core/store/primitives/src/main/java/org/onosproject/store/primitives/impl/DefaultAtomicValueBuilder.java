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

import java.util.function.Supplier;

import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AtomicValueBuilder;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.Serializer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of AtomicValueBuilder.
 *
 * @param <V> value type
 */
public class DefaultAtomicValueBuilder<V> extends AtomicValueBuilder<V> {

    private ConsistentMapBuilder<String, byte[]> mapBuilder;

    public DefaultAtomicValueBuilder(Supplier<ConsistentMapBuilder<String, byte[]>> mapBuilderSupplier) {
        mapBuilder = mapBuilderSupplier.get();
    }

    @Override
    public AsyncAtomicValue<V> build() {
        if (compatibilityFunction != null) {
            Serializer serializer = Serializer.using(KryoNamespaces.API, CompatibleValue.class);

            AsyncAtomicValue<CompatibleValue<byte[]>> rawValue = new DefaultAsyncAtomicValue<>(
                checkNotNull(name()), serializer, mapBuilder.buildAsyncMap());

            AsyncAtomicValue<CompatibleValue<V>> compatibleValue =
                DistributedPrimitives.newTranscodingAtomicValue(
                    rawValue,
                    value -> value == null ? null :
                        new CompatibleValue<byte[]>(serializer().encode(value.value()), value.version()),
                    value -> value == null ? null :
                        new CompatibleValue<V>(serializer().decode(value.value()), value.version()));
            return DistributedPrimitives.newCompatibleAtomicValue(compatibleValue, compatibilityFunction, version());
        }
        return new DefaultAsyncAtomicValue<>(
            checkNotNull(name()),
            checkNotNull(serializer()),
            mapBuilder.buildAsyncMap());
    }
}
