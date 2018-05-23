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
    public <V> DocumentTreeBuilder<V> documentTreeBuilder() {
        return null;
    }

    @Override
    public <E> DistributedSetBuilder<E> setBuilder() {
        return null;
    }

    @Override
    public AtomicCounterBuilder atomicCounterBuilder() {
        return null;
    }

    @Override
    public AtomicIdGeneratorBuilder atomicIdGeneratorBuilder() {
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

    @Override
    public DistributedLockBuilder lockBuilder() {
        return null;
    }

    @Override
    public LeaderElectorBuilder leaderElectorBuilder() {
        return null;
    }

    @Override
    public <T> TopicBuilder<T> topicBuilder() {
        return null;
    }

    @Override
    public <E> WorkQueueBuilder<E> workQueueBuilder() {
        return null;
    }

    @Override
    public <E> WorkQueue<E> getWorkQueue(String name, Serializer serializer) {
        return null;
    }

    @Override
    public <K, V> AsyncConsistentMultimap<K, V> getAsyncSetMultimap(
            String name, Serializer serializer) {
        return null;
    }

    @Override
    public <T> Topic<T> getTopic(String name, Serializer serializer) {
        return null;
    }

    @Override
    public <V> AsyncConsistentTreeMap<V> getAsyncTreeMap(
            String name, Serializer serializer) {
        return null;
    }

    @Override
    public <V> ConsistentTreeMapBuilder<V> consistentTreeMapBuilder() {
        return null;
    }

    @Override
    public <V> AsyncDocumentTree<V> getDocumentTree(String name, Serializer serializer) {
        return null;
    }

    @Override
    public <K, V> ConsistentMultimapBuilder<K, V> consistentMultimapBuilder() {
        return null;
    }

    @Override
    public <K> AtomicCounterMapBuilder<K> atomicCounterMapBuilder() {
        return null;
    }
}
