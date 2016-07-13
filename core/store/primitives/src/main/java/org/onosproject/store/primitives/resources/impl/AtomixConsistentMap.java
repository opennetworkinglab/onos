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

import io.atomix.copycat.client.CopycatClient;
import io.atomix.resource.AbstractResource;
import io.atomix.resource.ResourceTypeInfo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.onlab.util.Match;
import org.onlab.util.Tools;
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
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.TransactionPrepareAndCommit;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.TransactionRollback;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Unlisten;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.UpdateAndGet;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Values;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.MapTransaction;
import org.onosproject.store.service.Versioned;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Distributed resource providing the {@link AsyncConsistentMap} primitive.
 */
@ResourceTypeInfo(id = -151, factory = AtomixConsistentMapFactory.class)
public class AtomixConsistentMap extends AbstractResource<AtomixConsistentMap>
    implements AsyncConsistentMap<String, byte[]> {

    private final Set<Consumer<Status>> statusChangeListeners = Sets.newCopyOnWriteArraySet();
    private final Map<MapEventListener<String, byte[]>, Executor> mapEventListeners = new ConcurrentHashMap<>();

    public static final String CHANGE_SUBJECT = "changeEvents";

    public AtomixConsistentMap(CopycatClient client, Properties properties) {
        super(client, properties);
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public CompletableFuture<AtomixConsistentMap> open() {
        return super.open().thenApply(result -> {
            client.onStateChange(state -> {
                if (state == CopycatClient.State.CONNECTED && isListening()) {
                    client.submit(new Listen());
                }
            });
            client.onEvent(CHANGE_SUBJECT, this::handleEvent);
            return result;
        });
    }

    private void handleEvent(List<MapEvent<String, byte[]>> events) {
        events.forEach(event ->
            mapEventListeners.forEach((listener, executor) -> executor.execute(() -> listener.event(event))));
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return client.submit(new IsEmpty());
    }

    @Override
    public CompletableFuture<Integer> size() {
        return client.submit(new Size());
    }

    @Override
    public CompletableFuture<Boolean> containsKey(String key) {
        return client.submit(new ContainsKey(key));
    }

    @Override
    public CompletableFuture<Boolean> containsValue(byte[] value) {
        return client.submit(new ContainsValue(value));
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> get(String key) {
        return client.submit(new Get(key));
    }

    @Override
    public CompletableFuture<Set<String>> keySet() {
        return client.submit(new KeySet());
    }

    @Override
    public CompletableFuture<Collection<Versioned<byte[]>>> values() {
        return client.submit(new Values());
    }

    @Override
    public CompletableFuture<Set<Entry<String, Versioned<byte[]>>>> entrySet() {
        return client.submit(new EntrySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> put(String key, byte[] value) {
        return client.submit(new UpdateAndGet(key, value, Match.ANY, Match.ANY))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.oldValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> putAndGet(String key, byte[] value) {
        return client.submit(new UpdateAndGet(key, value, Match.ANY, Match.ANY))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.newValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> putIfAbsent(String key, byte[] value) {
        return client.submit(new UpdateAndGet(key, value, Match.NULL, Match.ANY))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.oldValue());
    }
    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> remove(String key) {
        return client.submit(new UpdateAndGet(key, null, Match.ANY, Match.ANY))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.oldValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> remove(String key, byte[] value) {
        return client.submit(new UpdateAndGet(key, null, Match.ifValue(value), Match.ANY))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.updated());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> remove(String key, long version) {
        return client.submit(new UpdateAndGet(key, null, Match.ANY, Match.ifValue(version)))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.updated());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<byte[]>> replace(String key, byte[] value) {
        return client.submit(new UpdateAndGet(key, value, Match.NOT_NULL, Match.ANY))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.oldValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> replace(String key, byte[] oldValue, byte[] newValue) {
        return client.submit(new UpdateAndGet(key, newValue, Match.ifValue(oldValue), Match.ANY))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.updated());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> replace(String key, long oldVersion, byte[] newValue) {
        return client.submit(new UpdateAndGet(key, newValue, Match.ANY, Match.ifValue(oldVersion)))
                .whenComplete((r, e) -> throwIfLocked(r.status()))
                .thenApply(v -> v.updated());
    }

    @Override
    public CompletableFuture<Void> clear() {
        return client.submit(new Clear())
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
            return client.submit(new UpdateAndGet(key,
                                                  computedValue.get(),
                                                  valueMatch,
                                                  versionMatch))
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
            return client.submit(new Listen()).thenRun(() -> mapEventListeners.put(listener, executor));
        } else {
            mapEventListeners.put(listener, executor);
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public synchronized CompletableFuture<Void> removeListener(MapEventListener<String, byte[]> listener) {
        if (mapEventListeners.remove(listener) != null && mapEventListeners.isEmpty()) {
            return client.submit(new Unlisten()).thenApply(v -> null);
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
        return client.submit(new TransactionPrepare(transaction)).thenApply(v -> v == PrepareResult.OK);
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        return client.submit(new TransactionCommit(transactionId)).thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        return client.submit(new TransactionRollback(transactionId))
                .thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(MapTransaction<String, byte[]> transaction) {
        return client.submit(new TransactionPrepareAndCommit(transaction)).thenApply(v -> v == PrepareResult.OK);
    }

    @Override
    public void addStatusChangeListener(Consumer<Status> listener) {
        statusChangeListeners.add(listener);
    }

    @Override
    public void removeStatusChangeListener(Consumer<Status> listener) {
        statusChangeListeners.remove(listener);
    }

    @Override
    public Collection<Consumer<Status>> statusChangeListeners() {
        return ImmutableSet.copyOf(statusChangeListeners);
    }

    private boolean isListening() {
        return !mapEventListeners.isEmpty();
    }
}
