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

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AtomicValueBuilder;
import org.onosproject.store.service.ConsistentMapBuilder;

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
    public AtomicValueBuilder<V> withExecutorSupplier(Supplier<Executor> executorSupplier) {
        mapBuilder.withExecutorSupplier(executorSupplier);
        return this;
    }

    @Override
    public AsyncAtomicValue<V> build() {
        return new DefaultAsyncAtomicValue<>(checkNotNull(name()),
                                             checkNotNull(serializer()),
                                             mapBuilder.buildAsyncMap());
    }
}
