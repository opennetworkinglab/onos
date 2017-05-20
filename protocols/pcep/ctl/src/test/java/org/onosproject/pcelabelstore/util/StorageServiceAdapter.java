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

import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.AsyncConsistentTreeMap;
import org.onosproject.store.service.AtomicCounterBuilder;
import org.onosproject.store.service.AtomicCounterMapBuilder;
import org.onosproject.store.service.AtomicIdGeneratorBuilder;
import org.onosproject.store.service.AtomicValueBuilder;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.ConsistentMultimapBuilder;
import org.onosproject.store.service.ConsistentTreeMapBuilder;
import org.onosproject.store.service.DistributedSetBuilder;
import org.onosproject.store.service.DocumentTreeBuilder;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.LeaderElectorBuilder;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Topic;
import org.onosproject.store.service.TransactionContextBuilder;
import org.onosproject.store.service.WorkQueue;

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
    public LeaderElectorBuilder leaderElectorBuilder() {
        return null;
    }

    @Override
    public <E> WorkQueue<E> getWorkQueue(String name, Serializer serializer) {
        return null;
    }

    @Override
    public <K, V> AsyncConsistentMultimap<K, V> getAsyncSetMultimap(String name, Serializer serializer) {
        return null;
    }

    @Override
    public <V> AsyncConsistentTreeMap<V> getAsyncTreeMap(String name, Serializer serializer) {
        return null;
    }

    @Override
    public <T> Topic<T> getTopic(String name, Serializer serializer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <V> ConsistentTreeMapBuilder<V> consistentTreeMapBuilder() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <V> AsyncDocumentTree<V> getDocumentTree(String name, Serializer serializer) {
        return null;
    }

    public <K, V> ConsistentMultimapBuilder<K, V> consistentMultimapBuilder() {
        return null;
    }

    @Override
    public <K> AtomicCounterMapBuilder<K> atomicCounterMapBuilder() {
        return null;
    }
}
