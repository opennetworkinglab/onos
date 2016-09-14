/*
 * Copyright 2015-present Open Networking Laboratory
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
 * Storage service.
 * <p>
 * This service provides builders for various distributed primitives.
 * <p>
 * It is expected that services and applications will leverage the primitives indirectly provided by
 * this service for their distributed state management and coordination.
 */
public interface StorageService {

    /**
     * Creates a new EventuallyConsistentMapBuilder.
     *
     * @param <K> key type
     * @param <V> value type
     * @return builder for an eventually consistent map
     */
    <K, V> EventuallyConsistentMapBuilder<K, V> eventuallyConsistentMapBuilder();

    /**
     * Creates a new ConsistentMapBuilder.
     *
     * @param <K> key type
     * @param <V> value type
     * @return builder for a consistent map
     */
    <K, V> ConsistentMapBuilder<K, V> consistentMapBuilder();

    /**
     * Creates a new {@code AsyncConsistentTreeMapBuilder}.
     *
     * @param <V> value type
     * @return builder for a async consistent tree map
     */
    <V> ConsistentTreeMapBuilder<V> consistentTreeMapBuilder();

    /**
     * Creates a new DistributedSetBuilder.
     *
     * @param <E> set element type
     * @return builder for an distributed set
     */
    <E> DistributedSetBuilder<E> setBuilder();

    /**
     * Creates a new AtomicCounterBuilder.
     *
     * @return atomic counter builder
     */
    AtomicCounterBuilder atomicCounterBuilder();

    /**
     * Creates a new AtomicValueBuilder.
     *
     * @param <V> atomic value type
     * @return atomic value builder
     */
    <V> AtomicValueBuilder<V> atomicValueBuilder();

    /**
     * Creates a new LeaderElectorBuilder.
     *
     * @return leader elector builder
     */
    LeaderElectorBuilder leaderElectorBuilder();

    /**
     * Creates a new transaction context builder.
     *
     * @return a builder for a transaction context.
     */
    TransactionContextBuilder transactionContextBuilder();

    /**
     * Returns an instance of {@code AsyncAtomicCounter} with specified name.
     * @param name counter name
     *
     * @return AsyncAtomicCounter instance
     */
    default AsyncAtomicCounter getAsyncAtomicCounter(String name) {
        return atomicCounterBuilder().withName(name).build();
    }

    /**
     * Returns an instance of {@code AtomicCounter} with specified name.
     * @param name counter name
     *
     * @return AtomicCounter instance
     */
    default AtomicCounter getAtomicCounter(String name) {
        return getAsyncAtomicCounter(name).asAtomicCounter();
    }

    /**
     * Returns an instance of {@code WorkQueue} with specified name.
     *
     * @param <E> work element type
     * @param name work queue name
     * @param serializer serializer
     * @return WorkQueue instance
     */
    <E> WorkQueue<E> getWorkQueue(String name, Serializer serializer);

    /**
     * Returns an instance of {@code AsyncDocumentTree} with specified name.
     *
     * @param <V> tree node value type
     * @param name document tree name
     * @param serializer serializer
     * @return AsyncDocumentTree instance
     */
    <V> AsyncDocumentTree<V> getDocumentTree(String name, Serializer serializer);

    /**
     * Returns an instance of {@code Topic} with specified name.
     *
     * @param <T> topic message type
     * @param name topic name
     * @param serializer serializer
     *
     * @return Topic instance
     */
    <T> Topic<T> getTopic(String name, Serializer serializer);
}
