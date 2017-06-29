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
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import io.atomix.protocols.raft.proxy.RaftProxy;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Match;
import org.onlab.util.Tools;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.ContainsKey;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.ContainsValue;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.Get;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.GetOrDefault;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.TransactionBegin;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.TransactionCommit;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.TransactionPrepare;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.TransactionPrepareAndCommit;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.TransactionRollback;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.UpdateAndGet;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Version;
import org.onosproject.store.service.Versioned;

import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapEvents.CHANGE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.ADD_LISTENER;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.BEGIN;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.CLEAR;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.COMMIT;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.CONTAINS_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.CONTAINS_VALUE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.ENTRY_SET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.GET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.GET_OR_DEFAULT;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.IS_EMPTY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.KEY_SET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.PREPARE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.PREPARE_AND_COMMIT;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.REMOVE_LISTENER;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.ROLLBACK;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.SIZE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.UPDATE_AND_GET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.VALUES;

/**
 * Distributed resource providing the {@link AsyncConsistentMap} primitive.
 */
public class AtomixConsistentMap extends AbstractRaftPrimitive implements AsyncConsistentMap<String, byte[]> {
    private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
            .register(KryoNamespaces.BASIC)
            .register(AtomixConsistentMapOperations.NAMESPACE)
            .register(AtomixConsistentMapEvents.NAMESPACE)
            .build());

    private final Map<MapEventListener<String, byte[]>, Executor> mapEventListeners = new ConcurrentHashMap<>();

    public AtomixConsistentMap(RaftProxy proxy) {
        super(proxy);
        proxy.addEventListener(CHANGE, SERIALIZER::decode, this::handleEvent);
        proxy.addStateChangeListener(state -> {
            if (state == RaftProxy.State.CONNECTED && isListening()) {
                proxy.invoke(ADD_LISTENER);
            }
        });
    }

    private void handleEvent(List<MapEvent<String, byte[]>> events) {
        events.forEach(event ->
                mapEventListeners.forEach((listener, executor) -> executor.execute(() -> listener.event(event))));
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
    public CompletableFuture<Set<Entry<String, Versioned<byte[]>>>> entrySet() {
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
            BiFunction<? super String, ? super byte[], ? extends byte[]> remappingFunction) {
        return get(key).thenCompose(r1 -> {
            byte[] existingValue = r1 == null ? null : r1.value();
            // if the condition evaluates to false, return existing value.
            if (!condition.test(existingValue)) {
                return CompletableFuture.completedFuture(r1);
            }

            AtomicReference<byte[]> computedValue = new AtomicReference<>();
            // if remappingFunction throws an exception, return the exception.
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
                    new UpdateAndGet(key,
                            computedValue.get(),
                            valueMatch,
                            versionMatch),
                    SERIALIZER::decode)
                    .whenComplete((r, e) -> throwIfLocked(r.status()))
                    .thenCompose(r -> {
                        if (r.status() == MapEntryUpdateResult.Status.PRECONDITION_FAILED ||
                                r.status() == MapEntryUpdateResult.Status.WRITE_LOCK) {
                            return Tools.exceptionalFuture(new ConsistentMapException.ConcurrentModification());
                        }
                        return CompletableFuture.completedFuture(r);
                    })
                    .thenApply(v -> v.newValue());
        });
    }

    @Override
    public synchronized CompletableFuture<Void> addListener(MapEventListener<String, byte[]> listener,
            Executor executor) {
        if (mapEventListeners.isEmpty()) {
            return proxy.invoke(ADD_LISTENER).thenRun(() -> mapEventListeners.put(listener, executor));
        } else {
            mapEventListeners.put(listener, executor);
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public synchronized CompletableFuture<Void> removeListener(MapEventListener<String, byte[]> listener) {
        if (mapEventListeners.remove(listener) != null && mapEventListeners.isEmpty()) {
            return proxy.invoke(REMOVE_LISTENER).thenApply(v -> null);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void throwIfLocked(MapEntryUpdateResult.Status status) {
        if (status == MapEntryUpdateResult.Status.WRITE_LOCK) {
            throw new ConcurrentModificationException("Cannot update map: Another transaction in progress");
        }
    }

    @Override
    public CompletableFuture<Version> begin(TransactionId transactionId) {
        return proxy.<TransactionBegin, Long>invoke(
                BEGIN,
                SERIALIZER::encode,
                new TransactionBegin(transactionId),
                SERIALIZER::decode)
                .thenApply(Version::new);
    }

    @Override
    public CompletableFuture<Boolean> prepare(TransactionLog<MapUpdate<String, byte[]>> transactionLog) {
        return proxy.<TransactionPrepare, PrepareResult>invoke(
                PREPARE,
                SERIALIZER::encode,
                new TransactionPrepare(transactionLog),
                SERIALIZER::decode)
                .thenApply(v -> v == PrepareResult.OK);
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<MapUpdate<String, byte[]>> transactionLog) {
        return proxy.<TransactionPrepareAndCommit, PrepareResult>invoke(
                PREPARE_AND_COMMIT,
                SERIALIZER::encode,
                new TransactionPrepareAndCommit(transactionLog),
                SERIALIZER::decode)
                .thenApply(v -> v == PrepareResult.OK);
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        return proxy.<TransactionCommit, CommitResult>invoke(
                COMMIT,
                SERIALIZER::encode,
                new TransactionCommit(transactionId),
                SERIALIZER::decode)
                .thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        return proxy.invoke(
                ROLLBACK,
                SERIALIZER::encode,
                new TransactionRollback(transactionId),
                SERIALIZER::decode)
                .thenApply(v -> null);
    }

    private boolean isListening() {
        return !mapEventListeners.isEmpty();
    }
}