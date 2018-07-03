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

package org.onosproject.store.primitives;

import com.google.common.collect.Maps;
import org.onosproject.core.ApplicationId;
import org.onosproject.store.service.AsyncConsistentTreeMap;
import org.onosproject.store.service.AsyncIterator;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentTreeMap;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Version;
import org.onosproject.store.service.Versioned;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AsyncConsistentTreeMapAdapter<V> implements AsyncConsistentTreeMap<V> {

    private TreeMap<String, V> map = Maps.newTreeMap();

    private CompletableFuture<Map.Entry<String, Versioned<V>>> makeVersionedEntry(Map.Entry<String, V> entry) {
        return CompletableFuture.completedFuture(
                new AbstractMap.SimpleImmutableEntry<>(entry.getKey(),
                                                       makeVersioned(entry.getValue())));
    }

    private CompletableFuture<Versioned<V>> makeVersionedFuture(V value) {
        return CompletableFuture.completedFuture(makeVersioned(value));
    }

    private Versioned<V> makeVersioned(V value) {
        return new Versioned<>(value, 1);
    }

    @Override
    public CompletableFuture<String> firstKey() {
        return CompletableFuture.completedFuture(map.firstKey());
    }

    @Override
    public CompletableFuture<String> lastKey() {
        return CompletableFuture.completedFuture(map.lastKey());
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> ceilingEntry(String key) {
        return makeVersionedEntry(map.ceilingEntry(key));
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> floorEntry(String key) {
        return makeVersionedEntry(map.floorEntry(key));
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> higherEntry(String key) {
        return makeVersionedEntry(map.higherEntry(key));
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> lowerEntry(String key) {
        return makeVersionedEntry(map.lowerEntry(key));
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> firstEntry() {
        return makeVersionedEntry(map.firstEntry());
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> lastEntry() {
        return makeVersionedEntry(map.lastEntry());
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> pollFirstEntry() {
        return makeVersionedEntry(map.pollFirstEntry());
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> pollLastEntry() {
        return makeVersionedEntry(map.pollLastEntry());
    }

    @Override
    public CompletableFuture<String> lowerKey(String key) {
        return CompletableFuture.completedFuture(map.lowerKey(key));
    }

    @Override
    public CompletableFuture<String> floorKey(String key) {
        return CompletableFuture.completedFuture(map.floorKey(key));
    }

    @Override
    public CompletableFuture<String> ceilingKey(String key) {
        return CompletableFuture.completedFuture(map.ceilingKey(key));
    }

    @Override
    public CompletableFuture<String> higherKey(String key) {
        return CompletableFuture.completedFuture(map.higherKey(key));
    }

    @Override
    public CompletableFuture<NavigableSet<String>> navigableKeySet() {
        return CompletableFuture.completedFuture(map.navigableKeySet());
    }

    @Override
    public CompletableFuture<NavigableMap<String, V>> subMap(String upperKey, String lowerKey,
                                                             boolean inclusiveUpper, boolean inclusiveLower) {
        NavigableMap<String, V> subMap = map.subMap(upperKey, inclusiveLower, lowerKey, inclusiveLower);
        return CompletableFuture.completedFuture(subMap);
    }

    @Override
    public ConsistentTreeMap<V> asTreeMap() {
        return null;
    }

    @Override
    public ConsistentTreeMap<V> asTreeMap(long timeoutMillis) {
        return null;
    }

    @Override
    public Type primitiveType() {
        return null;
    }

    @Override
    public CompletableFuture<Integer> size() {
        return CompletableFuture.completedFuture(map.size());
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return CompletableFuture.completedFuture(map.isEmpty());
    }

    @Override
    public CompletableFuture<Boolean> containsKey(String key) {
        return CompletableFuture.completedFuture(map.containsKey(key));
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        return CompletableFuture.completedFuture(map.containsValue(value));
    }

    @Override
    public CompletableFuture<Versioned<V>> get(String key) {
        return makeVersionedFuture(map.get(key));
    }

    @Override
    public CompletableFuture<Versioned<V>> getOrDefault(String key, V defaultValue) {
        return makeVersionedFuture(map.getOrDefault(key, defaultValue));
    }

    @Override
    public CompletableFuture<Versioned<V>>
           computeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction) {
        return makeVersionedFuture(map.computeIfAbsent(key, mappingFunction));
    }

    @Override
    public CompletableFuture<Versioned<V>>
           computeIfPresent(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        return makeVersionedFuture(map.computeIfPresent(key, remappingFunction));
    }

    @Override
    public CompletableFuture<Versioned<V>>
           compute(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        return makeVersionedFuture(map.compute(key, remappingFunction));
    }

    @Override
    public CompletableFuture<Versioned<V>>
    computeIf(String key, Predicate<? super V> condition,
              BiFunction<? super String, ? super V, ? extends V> remappingFunction) {

        V value = map.get(key);

        if (condition.test(value)) {
            value = map.compute(key, remappingFunction);
        }
        return makeVersionedFuture(value);
    }

    @Override
    public CompletableFuture<Versioned<V>> put(String key, V value) {
        return makeVersionedFuture(map.put(key, value));
    }

    @Override
    public CompletableFuture<Versioned<V>> putAndGet(String key, V value) {
        return makeVersionedFuture(map.put(key, value));
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(String key) {
        return makeVersionedFuture(map.remove(key));
    }

    @Override
    public CompletableFuture<Void> clear() {
        map.clear();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Set<String>> keySet() {
        return CompletableFuture.completedFuture(map.keySet());
    }

    @Override
    public CompletableFuture<Collection<Versioned<V>>> values() {
        Set<Versioned<V>> valuesAsVersionedCollection =
                map.values().stream().map(this::makeVersioned)
                        .collect(Collectors.toSet());
        return CompletableFuture.completedFuture(valuesAsVersionedCollection);
    }

    @Override
    public CompletableFuture<Set<Map.Entry<String, Versioned<V>>>> entrySet() {
        Map<String, Versioned<V>> valuesAsVersionedMap = new HashMap<>();
        map.forEach((key, value) ->
                valuesAsVersionedMap.put(key, makeVersioned(value))
        );
        return CompletableFuture.completedFuture(valuesAsVersionedMap.entrySet());
    }

    @Override
    public CompletableFuture<Versioned<V>> putIfAbsent(String key, V value) {
        return makeVersionedFuture(map.putIfAbsent(key, value));
    }

    @Override
    public CompletableFuture<Boolean> remove(String key, V value) {
        return CompletableFuture.completedFuture(map.remove(key, value));
    }

    @Override
    public CompletableFuture<Boolean> remove(String key, long version) {
        Object value = map.remove(key);
        return CompletableFuture.completedFuture(value != null);
    }

    @Override
    public CompletableFuture<Versioned<V>> replace(String key, V value) {
        return makeVersionedFuture(map.replace(key, value));
    }

    @Override
    public CompletableFuture<Boolean> replace(String key, V oldValue, V newValue) {
        return CompletableFuture.completedFuture(map.replace(key, oldValue, newValue));
    }

    @Override
    public CompletableFuture<Boolean> replace(String key, long oldVersion, V newValue) {
        map.replace(key, newValue);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Void> addListener(MapEventListener<String, V> listener) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addListener(MapEventListener<String, V> listener, Executor executor) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removeListener(MapEventListener<String, V> listener) {
        return null;
    }

    @Override
    public ConsistentMap<String, V> asConsistentMap() {
        return null;
    }

    @Override
    public ConsistentMap<String, V> asConsistentMap(long timeoutMillis) {
        return null;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public ApplicationId applicationId() {
        return null;
    }

    @Override
    public void addStatusChangeListener(Consumer<Status> listener) {

    }

    @Override
    public void removeStatusChangeListener(Consumer<Status> listener) {

    }

    @Override
    public Collection<Consumer<Status>> statusChangeListeners() {
        return null;
    }

    @Override
    public CompletableFuture<Version> begin(TransactionId transactionId) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> prepare(TransactionLog<MapUpdate<String, V>> transactionLog) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<MapUpdate<String, V>> transactionLog) {
        return null;
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        return null;
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        return null;
    }

    @Override
    public CompletableFuture<AsyncIterator<Map.Entry<String, Versioned<V>>>> iterator() {
        return null;
    }
}
