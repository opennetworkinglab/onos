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
     * Creates a new DistributedSetBuilder.
     *
     * @param <E> set element type
     * @return builder for an distributed set
     */
    <E> DistributedSetBuilder<E> setBuilder();

    /**
     * Creates a new DistributedQueueBuilder.
     *
     * @param <E> queue entry type
     * @return builder for an distributed queue
     */
    <E> DistributedQueueBuilder<E> queueBuilder();

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
     * Creates a new transaction context builder.
     *
     * @return a builder for a transaction context.
     */
    TransactionContextBuilder transactionContextBuilder();
}
