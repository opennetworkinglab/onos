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
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import com.google.common.collect.Maps;
import io.atomix.protocols.raft.proxy.RaftProxy;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Match;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.FloorEntry;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.HigherEntry;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.LowerEntry;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentTreeMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Version;
import org.onosproject.store.service.Versioned;

import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapEvents.CHANGE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.ADD_LISTENER;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.CEILING_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.CEILING_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.CLEAR;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.CONTAINS_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.CONTAINS_VALUE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.CeilingEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.CeilingKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.ContainsKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.ContainsValue;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.ENTRY_SET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.FIRST_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.FIRST_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.FLOOR_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.FLOOR_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.FloorKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.GET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.GET_OR_DEFAULT;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.Get;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.GetOrDefault;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.HIGHER_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.HIGHER_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.HigherKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.IS_EMPTY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.KEY_SET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.LAST_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.LAST_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.LOWER_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.LOWER_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.LowerKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.POLL_FIRST_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.POLL_LAST_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.REMOVE_LISTENER;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.SIZE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.UPDATE_AND_GET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.UpdateAndGet;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.VALUES;

/**
 * Implementation of {@link AsyncConsistentTreeMap}.
 */
public class AtomixConsistentTreeMap extends AbstractRaftPrimitive implements AsyncConsistentTreeMap<byte[]> {
    private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
            .register(KryoNamespaces.BASIC)
            .register(AtomixConsistentTreeMapOperations.NAMESPACE)
            .register(AtomixConsistentTreeMapEvents.NAMESPACE)
            .build());

    private final Map<MapEventListener<String, byte[]>, Executor>
            mapEventListeners = Maps.newConcurrentMap();

    public AtomixConsistentTreeMap(RaftProxy proxy) {
        super(proxy);
        proxy.addEventListener(CHANGE, SERIALIZER::decode, this::handleEvent);
    }

    private void handleEvent(List<MapEvent<String, byte[]>> events) {
        events.forEach(event -> mapEventListeners.
                forEach((listener, executor) ->
                        executor.execute(() ->
                                listener.event(event))));
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return proxy.invoke(IS_EMPTY, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Integer> size() {
        return proxy.invoke(SIZE, SERIALIZER::decode);
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
    public CompletableFuture<Versioned<byte[]>> get(String key) {
        return proxy.invoke(GET, SERIALIZER::encode, new Get(key), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> getOrDefault(String key, byte[] defaultValue) {
        return proxy.invoke(
                GET_OR_DEFAULT,
                SERIALIZER::encode,
                new GetOrDefault(key, defaultValue),
                SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Set<String>> keySet() {
        return proxy.invoke(KEY_SET, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Collection<Versioned<byte[]>>> values() {
        return proxy.invoke(VALUES, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Set<Map.Entry<String, Versioned<byte[]>>>> entrySet() {
        return proxy.invoke(ENTRY_SET, SERIALIZER::decode);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> put(String key, byte[] value) {
        return proxy.<UpdateAndGet, MapEntryUpdateResult<String, byte[]>>invoke(
                UPDATE_AND_GET,
                SERIALIZER::encode,
                new UpdateAndGet(key, value, Match.ANY, Match.ANY),
                SERIALIZER::decode)
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.oldValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> putAndGet(String key, byte[] value) {
        return proxy.<UpdateAndGet, MapEntryUpdateResult<String, byte[]>>invoke(
                UPDATE_AND_GET,
                SERIALIZER::encode,
                new UpdateAndGet(key, value, Match.ANY, Match.ANY),
                SERIALIZER::decode)
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.newValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> putIfAbsent(String key, byte[] value) {
        return proxy.<UpdateAndGet, MapEntryUpdateResult<String, byte[]>>invoke(
                UPDATE_AND_GET,
                SERIALIZER::encode,
                new UpdateAndGet(key, value, Match.NULL, Match.ANY),
                SERIALIZER::decode)
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.oldValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> remove(String key) {
        return proxy.<UpdateAndGet, MapEntryUpdateResult<String, byte[]>>invoke(
                UPDATE_AND_GET,
                SERIALIZER::encode,
                new UpdateAndGet(key, null, Match.ANY, Match.ANY),
                SERIALIZER::decode)
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.oldValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> remove(String key, byte[] value) {
        return proxy.<UpdateAndGet, MapEntryUpdateResult<String, byte[]>>invoke(
                UPDATE_AND_GET,
                SERIALIZER::encode,
                new UpdateAndGet(key, null, Match.ifValue(value), Match.ANY),
                SERIALIZER::decode)
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.updated());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> remove(String key, long version) {
        return proxy.<UpdateAndGet, MapEntryUpdateResult<String, byte[]>>invoke(
                UPDATE_AND_GET,
                SERIALIZER::encode,
                new UpdateAndGet(key, null, Match.ANY, Match.ifValue(version)),
                SERIALIZER::decode)
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.updated());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> replace(String key, byte[] value) {
        return proxy.<UpdateAndGet, MapEntryUpdateResult<String, byte[]>>invoke(
                UPDATE_AND_GET,
                SERIALIZER::encode,
                new UpdateAndGet(key, value, Match.NOT_NULL, Match.ANY),
                SERIALIZER::decode)
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.oldValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> replace(String key, byte[] oldValue, byte[] newValue) {
        return proxy.<UpdateAndGet, MapEntryUpdateResult<String, byte[]>>invoke(
                UPDATE_AND_GET,
                SERIALIZER::encode,
                new UpdateAndGet(key, newValue, Match.ifValue(oldValue), Match.ANY),
                SERIALIZER::decode)
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.updated());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> replace(String key, long oldVersion, byte[] newValue) {
        return proxy.<UpdateAndGet, MapEntryUpdateResult<String, byte[]>>invoke(
                UPDATE_AND_GET,
                SERIALIZER::encode,
                new UpdateAndGet(key, newValue, Match.ANY, Match.ifValue(oldVersion)),
                SERIALIZER::decode)
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.updated());
    }

    @Override
    public CompletableFuture<Void> clear() {
        return proxy.<MapEntryUpdateResult.Status>invoke(CLEAR, SERIALIZER::decode)
                .whenComplete((r, e) -> throwIfLocked(r))
                .thenApply(v -> null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> computeIf(String key,
            Predicate<? super byte[]> condition,
            BiFunction<? super String,
                    ? super byte[],
                    ? extends byte[]> remappingFunction) {
        return get(key).thenCompose(r1 -> {
            byte[] existingValue = r1 == null ? null : r1.value();

            if (!condition.test(existingValue)) {
                return CompletableFuture.completedFuture(r1);
            }

            AtomicReference<byte[]> computedValue = new AtomicReference<byte[]>();
            try {
                computedValue.set(remappingFunction.apply(key, existingValue));
            } catch (Exception e) {
                CompletableFuture<Versioned<byte[]>> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
            if (computedValue.get() == null && r1 == null) {
                return CompletableFuture.completedFuture(null);
            }
            Match<byte[]> valueMatch = r1 == null ? Match.NULL : Match.ANY;
            Match<Long> versionMatch = r1 == null ? Match.ANY : Match.ifValue(r1.version());
            return proxy.<UpdateAndGet, MapEntryUpdateResult<String, byte[]>>invoke(
                    UPDATE_AND_GET,
                    SERIALIZER::encode,
                    new UpdateAndGet(key, computedValue.get(), valueMatch, versionMatch),
                    SERIALIZER::decode)
                    .whenComplete((r, e) -> throwIfLocked(r.status()))
                    .thenApply(v -> v.newValue());
        });
    }

    @Override
    public CompletableFuture<Void> addListener(
            MapEventListener<String, byte[]> listener, Executor executor) {
        if (mapEventListeners.isEmpty()) {
            return proxy.invoke(ADD_LISTENER).thenRun(() ->
                    mapEventListeners.put(listener,
                            executor));
        } else {
            mapEventListeners.put(listener, executor);
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public synchronized CompletableFuture<Void> removeListener(MapEventListener<String, byte[]> listener) {
        if (mapEventListeners.remove(listener) != null &&
                mapEventListeners.isEmpty()) {
            return proxy.invoke(REMOVE_LISTENER)
                    .thenApply(v -> null);
        }
        return CompletableFuture.completedFuture(null);
    }


    private void throwIfLocked(MapEntryUpdateResult.Status status) {
        if (status == MapEntryUpdateResult.Status.WRITE_LOCK) {
            throw new ConcurrentModificationException("Cannot update TreeMap: another update is in progress.");
        }
    }

    @Override
    public CompletableFuture<String> firstKey() {
        return proxy.invoke(FIRST_KEY, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<String> lastKey() {
        return proxy.invoke(LAST_KEY, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> ceilingEntry(String key) {
        return proxy.invoke(CEILING_ENTRY, SERIALIZER::encode, new CeilingEntry(key), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> floorEntry(String key) {
        return proxy.invoke(FLOOR_ENTRY, SERIALIZER::encode, new FloorEntry(key), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> higherEntry(
            String key) {
        return proxy.invoke(HIGHER_ENTRY, SERIALIZER::encode, new HigherEntry(key), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> lowerEntry(
            String key) {
        return proxy.invoke(LOWER_ENTRY, SERIALIZER::encode, new LowerEntry(key), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> firstEntry() {
        return proxy.invoke(FIRST_ENTRY, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> lastEntry() {
        return proxy.invoke(LAST_ENTRY, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> pollFirstEntry() {
        return proxy.invoke(POLL_FIRST_ENTRY, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> pollLastEntry() {
        return proxy.invoke(POLL_LAST_ENTRY, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<String> lowerKey(String key) {
        return proxy.invoke(LOWER_KEY, SERIALIZER::encode, new LowerKey(key), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<String> floorKey(String key) {
        return proxy.invoke(FLOOR_KEY, SERIALIZER::encode, new FloorKey(key), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<String> ceilingKey(String key) {
        return proxy.invoke(CEILING_KEY, SERIALIZER::encode, new CeilingKey(key), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<String> higherKey(String key) {
        return proxy.invoke(HIGHER_KEY, SERIALIZER::encode, new HigherKey(key), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<NavigableSet<String>> navigableKeySet() {
        throw new UnsupportedOperationException("This operation is not yet supported.");
    }

    @Override
    public CompletableFuture<NavigableMap<String, byte[]>> subMap(
            String upperKey, String lowerKey, boolean inclusiveUpper,
            boolean inclusiveLower) {
        throw new UnsupportedOperationException("This operation is not yet supported.");
    }

    @Override
    public CompletableFuture<Version> begin(TransactionId transactionId) {
        throw new UnsupportedOperationException("This operation is not yet supported.");
    }

    @Override
    public CompletableFuture<Boolean> prepare(TransactionLog<MapUpdate<String, byte[]>> transactionLog) {
        throw new UnsupportedOperationException("This operation is not yet supported.");
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<MapUpdate<String, byte[]>> transactionLog) {
        throw new UnsupportedOperationException("This operation is not yet supported.");
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        throw new UnsupportedOperationException("This operation is not yet supported.");
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        throw new UnsupportedOperationException("This operation is not yet supported.");
    }
}