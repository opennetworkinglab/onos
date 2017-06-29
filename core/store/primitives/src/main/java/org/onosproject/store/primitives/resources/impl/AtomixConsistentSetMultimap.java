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

package org.onosproject.store.primitives.resources.impl;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import io.atomix.protocols.raft.proxy.RaftProxy;
import org.onlab.util.KryoNamespace;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.MultimapEvent;
import org.onosproject.store.service.MultimapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Versioned;

import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapEvents.CHANGE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.ADD_LISTENER;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.CLEAR;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.CONTAINS_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.CONTAINS_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.CONTAINS_VALUE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.ContainsEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.ContainsKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.ContainsValue;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.ENTRIES;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.GET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.Get;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.IS_EMPTY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.KEYS;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.KEY_SET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.MultiRemove;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.PUT;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.Put;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.REMOVE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.REMOVE_ALL;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.REMOVE_LISTENER;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.REPLACE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.RemoveAll;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.Replace;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.SIZE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.VALUES;


/**
 * Set based implementation of the {@link AsyncConsistentMultimap}.
 * <p>
 * Note: this implementation does not allow null entries or duplicate entries.
 */
public class AtomixConsistentSetMultimap
        extends AbstractRaftPrimitive
        implements AsyncConsistentMultimap<String, byte[]> {

    private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
            .register(KryoNamespaces.BASIC)
            .register(AtomixConsistentSetMultimapOperations.NAMESPACE)
            .register(AtomixConsistentSetMultimapEvents.NAMESPACE)
            .build());

    private final Map<MultimapEventListener<String, byte[]>, Executor> mapEventListeners = new ConcurrentHashMap<>();

    public AtomixConsistentSetMultimap(RaftProxy proxy) {
        super(proxy);
        proxy.addEventListener(CHANGE, SERIALIZER::decode, this::handleEvent);
        proxy.addStateChangeListener(state -> {
            if (state == RaftProxy.State.CONNECTED && isListening()) {
                proxy.invoke(ADD_LISTENER);
            }
        });
    }

    private void handleEvent(List<MultimapEvent<String, byte[]>> events) {
        events.forEach(event ->
                mapEventListeners.forEach((listener, executor) -> executor.execute(() -> listener.event(event))));
    }

    @Override
    public CompletableFuture<Integer> size() {
        return proxy.invoke(SIZE, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return proxy.invoke(IS_EMPTY, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Boolean> containsKey(String key) {
        return proxy.invoke(CONTAINS_KEY, SERIALIZER::encode, new ContainsKey(key), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Boolean> containsValue(byte[] value) {
        return proxy.invoke(CONTAINS_VALUE, SERIALIZER::encode, new ContainsValue(value), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Boolean> containsEntry(String key, byte[] value) {
        return proxy.invoke(CONTAINS_ENTRY, SERIALIZER::encode, new ContainsEntry(key, value), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Boolean> put(String key, byte[] value) {
        return proxy.invoke(
                PUT,
                SERIALIZER::encode,
                new Put(key, Lists.newArrayList(value), null),
                SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Boolean> remove(String key, byte[] value) {
        return proxy.invoke(REMOVE, SERIALIZER::encode, new MultiRemove(key,
                Lists.newArrayList(value),
                null), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Boolean> removeAll(String key, Collection<? extends byte[]> values) {
        return proxy.invoke(
                REMOVE,
                SERIALIZER::encode,
                new MultiRemove(key, (Collection<byte[]>) values, null),
                SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends byte[]>>> removeAll(String key) {
        return proxy.invoke(REMOVE_ALL, SERIALIZER::encode, new RemoveAll(key, null), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Boolean> putAll(
            String key, Collection<? extends byte[]> values) {
        return proxy.invoke(PUT, SERIALIZER::encode, new Put(key, values, null), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends byte[]>>> replaceValues(
            String key, Collection<byte[]> values) {
        return proxy.invoke(
                REPLACE,
                SERIALIZER::encode,
                new Replace(key, values, null),
                SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Void> clear() {
        return proxy.invoke(CLEAR);
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends byte[]>>> get(String key) {
        return proxy.invoke(GET, SERIALIZER::encode, new Get(key), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Set<String>> keySet() {
        return proxy.invoke(KEY_SET, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Multiset<String>> keys() {
        return proxy.invoke(KEYS, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Multiset<byte[]>> values() {
        return proxy.invoke(VALUES, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Collection<Map.Entry<String, byte[]>>> entries() {
        return proxy.invoke(ENTRIES, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Void> addListener(MultimapEventListener<String, byte[]> listener, Executor executor) {
        if (mapEventListeners.isEmpty()) {
            return proxy.invoke(ADD_LISTENER).thenRun(() -> mapEventListeners.put(listener, executor));
        } else {
            mapEventListeners.put(listener, executor);
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public CompletableFuture<Void> removeListener(MultimapEventListener<String, byte[]> listener) {
        if (mapEventListeners.remove(listener) != null && mapEventListeners.isEmpty()) {
            return proxy.invoke(REMOVE_LISTENER).thenApply(v -> null);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Map<String, Collection<byte[]>>> asMap() {
        throw new UnsupportedOperationException("Expensive operation.");
    }

    /**
     * Helper to check if there was a lock based issue.
     * @param status the status of an update result
     */
    private void throwIfLocked(MapEntryUpdateResult.Status status) {
        if (status == MapEntryUpdateResult.Status.WRITE_LOCK) {
            throw new ConcurrentModificationException("Cannot update map: " +
                    "Another transaction " +
                    "in progress");
        }
    }

    private boolean isListening() {
        return !mapEventListeners.isEmpty();
    }
}