/*
 * Copyright 2016 Open Networking Laboratory
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

import io.atomix.copycat.client.CopycatClient;
import io.atomix.resource.Resource;
import io.atomix.resource.ResourceTypeInfo;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.onlab.util.Match;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Clear;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.ContainsKey;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.ContainsValue;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.EntrySet;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Get;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.IsEmpty;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.KeySet;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Listen;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Size;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.TransactionCommit;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.TransactionPrepare;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.TransactionRollback;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Unlisten;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.UpdateAndGet;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Values;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.MapTransaction;
import org.onosproject.store.service.Versioned;

import com.google.common.collect.Sets;

/**
 * Distributed resource providing the {@link AsyncConsistentMap} primitive.
 */
@ResourceTypeInfo(id = -151,
                  stateMachine = AtomixConsistentMapState.class,
                  typeResolver = AtomixConsistentMapCommands.TypeResolver.class)
public class AtomixConsistentMap extends Resource<AtomixConsistentMap>
    implements AsyncConsistentMap<String, byte[]> {

    private final Set<MapEventListener<String, byte[]>> mapEventListeners = Sets.newCopyOnWriteArraySet();

    public static final String CHANGE_SUBJECT = "changeEvents";

    public AtomixConsistentMap(CopycatClient client, Resource.Options options) {
        super(client, options);
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public CompletableFuture<AtomixConsistentMap> open() {
        return super.open().thenApply(result -> {
            client.onEvent(CHANGE_SUBJECT, this::handleEvent);
            return result;
        });
    }

    private void handleEvent(List<MapEvent<String, byte[]>> events) {
        events.forEach(event -> mapEventListeners.forEach(listener -> listener.event(event)));
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return submit(new IsEmpty());
    }

    @Override
    public CompletableFuture<Integer> size() {
        return submit(new Size());
    }

    @Override
    public CompletableFuture<Boolean> containsKey(String key) {
        return submit(new ContainsKey(key));
    }

    @Override
    public CompletableFuture<Boolean> containsValue(byte[] value) {
        return submit(new ContainsValue(value));
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> get(String key) {
        return submit(new Get(key));
    }

    @Override
    public CompletableFuture<Set<String>> keySet() {
        return submit(new KeySet());
    }

    @Override
    public CompletableFuture<Collection<Versioned<byte[]>>> values() {
        return submit(new Values());
    }

    @Override
    public CompletableFuture<Set<Entry<String, Versioned<byte[]>>>> entrySet() {
        return submit(new EntrySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> put(String key, byte[] value) {
        return submit(new UpdateAndGet(key, value, Match.ANY, Match.ANY))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.oldValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> putAndGet(String key, byte[] value) {
        return submit(new UpdateAndGet(key, value, Match.ANY, Match.ANY))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.newValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> putIfAbsent(String key, byte[] value) {
        return submit(new UpdateAndGet(key, value, Match.NULL, Match.ANY))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.oldValue());
    }
    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> remove(String key) {
        return submit(new UpdateAndGet(key, null, Match.ANY, Match.ANY))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.oldValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> remove(String key, byte[] value) {
        return submit(new UpdateAndGet(key, null, Match.ifValue(value), Match.ANY))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.updated());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> remove(String key, long version) {
        return submit(new UpdateAndGet(key, null, Match.ANY, Match.ifValue(version)))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.updated());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> replace(String key, byte[] value) {
        return submit(new UpdateAndGet(key, value, Match.NOT_NULL, Match.ANY))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.oldValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> replace(String key, byte[] oldValue, byte[] newValue) {
        return submit(new UpdateAndGet(key,
                newValue,
                Match.ifValue(oldValue),
                Match.ANY))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.updated());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> replace(String key, long oldVersion, byte[] newValue) {
        return submit(new UpdateAndGet(key,
                newValue,
                Match.ANY,
                Match.ifValue(oldVersion)))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.updated());
    }

    @Override
    public CompletableFuture<Void> clear() {
        return submit(new Clear())
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
            return submit(new UpdateAndGet(key,
                    computedValue.get(),
                    valueMatch,
                    versionMatch))
                    .whenComplete((r, e) -> throwIfLocked(r.status()))
                    .thenApply(v -> v.newValue());
        });
    }

    @Override
    public synchronized CompletableFuture<Void> addListener(MapEventListener<String, byte[]> listener) {
        if (mapEventListeners.isEmpty()) {
            return submit(new Listen()).thenRun(() -> mapEventListeners.add(listener));
        } else {
            mapEventListeners.add(listener);
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public synchronized CompletableFuture<Void> removeListener(MapEventListener<String, byte[]> listener) {
        if (mapEventListeners.remove(listener) && mapEventListeners.isEmpty()) {
            return submit(new Unlisten()).thenApply(v -> null);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void throwIfLocked(MapEntryUpdateResult.Status status) {
        if (status == MapEntryUpdateResult.Status.WRITE_LOCK) {
            throw new ConcurrentModificationException("Cannot update map: Another transaction in progress");
        }
    }

    @Override
    public CompletableFuture<Boolean> prepare(MapTransaction<String, byte[]> transaction) {
        return submit(new TransactionPrepare(transaction)).thenApply(v -> v == PrepareResult.OK);
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        return submit(new TransactionCommit(transactionId)).thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        return submit(new TransactionRollback(transactionId))
                .thenApply(v -> null);
    }
}