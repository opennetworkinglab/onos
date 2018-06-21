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

package org.onosproject.store.primitives.resources.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import io.atomix.protocols.raft.proxy.RaftProxy;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.AsyncIterator;
import org.onosproject.store.service.MultimapEvent;
import org.onosproject.store.service.MultimapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Versioned;

import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapEvents.CHANGE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.ADD_LISTENER;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.CLEAR;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.CLOSE_ITERATOR;
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
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.IteratorBatch;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.IteratorPosition;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.KEYS;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.KEY_SET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.MultiRemove;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.NEXT;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.OPEN_ITERATOR;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.PUT;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.PUT_AND_GET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.Put;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.REMOVE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.REMOVE_ALL;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapOperations.REMOVE_AND_GET;
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
        proxy.addEventListener(CHANGE, SERIALIZER::decode, this::handleChange);
        proxy.addStateChangeListener(state -> {
            if (state == RaftProxy.State.CONNECTED && isListening()) {
                proxy.invoke(ADD_LISTENER);
            }
        });
    }

    private void handleChange(List<MultimapEvent<String, byte[]>> events) {
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
    public CompletableFuture<Versioned<Collection<? extends byte[]>>> putAndGet(String key, byte[] value) {
        return proxy.invoke(
            PUT_AND_GET,
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
    public CompletableFuture<Versioned<Collection<? extends byte[]>>> removeAndGet(String key, byte[] value) {
        return proxy.invoke(REMOVE_AND_GET, SERIALIZER::encode, new MultiRemove(key,
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
    public CompletableFuture<AsyncIterator<Map.Entry<String, byte[]>>> iterator() {
        return proxy.<Long>invoke(OPEN_ITERATOR, SERIALIZER::decode).thenApply(ConsistentMultimapIterator::new);
    }

    /**
     * Consistent multimap iterator.
     */
    private class ConsistentMultimapIterator implements AsyncIterator<Map.Entry<String, byte[]>> {
        private final long id;
        private volatile CompletableFuture<IteratorBatch> batch;
        private volatile CompletableFuture<Void> closeFuture;

        ConsistentMultimapIterator(long id) {
            this.id = id;
            this.batch = CompletableFuture.completedFuture(
                new IteratorBatch(0, Collections.emptyList()));
        }

        /**
         * Returns the current batch iterator or lazily fetches the next batch from the cluster.
         *
         * @return the next batch iterator
         */
        private CompletableFuture<Iterator<Map.Entry<String, byte[]>>> batch() {
            return batch.thenCompose(iterator -> {
                if (iterator != null && !iterator.hasNext()) {
                    batch = fetch(iterator.position());
                    return batch.thenApply(Function.identity());
                }
                return CompletableFuture.completedFuture(iterator);
            });
        }

        /**
         * Fetches the next batch of entries from the cluster.
         *
         * @param position the position from which to fetch the next batch
         * @return the next batch of entries from the cluster
         */
        private CompletableFuture<IteratorBatch> fetch(int position) {
            return proxy.<IteratorPosition, IteratorBatch>invoke(
                NEXT,
                SERIALIZER::encode,
                new IteratorPosition(id, position),
                SERIALIZER::decode)
                .thenCompose(batch -> {
                    if (batch == null) {
                        return close().thenApply(v -> null);
                    }
                    return CompletableFuture.completedFuture(batch);
                });
        }

        /**
         * Closes the iterator.
         *
         * @return future to be completed once the iterator has been closed
         */
        private CompletableFuture<Void> close() {
            if (closeFuture == null) {
                synchronized (this) {
                    if (closeFuture == null) {
                        closeFuture = proxy.invoke(CLOSE_ITERATOR, SERIALIZER::encode, id);
                    }
                }
            }
            return closeFuture;
        }

        @Override
        public CompletableFuture<Boolean> hasNext() {
            return batch().thenApply(iterator -> iterator != null && iterator.hasNext());
        }

        @Override
        public CompletableFuture<Map.Entry<String, byte[]>> next() {
            return batch().thenCompose(iterator -> {
                if (iterator == null) {
                    return Tools.exceptionalFuture(new NoSuchElementException());
                }
                return CompletableFuture.completedFuture(iterator.next());
            });
        }
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
     *
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