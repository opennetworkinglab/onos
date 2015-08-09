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
package org.onosproject.store.service;

/**
 * Adapter for the storage service.
 */
public class StorageServiceAdapter implements StorageService {
    @Override
    public <K, V> EventuallyConsistentMapBuilder<K, V> eventuallyConsistentMapBuilder() {
        return null;
    }

    @Override
    public <K, V> ConsistentMapBuilder<K, V> consistentMapBuilder() {
        return null;
    }

    @Override
    public <E> DistributedSetBuilder<E> setBuilder() {
        return null;
    }

    @Override
    public <E> DistributedQueueBuilder<E> queueBuilder() {
        return null;
    }

    @Override
    public AtomicCounterBuilder atomicCounterBuilder() {
        return null;
    }

    @Override
    public <V> AtomicValueBuilder<V> atomicValueBuilder() {
        return null;
    }

    @Override
    public TransactionContextBuilder transactionContextBuilder() {
        return null;
    }
}
