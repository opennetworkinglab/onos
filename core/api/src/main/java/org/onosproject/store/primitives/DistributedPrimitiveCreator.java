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
package org.onosproject.store.primitives;

import org.onosproject.store.service.AsyncAtomicCounter;
import org.onosproject.store.service.AsyncAtomicCounterMap;
import org.onosproject.store.service.AsyncAtomicIdGenerator;
import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.AsyncConsistentTreeMap;
import org.onosproject.store.service.AsyncDistributedSet;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.AsyncLeaderElector;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.WorkQueue;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Interface for entity that can create instances of different distributed primitives.
 */
public interface DistributedPrimitiveCreator {

    /**
     * Creates a new {@code AsyncConsistentMap}.
     *
     * @param name map name
     * @param serializer serializer to use for serializing/deserializing map entries
     * @param <K> key type
     * @param <V> value type
     * @return map
     */
    default <K, V> AsyncConsistentMap<K, V> newAsyncConsistentMap(String name, Serializer serializer) {
        return newAsyncConsistentMap(name, serializer, null);
    }

    /**
     * Creates a new {@code AsyncConsistentMap}.
     *
     * @param name map name
     * @param serializer serializer to use for serializing/deserializing map entries
     * @param executorSupplier a callback that returns an executor to be used for each partition
     * @param <K> key type
     * @param <V> value type
     * @return map
     */
    <K, V> AsyncConsistentMap<K, V> newAsyncConsistentMap(
            String name, Serializer serializer, Supplier<Executor> executorSupplier);

    /**
     * Creates a new {@code AsyncConsistentTreeMap}.
     *
     * @param name tree name
     * @param serializer serializer to use for serializing/deserializing map entries
     * @param <V> value type
     * @return distributedTreeMap
     */
    default <V> AsyncConsistentTreeMap<V> newAsyncConsistentTreeMap(
            String name, Serializer serializer) {
        return newAsyncConsistentTreeMap(name, serializer, null);
    }

    /**
     * Creates a new {@code AsyncConsistentTreeMap}.
     *
     * @param name tree name
     * @param serializer serializer to use for serializing/deserializing map entries
     * @param executorSupplier a callback that returns an executor to be used for each partition
     * @param <V> value type
     * @return distributedTreeMap
     */
    <V> AsyncConsistentTreeMap<V> newAsyncConsistentTreeMap(
            String name, Serializer serializer, Supplier<Executor> executorSupplier);

    /**
     * Creates a new set backed {@code AsyncConsistentMultimap}.
     *
     * @param name multimap name
     * @param serializer serializer to use for serializing/deserializing
     * @param <K> key type
     * @param <V> value type
     * @return set backed distributedMultimap
     */
    default <K, V> AsyncConsistentMultimap<K, V> newAsyncConsistentSetMultimap(
            String name, Serializer serializer) {
        return newAsyncConsistentSetMultimap(name, serializer, null);
    }

    /**
     * Creates a new set backed {@code AsyncConsistentMultimap}.
     *
     * @param name multimap name
     * @param serializer serializer to use for serializing/deserializing
     * @param executorSupplier a callback that returns an executor to be used for each partition
     * @param <K> key type
     * @param <V> value type
     * @return set backed distributedMultimap
     */
    <K, V> AsyncConsistentMultimap<K, V> newAsyncConsistentSetMultimap(
            String name, Serializer serializer, Supplier<Executor> executorSupplier);

    /**
     * Creates a new {@code AsyncAtomicCounterMap}.
     *
     * @param name counter map name
     * @param serializer serializer to use for serializing/deserializing keys
     * @param <K> key type
     * @return atomic counter map
     */
    default <K> AsyncAtomicCounterMap<K> newAsyncAtomicCounterMap(
            String name, Serializer serializer) {
        return newAsyncAtomicCounterMap(name, serializer, null);
    }

    /**
     * Creates a new {@code AsyncAtomicCounterMap}.
     *
     * @param name counter map name
     * @param serializer serializer to use for serializing/deserializing keys
     * @param executorSupplier a callback that returns an executor to be used for each partition
     * @param <K> key type
     * @return atomic counter map
     */
    <K> AsyncAtomicCounterMap<K> newAsyncAtomicCounterMap(
            String name, Serializer serializer, Supplier<Executor> executorSupplier);

    /**
     * Creates a new {@code AsyncAtomicCounter}.
     *
     * @param name counter name
     * @return counter
     */
    default AsyncAtomicCounter newAsyncCounter(String name) {
        return newAsyncCounter(name, null);
    }

    /**
     * Creates a new {@code AsyncAtomicCounter}.
     *
     * @param name counter name
     * @param executorSupplier a callback that returns an executor to be used asynchronous callbacks
     * @return counter
     */
    AsyncAtomicCounter newAsyncCounter(String name, Supplier<Executor> executorSupplier);

    /**
     * Creates a new {@code AsyncAtomixIdGenerator}.
     *
     * @param name ID generator name
     * @return ID generator
     */
    default AsyncAtomicIdGenerator newAsyncIdGenerator(String name) {
        return newAsyncIdGenerator(name, null);
    }

    /**
     * Creates a new {@code AsyncAtomixIdGenerator}.
     *
     * @param name ID generator name
     * @param executorSupplier a callback that returns an executor to be used asynchronous callbacks
     * @return ID generator
     */
    AsyncAtomicIdGenerator newAsyncIdGenerator(String name, Supplier<Executor> executorSupplier);

    /**
     * Creates a new {@code AsyncAtomicValue}.
     *
     * @param name value name
     * @param serializer serializer to use for serializing/deserializing value type
     * @param <V> value type
     * @return value
     */
    default <V> AsyncAtomicValue<V> newAsyncAtomicValue(String name, Serializer serializer) {
        return newAsyncAtomicValue(name, serializer, null);
    }

    /**
     * Creates a new {@code AsyncAtomicValue}.
     *
     * @param name value name
     * @param serializer serializer to use for serializing/deserializing value type
     * @param executorSupplier a callback that returns an executor to be used asynchronous callbacks
     * @param <V> value type
     * @return value
     */
    <V> AsyncAtomicValue<V> newAsyncAtomicValue(
            String name, Serializer serializer, Supplier<Executor> executorSupplier);

    /**
     * Creates a new {@code AsyncDistributedSet}.
     *
     * @param name set name
     * @param serializer serializer to use for serializing/deserializing set entries
     * @param <E> set entry type
     * @return set
     */
    default <E> AsyncDistributedSet<E> newAsyncDistributedSet(String name, Serializer serializer) {
        return newAsyncDistributedSet(name, serializer, null);
    }

    /**
     * Creates a new {@code AsyncDistributedSet}.
     *
     * @param name set name
     * @param serializer serializer to use for serializing/deserializing set entries
     * @param executorSupplier a callback that returns an executor to be used asynchronous callbacks
     * @param <E> set entry type
     * @return set
     */
    <E> AsyncDistributedSet<E> newAsyncDistributedSet(
            String name, Serializer serializer, Supplier<Executor> executorSupplier);

    /**
     * Creates a new {@code AsyncLeaderElector}.
     *
     * @param name leader elector name
     * @return leader elector
     */
    default AsyncLeaderElector newAsyncLeaderElector(String name) {
        return newAsyncLeaderElector(name, null);
    }

    /**
     * Creates a new {@code AsyncLeaderElector}.
     *
     * @param name leader elector name
     * @param executorSupplier a callback that returns an executor to be used asynchronous callbacks
     * @return leader elector
     */
    AsyncLeaderElector newAsyncLeaderElector(String name, Supplier<Executor> executorSupplier);

    /**
     * Creates a new {@code WorkQueue}.
     *
     * @param <E> work element type
     * @param name work queue name
     * @param serializer serializer
     * @return work queue
     */
    default <E> WorkQueue<E> newWorkQueue(String name, Serializer serializer) {
        return newWorkQueue(name, serializer, null);
    }

    /**
     * Creates a new {@code WorkQueue}.
     *
     * @param <E> work element type
     * @param name work queue name
     * @param serializer serializer
     * @param executorSupplier a callback that returns an executor to be used asynchronous callbacks
     * @return work queue
     */
    <E> WorkQueue<E> newWorkQueue(String name, Serializer serializer, Supplier<Executor> executorSupplier);

    /**
     * Creates a new {@code AsyncDocumentTree}.
     *
     * @param <V> document tree node value type
     * @param name tree name
     * @param serializer serializer
     * @return document tree
     */
    default <V> AsyncDocumentTree<V> newAsyncDocumentTree(String name, Serializer serializer) {
        return newAsyncDocumentTree(name, serializer, null);
    }

    /**
     * Creates a new {@code AsyncDocumentTree}.
     *
     * @param <V> document tree node value type
     * @param name tree name
     * @param serializer serializer
     * @param executorSupplier a callback that returns an executor to be used asynchronous callbacks
     * @return document tree
     */
    <V> AsyncDocumentTree<V> newAsyncDocumentTree(
            String name, Serializer serializer, Supplier<Executor> executorSupplier);

    /**
     * Returns the names of all created {@code AsyncConsistentMap} instances.
     * @return set of {@code AsyncConsistentMap} names
     */
    Set<String> getAsyncConsistentMapNames();

    /**
     * Returns the names of all created {@code AsyncAtomicCounter} instances.
     * @return set of {@code AsyncAtomicCounter} names
     */
    Set<String> getAsyncAtomicCounterNames();

    /**
     * Returns the names of all created {@code WorkQueue} instances.
     * @return set of {@code WorkQueue} names
     */
    Set<String> getWorkQueueNames();
}
