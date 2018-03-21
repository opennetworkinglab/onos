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
package org.onosproject.store.primitives;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.onosproject.store.service.AsyncAtomicCounter;
import org.onosproject.store.service.AsyncAtomicCounterMap;
import org.onosproject.store.service.AsyncAtomicIdGenerator;
import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.AsyncConsistentTreeMap;
import org.onosproject.store.service.AsyncDistributedLock;
import org.onosproject.store.service.AsyncDistributedSet;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.AsyncLeaderElector;
import org.onosproject.store.service.AtomicCounterMapOptions;
import org.onosproject.store.service.AtomicCounterOptions;
import org.onosproject.store.service.AtomicIdGeneratorOptions;
import org.onosproject.store.service.AtomicValueOptions;
import org.onosproject.store.service.ConsistentMapOptions;
import org.onosproject.store.service.ConsistentMultimapOptions;
import org.onosproject.store.service.ConsistentTreeMapOptions;
import org.onosproject.store.service.DistributedLockOptions;
import org.onosproject.store.service.DistributedSetOptions;
import org.onosproject.store.service.DocumentTreeOptions;
import org.onosproject.store.service.LeaderElectorOptions;
import org.onosproject.store.service.Ordering;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.WorkQueue;
import org.onosproject.store.service.WorkQueueOptions;

import static org.onosproject.store.service.DistributedPrimitive.DEFAULT_OPERATION_TIMEOUT_MILLIS;

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
     * @deprecated in Nightingale Release (1.13)
     */
    @Deprecated
    default <K, V> AsyncConsistentMap<K, V> newAsyncConsistentMap(String name, Serializer serializer) {
        return newAsyncConsistentMap((ConsistentMapOptions) new ConsistentMapOptions() {
        }.withName(name).withSerializer(serializer));
    }

    /**
     * Creates a new {@code AsyncConsistentMap}.
     *
     * @param options map options
     * @param <K> key type
     * @param <V> value type
     * @return map
     */
    <K, V> AsyncConsistentMap<K, V> newAsyncConsistentMap(ConsistentMapOptions options);

    /**
     * Creates a new {@code AsyncConsistentTreeMap}.
     *
     * @param name tree name
     * @param serializer serializer to use for serializing/deserializing map entries
     * @param <V> value type
     * @return distributedTreeMap
     * @deprecated in Nightingale Release (1.13)
     */
    @Deprecated
    default <V> AsyncConsistentTreeMap<V> newAsyncConsistentTreeMap(String name, Serializer serializer) {
        return newAsyncConsistentTreeMap((ConsistentTreeMapOptions) new ConsistentTreeMapOptions() {
        }.withName(name).withSerializer(serializer));
    }

    /**
     * Creates a new {@code AsyncConsistentTreeMap}.
     *
     * @param options tree map options
     * @param <V> value type
     * @return distributedTreeMap
     */
    <V> AsyncConsistentTreeMap<V> newAsyncConsistentTreeMap(ConsistentTreeMapOptions options);

    /**
     * Creates a new set backed {@code AsyncConsistentMultimap}.
     *
     * @param name multimap name
     * @param serializer serializer to use for serializing/deserializing
     * @param <K> key type
     * @param <V> value type
     * @return set backed distributedMultimap
     * @deprecated in Nightingale Release (1.13)
     */
    @Deprecated
    default <K, V> AsyncConsistentMultimap<K, V> newAsyncConsistentSetMultimap(String name, Serializer serializer) {
        return newAsyncConsistentSetMultimap((ConsistentMultimapOptions) new ConsistentMultimapOptions() {
        }.withName(name).withSerializer(serializer));
    }

    /**
     * Creates a new set backed {@code AsyncConsistentMultimap}.
     *
     * @param options multimap options
     * @param <K> key type
     * @param <V> value type
     * @return set backed distributedMultimap
     */
    <K, V> AsyncConsistentMultimap<K, V> newAsyncConsistentSetMultimap(ConsistentMultimapOptions options);

    /**
     * Creates a new {@code AsyncAtomicCounterMap}.
     *
     * @param name counter map name
     * @param serializer serializer to use for serializing/deserializing keys
     * @param <K> key type
     * @return atomic counter map
     * @deprecated in Nightingale Release (1.13)
     */
    @Deprecated
    default <K> AsyncAtomicCounterMap<K> newAsyncAtomicCounterMap(String name, Serializer serializer) {
        return newAsyncAtomicCounterMap((AtomicCounterMapOptions) new AtomicCounterMapOptions() {
        }.withName(name).withSerializer(serializer));
    }

    /**
     * Creates a new {@code AsyncAtomicCounterMap}.
     *
     * @param options counter map options
     * @param <K> key type
     * @return atomic counter map
     */
    <K> AsyncAtomicCounterMap<K> newAsyncAtomicCounterMap(AtomicCounterMapOptions options);

    /**
     * Creates a new {@code AsyncAtomicCounter}.
     *
     * @param name counter name
     * @return counter
     * @deprecated in Nightingale Release (1.13)
     */
    @Deprecated
    default AsyncAtomicCounter newAsyncCounter(String name) {
        return newAsyncCounter((AtomicCounterOptions) new AtomicCounterOptions() {
        }.withName(name));
    }

    /**
     * Creates a new {@code AsyncAtomicCounter}.
     *
     * @param options counter options
     * @return counter
     */
    AsyncAtomicCounter newAsyncCounter(AtomicCounterOptions options);

    /**
     * Creates a new {@code AsyncAtomixIdGenerator}.
     *
     * @param name ID generator name
     * @return ID generator
     * @deprecated in Nightingale Release (1.13)
     */
    @Deprecated
    default AsyncAtomicIdGenerator newAsyncIdGenerator(String name) {
        return newAsyncIdGenerator((AtomicIdGeneratorOptions) new AtomicIdGeneratorOptions() {
        }.withName(name));
    }

    /**
     * Creates a new {@code AsyncAtomixIdGenerator}.
     *
     * @param options ID generator options
     * @return ID generator
     */
    AsyncAtomicIdGenerator newAsyncIdGenerator(AtomicIdGeneratorOptions options);

    /**
     * Creates a new {@code AsyncAtomicValue}.
     *
     * @param name value name
     * @param serializer serializer to use for serializing/deserializing value type
     * @param <V> value type
     * @return value
     * @deprecated in Nightingale Release (1.13)
     */
    @Deprecated
    default <V> AsyncAtomicValue<V> newAsyncAtomicValue(String name, Serializer serializer) {
        return newAsyncAtomicValue((AtomicValueOptions) new AtomicValueOptions() {
        }.withName(name).withSerializer(serializer));
    }

    /**
     * Creates a new {@code AsyncAtomicValue}.
     *
     * @param options value options
     * @param <V> value type
     * @return value
     */
    <V> AsyncAtomicValue<V> newAsyncAtomicValue(AtomicValueOptions options);

    /**
     * Creates a new {@code AsyncDistributedSet}.
     *
     * @param name set name
     * @param serializer serializer to use for serializing/deserializing set entries
     * @param <E> set entry type
     * @return set
     * @deprecated in Nightingale Release (1.13)
     */
    @Deprecated
    default <E> AsyncDistributedSet<E> newAsyncDistributedSet(String name, Serializer serializer) {
        return newAsyncDistributedSet((DistributedSetOptions) new DistributedSetOptions() {
        }.withName(name).withSerializer(serializer));
    }

    /**
     * Creates a new {@code AsyncDistributedSet}.
     *
     * @param options set options
     * @param <E> set entry type
     * @return set
     */
    <E> AsyncDistributedSet<E> newAsyncDistributedSet(DistributedSetOptions options);

    /**
     * Creates a new {@code AsyncDistributedLock}.
     *
     * @param name lock name
     * @return lock
     * @deprecated in Nightingale Release (1.13)
     */
    @Deprecated
    default AsyncDistributedLock newAsyncDistributedLock(String name) {
        return newAsyncDistributedLock((DistributedLockOptions) new DistributedLockOptions() {
        }.withName(name));
    }

    /**
     * Creates a new {@code AsyncDistributedLock}.
     *
     * @param options lock options
     * @return lock
     */
    AsyncDistributedLock newAsyncDistributedLock(DistributedLockOptions options);

    /**
     * Creates a new {@code AsyncLeaderElector}.
     *
     * @param name leader elector name
     * @return leader elector
     * @deprecated in Nightingale Release (1.13)
     */
    @Deprecated
    default AsyncLeaderElector newAsyncLeaderElector(String name) {
        return newAsyncLeaderElector(name, DEFAULT_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new {@code AsyncLeaderElector}.
     *
     * @param name leader elector name
     * @param electionTimeout leader election timeout
     * @param timeUnit leader election timeout time unit
     * @return leader elector
     * @deprecated in Nightingale Release (1.13)
     */
    @Deprecated
    default AsyncLeaderElector newAsyncLeaderElector(String name, long electionTimeout, TimeUnit timeUnit) {
        LeaderElectorOptions options = new LeaderElectorOptions() {
        };
        options.withName(name);
        options.withElectionTimeout(electionTimeout, timeUnit);
        return newAsyncLeaderElector(options);
    }

    /**
     * Creates a new {@code AsyncLeaderElector}.
     *
     * @param options leader elector options
     * @return leader elector
     */
    AsyncLeaderElector newAsyncLeaderElector(LeaderElectorOptions options);

    /**
     * Creates a new {@code WorkQueue}.
     *
     * @param <E> work element type
     * @param name work queue name
     * @param serializer serializer
     * @return work queue
     * @deprecated in Nightingale Release (1.13)
     */
    @Deprecated
    default <E> WorkQueue<E> newWorkQueue(String name, Serializer serializer) {
        return newWorkQueue((WorkQueueOptions) new WorkQueueOptions() {
        }.withName(name).withSerializer(serializer));
    }

    /**
     * Creates a new {@code WorkQueue}.
     *
     * @param <E> work element type
     * @param options work queue options
     * @return work queue
     */
    <E> WorkQueue<E> newWorkQueue(WorkQueueOptions options);

    /**
     * Creates a new {@code AsyncDocumentTree}.
     *
     * @param <V> document tree node value type
     * @param name tree name
     * @param serializer serializer
     * @return document tree
     * @deprecated in Nightingale Release (1.13)
     */
    @Deprecated
    default <V> AsyncDocumentTree<V> newAsyncDocumentTree(String name, Serializer serializer) {
        return newAsyncDocumentTree(name, serializer, Ordering.NATURAL);
    }

    /**
     * Creates a new {@code AsyncDocumentTree}.
     *
     * @param <V> document tree node value type
     * @param name tree name
     * @param serializer serializer
     * @param ordering tree node ordering
     * @return document tree
     * @deprecated in Nightingale Release (1.13)
     */
    @Deprecated
    default <V> AsyncDocumentTree<V> newAsyncDocumentTree(String name, Serializer serializer, Ordering ordering) {
        DocumentTreeOptions options = new DocumentTreeOptions() {
        };
        options.withName(name);
        options.withSerializer(serializer);
        options.withOrdering(ordering);
        return newAsyncDocumentTree(options);
    }

    /**
     * Creates a new {@code AsyncDocumentTree}.
     *
     * @param <V> document tree node value type
     * @param options tree options
     * @return document tree
     */
    <V> AsyncDocumentTree<V> newAsyncDocumentTree(DocumentTreeOptions options);

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
