/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.store.service;

import org.onosproject.store.primitives.AsyncAtomicCounterAdapter;

public class TestStorageService extends StorageServiceAdapter {


    @Override
    public <K, V> EventuallyConsistentMapBuilder<K, V> eventuallyConsistentMapBuilder() {
        return TestEventuallyConsistentMap.builder();
    }

    @Override
    public <K, V> ConsistentMapBuilder<K, V> consistentMapBuilder() {
        return TestConsistentMap.builder();
    }

    @Override
    public <E> DistributedSetBuilder<E> setBuilder() {
        return TestDistributedSet.builder();
    }

    @Override
    public AtomicCounterBuilder atomicCounterBuilder() {
        return AsyncAtomicCounterAdapter.builder();
    }

    @Override
    public AtomicIdGeneratorBuilder atomicIdGeneratorBuilder() {
        return TestAtomicIdGenerator.builder();
    }

    @Override
    public <V> AtomicValueBuilder<V> atomicValueBuilder() {
        return TestAtomicValue.builder();
    }

    @Override
    public TransactionContextBuilder transactionContextBuilder() {
        throw new UnsupportedOperationException("transactionContextBuilder");
    }

    @Override
    public <K, V> ConsistentMultimapBuilder<K, V> consistentMultimapBuilder() {
        return TestConsistentMultimap.builder();
    }

    @Override
    public <T> Topic<T> getTopic(String name, Serializer serializer) {
        return new TestTopic(name);
    }

    @Override
    public <K> AtomicCounterMapBuilder<K> atomicCounterMapBuilder() {
        return TestAtomicCounterMap.builder();
    }
}
