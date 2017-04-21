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

import com.google.common.collect.Maps;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.resource.AbstractResource;
import io.atomix.resource.ResourceTypeInfo;
import org.onlab.util.Match;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.FirstKey;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.FloorEntry;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.HigherEntry;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.LastEntry;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.LowerEntry;
import org.onosproject.store.service.AsyncConsistentTreeMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Version;
import org.onosproject.store.service.Versioned;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.CeilingEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.CeilingKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.Clear;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.ContainsKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.ContainsValue;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.EntrySet;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.FirstEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.FloorKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.Get;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.GetOrDefault;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.HigherKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.IsEmpty;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.KeySet;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.LastKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.Listen;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.LowerKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.PollFirstEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.PollLastEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.Size;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.Unlisten;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.UpdateAndGet;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.Values;

/**
 * Implementation of {@link AsyncConsistentTreeMap}.
 */
@ResourceTypeInfo(id = -155, factory = AtomixConsistentTreeMapFactory.class)
public class AtomixConsistentTreeMap extends AbstractResource<AtomixConsistentTreeMap>
        implements AsyncConsistentTreeMap<byte[]> {

    private final Map<MapEventListener<String, byte[]>, Executor>
            mapEventListeners = Maps.newConcurrentMap();

    public static final String CHANGE_SUBJECT = "changeEvents";

    public AtomixConsistentTreeMap(CopycatClient client, Properties options) {
        super(client, options);
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public CompletableFuture<AtomixConsistentTreeMap> open() {
        return super.open().thenApply(result -> {
            client.onEvent(CHANGE_SUBJECT, this::handleEvent);
            return result;
        });
    }

    private void handleEvent(List<MapEvent<String, byte[]>> events) {
        events.forEach(event -> mapEventListeners.
                forEach((listener, executor) ->
                                executor.execute(() ->
                                                     listener.event(event))));
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
    public CompletableFuture<Versioned<byte[]>> getOrDefault(String key, byte[] defaultValue) {
        return client.submit(new GetOrDefault(key, defaultValue));
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
    public CompletableFuture<Set<Map.Entry<String, Versioned<byte[]>>>> entrySet() {
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
            return client.submit(new UpdateAndGet(key, computedValue.get(),
                                                                      valueMatch, versionMatch))
                    .whenComplete((r, e) -> throwIfLocked(r.status()))
                    .thenApply(v -> v.newValue());
        });
    }

    @Override
    public CompletableFuture<Void> addListener(
            MapEventListener<String, byte[]> listener, Executor executor) {
        if (mapEventListeners.isEmpty()) {
            return client.submit(new Listen()).thenRun(() ->
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
            return client.submit(new Unlisten())
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
        return client.submit(new FirstKey<String>());
    }

    @Override
    public CompletableFuture<String> lastKey() {
        return client.submit(new LastKey<String>());
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> ceilingEntry(String key) {
        return client.submit(new CeilingEntry(key));
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> floorEntry(String key) {
        return client.submit(new FloorEntry<String, Versioned<byte[]>>(key));
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> higherEntry(
            String key) {
        return client.submit(new HigherEntry<String, Versioned<byte[]>>(key));
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> lowerEntry(
            String key) {
        return client.submit(new LowerEntry<>(key));
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> firstEntry() {
        return client.submit(new FirstEntry());
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> lastEntry() {
        return client.submit(new LastEntry<String, Versioned<byte[]>>());
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> pollFirstEntry() {
        return client.submit(new PollFirstEntry());
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<byte[]>>> pollLastEntry() {
        return client.submit(new PollLastEntry());
    }

    @Override
    public CompletableFuture<String> lowerKey(String key) {
        return client.submit(new LowerKey(key));
    }

    @Override
    public CompletableFuture<String> floorKey(String key) {
        return client.submit(new FloorKey(key));
    }

    @Override
    public CompletableFuture<String> ceilingKey(String key) {
        return client.submit(new CeilingKey(key));
    }

    @Override
    public CompletableFuture<String> higherKey(String key) {
        return client.submit(new HigherKey(key));
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
