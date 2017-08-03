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
import java.util.function.BiFunction;
import java.util.function.Predicate;

import io.atomix.protocols.raft.proxy.RaftProxy;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.ContainsKey;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.ContainsValue;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.Get;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.GetOrDefault;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.Put;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.Remove;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.RemoveValue;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.RemoveVersion;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.Replace;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.ReplaceValue;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.ReplaceVersion;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.TransactionBegin;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.TransactionCommit;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.TransactionPrepare;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.TransactionPrepareAndCommit;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.TransactionRollback;
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
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.PUT;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.PUT_AND_GET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.PUT_IF_ABSENT;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.REMOVE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.REMOVE_LISTENER;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.REMOVE_VALUE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.REMOVE_VERSION;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.REPLACE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.REPLACE_VALUE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.REPLACE_VERSION;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.ROLLBACK;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.SIZE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.VALUES;

/**
 * Distributed resource providing the {@link AsyncConsistentMap} primitive.
 */
public class AtomixConsistentMap extends AbstractRaftPrimitive implements AsyncConsistentMap<String, byte[]> {
    private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
            .register(KryoNamespaces.BASIC)
            .register(AtomixConsistentMapOperations.NAMESPACE)
            .register(AtomixConsistentMapEvents.NAMESPACE)
            .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID + 100)
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

    protected Serializer serializer() {
        return SERIALIZER;
    }

    private void handleEvent(List<MapEvent<String, byte[]>> events) {
        events.forEach(event ->
                mapEventListeners.forEach((listener, executor) -> executor.execute(() -> listener.event(event))));
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return proxy.invoke(IS_EMPTY, serializer()::decode);
    }

    @Override
    public CompletableFuture<Integer> size() {
        return proxy.invoke(SIZE, serializer()::decode);
    }

    @Override
    public CompletableFuture<Boolean> containsKey(String key) {
        return proxy.invoke(CONTAINS_KEY, serializer()::encode, new ContainsKey(key), serializer()::decode);
    }

    @Override
    public CompletableFuture<Boolean> containsValue(byte[] value) {
        return proxy.invoke(CONTAINS_VALUE, serializer()::encode, new ContainsValue(value), serializer()::decode);
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> get(String key) {
        return proxy.invoke(GET, serializer()::encode, new Get(key), serializer()::decode);
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> getOrDefault(String key, byte[] defaultValue) {
        return proxy.invoke(
                GET_OR_DEFAULT,
                serializer()::encode,
                new GetOrDefault(key, defaultValue),
                serializer()::decode);
    }

    @Override
    public CompletableFuture<Set<String>> keySet() {
        return proxy.invoke(KEY_SET, serializer()::decode);
    }

    @Override
    public CompletableFuture<Collection<Versioned<byte[]>>> values() {
        return proxy.invoke(VALUES, serializer()::decode);
    }

    @Override
    public CompletableFuture<Set<Entry<String, Versioned<byte[]>>>> entrySet() {
        return proxy.invoke(ENTRY_SET, serializer()::decode);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> put(String key, byte[] value) {
        return proxy.<Put, MapEntryUpdateResult<String, byte[]>>invoke(
                PUT,
                serializer()::encode,
                new Put(key, value),
                serializer()::decode)
                .whenComplete((r, e) -> throwIfLocked(r))
                .thenApply(v -> v.result());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> putAndGet(String key, byte[] value) {
        return proxy.<Put, MapEntryUpdateResult<String, byte[]>>invoke(
                PUT_AND_GET,
                serializer()::encode,
                new Put(key, value),
                serializer()::decode)
                .whenComplete((r, e) -> throwIfLocked(r))
                .thenApply(v -> v.result());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> putIfAbsent(String key, byte[] value) {
        return proxy.<Put, MapEntryUpdateResult<String, byte[]>>invoke(
                PUT_IF_ABSENT,
                serializer()::encode,
                new Put(key, value),
                serializer()::decode)
                .whenComplete((r, e) -> throwIfLocked(r))
                .thenApply(v -> v.result());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> remove(String key) {
        return proxy.<Remove, MapEntryUpdateResult<String, byte[]>>invoke(
                REMOVE,
                serializer()::encode,
                new Remove(key),
                serializer()::decode)
                .whenComplete((r, e) -> throwIfLocked(r))
                .thenApply(v -> v.result());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> remove(String key, byte[] value) {
        return proxy.<RemoveValue, MapEntryUpdateResult<String, byte[]>>invoke(
                REMOVE_VALUE,
                serializer()::encode,
                new RemoveValue(key, value),
                serializer()::decode)
                .whenComplete((r, e) -> throwIfLocked(r))
                .thenApply(v -> v.updated());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> remove(String key, long version) {
        return proxy.<RemoveVersion, MapEntryUpdateResult<String, byte[]>>invoke(
                REMOVE_VERSION,
                serializer()::encode,
                new RemoveVersion(key, version),
                serializer()::decode)
                .whenComplete((r, e) -> throwIfLocked(r))
                .thenApply(v -> v.updated());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> replace(String key, byte[] value) {
        return proxy.<Replace, MapEntryUpdateResult<String, byte[]>>invoke(
                REPLACE,
                serializer()::encode,
                new Replace(key, value),
                serializer()::decode)
                .whenComplete((r, e) -> throwIfLocked(r))
                .thenApply(v -> v.result());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> replace(String key, byte[] oldValue, byte[] newValue) {
        return proxy.<ReplaceValue, MapEntryUpdateResult<String, byte[]>>invoke(
                REPLACE_VALUE,
                serializer()::encode,
                new ReplaceValue(key, oldValue, newValue),
                serializer()::decode)
                .whenComplete((r, e) -> throwIfLocked(r))
                .thenApply(v -> v.updated());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> replace(String key, long oldVersion, byte[] newValue) {
        return proxy.<ReplaceVersion, MapEntryUpdateResult<String, byte[]>>invoke(
                REPLACE_VERSION,
                serializer()::encode,
                new ReplaceVersion(key, oldVersion, newValue),
                serializer()::decode)
                .whenComplete((r, e) -> throwIfLocked(r))
                .thenApply(v -> v.updated());
    }

    @Override
    public CompletableFuture<Void> clear() {
        return proxy.<MapEntryUpdateResult.Status>invoke(CLEAR, serializer()::decode)
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

            byte[] computedValue;
            try {
                computedValue = remappingFunction.apply(key, existingValue);
            } catch (Exception e) {
                return Tools.exceptionalFuture(e);
            }

            if (computedValue == null && r1 == null) {
                return CompletableFuture.completedFuture(null);
            }

            if (r1 == null) {
                return proxy.<Put, MapEntryUpdateResult<String, byte[]>>invoke(
                        PUT_IF_ABSENT,
                        serializer()::encode,
                        new Put(key, computedValue),
                        serializer()::decode)
                        .whenComplete((r, e) -> throwIfLocked(r))
                        .thenCompose(r -> checkLocked(r))
                        .thenApply(result -> new Versioned<>(computedValue, result.version()));
            } else if (computedValue == null) {
                return proxy.<RemoveVersion, MapEntryUpdateResult<String, byte[]>>invoke(
                        REMOVE_VERSION,
                        serializer()::encode,
                        new RemoveVersion(key, r1.version()),
                        serializer()::decode)
                        .whenComplete((r, e) -> throwIfLocked(r))
                        .thenCompose(r -> checkLocked(r))
                        .thenApply(v -> null);
            } else {
                return proxy.<ReplaceVersion, MapEntryUpdateResult<String, byte[]>>invoke(
                        REPLACE_VERSION,
                        serializer()::encode,
                        new ReplaceVersion(key, r1.version(), computedValue),
                        serializer()::decode)
                        .whenComplete((r, e) -> throwIfLocked(r))
                        .thenCompose(r -> checkLocked(r))
                        .thenApply(result -> result.status() == MapEntryUpdateResult.Status.OK
                                ? new Versioned(computedValue, result.version()) : result.result());
            }
        });
    }

    private CompletableFuture<MapEntryUpdateResult<String, byte[]>> checkLocked(
            MapEntryUpdateResult<String, byte[]> result) {
        if (result.status() == MapEntryUpdateResult.Status.PRECONDITION_FAILED ||
                result.status() == MapEntryUpdateResult.Status.WRITE_LOCK) {
            return Tools.exceptionalFuture(new ConsistentMapException.ConcurrentModification());
        }
        return CompletableFuture.completedFuture(result);
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

    private void throwIfLocked(MapEntryUpdateResult<String, byte[]> result) {
        if (result != null) {
            throwIfLocked(result.status());
        }
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
                serializer()::encode,
                new TransactionBegin(transactionId),
                serializer()::decode)
                .thenApply(Version::new);
    }

    @Override
    public CompletableFuture<Boolean> prepare(TransactionLog<MapUpdate<String, byte[]>> transactionLog) {
        return proxy.<TransactionPrepare, PrepareResult>invoke(
                PREPARE,
                serializer()::encode,
                new TransactionPrepare(transactionLog),
                serializer()::decode)
                .thenApply(v -> v == PrepareResult.OK);
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<MapUpdate<String, byte[]>> transactionLog) {
        return proxy.<TransactionPrepareAndCommit, PrepareResult>invoke(
                PREPARE_AND_COMMIT,
                serializer()::encode,
                new TransactionPrepareAndCommit(transactionLog),
                serializer()::decode)
                .thenApply(v -> v == PrepareResult.OK);
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        return proxy.<TransactionCommit, CommitResult>invoke(
                COMMIT,
                serializer()::encode,
                new TransactionCommit(transactionId),
                serializer()::decode)
                .thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        return proxy.invoke(
                ROLLBACK,
                serializer()::encode,
                new TransactionRollback(transactionId),
                serializer()::decode)
                .thenApply(v -> null);
    }

    private boolean isListening() {
        return !mapEventListeners.isEmpty();
    }
}