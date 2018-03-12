/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.primitives.impl;

import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.Maps;
import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.service.AsyncAtomicCounter;
import org.onosproject.store.service.AsyncAtomicIdGenerator;
import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.AsyncConsistentTreeMap;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Topic;
import org.onosproject.store.service.WorkQueue;

/**
 * Primitive instance manager.
 */
public class DistributedPrimitiveManager {
    private final Map<String, DistributedPrimitive> primitives = Maps.newConcurrentMap();
    private final DistributedPrimitiveCreator primitiveCreator;

    DistributedPrimitiveManager(DistributedPrimitiveCreator primitiveCreator) {
        this.primitiveCreator = primitiveCreator;
    }

    /**
     * Returns a cached primitive instance.
     *
     * @param name    the primitive name
     * @param factory the primitive factory
     * @param <T>     the primitive type
     * @return the primitive instance
     */
    @SuppressWarnings("unchecked")
    private <T extends DistributedPrimitive> T getPrimitive(String name, Function<String, T> factory) {
        return (T) primitives.computeIfAbsent(name, factory);
    }

    /**
     * Returns an instance of {@code AsyncAtomicCounter} with specified name.
     * @param name counter name
     *
     * @return AsyncAtomicCounter instance
     */
    public AsyncAtomicCounter getAsyncAtomicCounter(String name) {
        return getPrimitive(name, primitiveCreator::newAsyncCounter);
    }

    /**
     * Returns an instance of {@code AsyncAtomicIdGenerator} with specified name.
     *
     * @param name ID generator name
     * @return AsyncAtomicIdGenerator instance
     */
    public AsyncAtomicIdGenerator getAsyncAtomicIdGenerator(String name) {
        return getPrimitive(name, primitiveCreator::newAsyncIdGenerator);
    }

    /**
     * Returns an instance of {@code WorkQueue} with specified name.
     *
     * @param <E> work element type
     * @param name work queue name
     * @param serializer serializer
     * @return WorkQueue instance
     */
    public <E> WorkQueue<E> getWorkQueue(String name, Serializer serializer) {
        return getPrimitive(name, n -> primitiveCreator.newWorkQueue(n, serializer));
    }

    /**
     * Returns an instance of {@code AsyncDocumentTree} with specified name.
     *
     * @param <V> tree node value type
     * @param name document tree name
     * @param serializer serializer
     * @return AsyncDocumentTree instance
     */
    public <V> AsyncDocumentTree<V> getDocumentTree(String name, Serializer serializer) {
        return getPrimitive(name, n -> primitiveCreator.newAsyncDocumentTree(n, serializer));
    }

    /**
     * Returns a set backed instance of {@code AsyncConsistentMultimap} with
     * the specified name.
     *
     * @param name       the multimap name
     * @param serializer serializer
     * @param <K>        key type
     * @param <V>        value type
     * @return set backed {@code AsyncConsistentMultimap} instance
     */
    public <K, V> AsyncConsistentMultimap<K, V> getAsyncSetMultimap(String name, Serializer serializer) {
        return getPrimitive(name, n -> primitiveCreator.newAsyncConsistentSetMultimap(n, serializer));
    }

    /**
     * Returns an instance of {@code AsyncConsistentTreeMap} with the specified
     * name.
     *
     * @param name the treemap name
     * @param serializer serializer
     * @param <V> value type
     * @return set backed {@code AsyncConsistentTreeMap} instance
     */
    public <V> AsyncConsistentTreeMap<V> getAsyncTreeMap(String name, Serializer serializer) {
        return getPrimitive(name, n -> primitiveCreator.newAsyncConsistentTreeMap(n, serializer));
    }

    /**
     * Returns an instance of {@code Topic} with specified name.
     *
     * @param <T> topic message type
     * @param name topic name
     * @param serializer serializer
     *
     * @return Topic instance
     */
    public <T> Topic<T> getTopic(String name, Serializer serializer) {
        AsyncAtomicValue<T> atomicValue =
            getPrimitive(name, n -> primitiveCreator.newAsyncAtomicValue("topic-" + n, serializer));
        return new DefaultDistributedTopic<>(atomicValue);
    }
}
