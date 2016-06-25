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

import java.util.Set;

import org.onosproject.store.service.AsyncAtomicCounter;
import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.AsyncDistributedSet;
import org.onosproject.store.service.AsyncLeaderElector;
import org.onosproject.store.service.DistributedQueue;
import org.onosproject.store.service.Serializer;

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
    <K, V> AsyncConsistentMap<K, V> newAsyncConsistentMap(String name, Serializer serializer);

    /**
     * Creates a new {@code AsyncAtomicCounter}.
     *
     * @param name counter name
     * @return counter
     */
    AsyncAtomicCounter newAsyncCounter(String name);

    /**
     * Creates a new {@code AsyncAtomicValue}.
     *
     * @param name value name
     * @param serializer serializer to use for serializing/deserializing value type
     * @param <V> value type
     * @return value
     */
    <V> AsyncAtomicValue<V> newAsyncAtomicValue(String name, Serializer serializer);

    /**
     * Creates a new {@code DistributedQueue}.
     *
     * @param name queue name
     * @param serializer serializer to use for serializing/deserializing queue entries
     * @param <E> queue entry type
     * @return queue
     */
    <E> DistributedQueue<E> newDistributedQueue(String name, Serializer serializer);

    /**
     * Creates a new {@code AsyncDistributedSet}.
     *
     * @param name set name
     * @param serializer serializer to use for serializing/deserializing set entries
     * @param <E> set entry type
     * @return set
     */
    <E> AsyncDistributedSet<E> newAsyncDistributedSet(String name, Serializer serializer);

    /**
     * Creates a new {@code AsyncLeaderElector}.
     *
     * @param name leader elector name
     * @return leader elector
     */
    AsyncLeaderElector newAsyncLeaderElector(String name);

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
}