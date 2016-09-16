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
package org.onosproject.pcelabelstore.util;

import org.onosproject.store.service.AtomicCounterBuilder;
import org.onosproject.store.service.AtomicValueBuilder;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.DistributedSetBuilder;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.TransactionContextBuilder;

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
        return TestAtomicCounter.builder();
    }

    @Override
    public <V> AtomicValueBuilder<V> atomicValueBuilder() {
        throw new UnsupportedOperationException("atomicValueBuilder");
    }

    @Override
    public TransactionContextBuilder transactionContextBuilder() {
        throw new UnsupportedOperationException("transactionContextBuilder");
    }
}
