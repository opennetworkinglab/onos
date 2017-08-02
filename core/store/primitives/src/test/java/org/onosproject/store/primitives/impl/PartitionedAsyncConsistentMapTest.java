/*
 * Copyright 2015-present Open Networking Foundation
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

import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.AsyncConsistentMapAdapter;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Versioned;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;


public class PartitionedAsyncConsistentMapTest {

    PartitionedAsyncConsistentMap<String, String> partitionedAsyncConsistentMap;

    private AsyncConsistentMapMock asyncMap1;
    private AsyncConsistentMapMock asyncMap2;
    private Map<PartitionId, AsyncConsistentMap<String, String>> partitions;
    private List<PartitionId> sortedMemberPartitionIds;


    private Map<String, String> baseMap;
    private String partitionName = "PartitionManager";
    private PartitionId pid1;
    private PartitionId pid2;
    private Hasher<String> hasher;
    private Serializer serializer;


    private final List<String> allKeys = Lists.newArrayList(KEY1, KEY2);

    private static final String KEY1 = "AAA";
    private static final String VALUE1 = "one";
    private static final String KEY2 = "BBB";
    private static final String VALUE2 = "two";
    private static final String TEST3 = "CCC";
    private static final String VALUE3 = "three";
    private static final String TEST4  = "DDD";
    private static final String VALUE4 = "four";



    public class AsyncConsistentMapMock<K, V> extends AsyncConsistentMapAdapter<K, V> {
        private final List<MapEventListener<K, V>> listeners;
        Collection<Consumer<Status>> statusChangeListeners = new ArrayList<>();
        private final Map<K, V> baseMap;

        Versioned<V> makeVersioned(V v) {
            return new Versioned<>(v, 0, 0);
        }

        AsyncConsistentMapMock(Map<K, V> newBaseMap) {
            baseMap = newBaseMap;
            listeners = new ArrayList<>();
        }

        public CompletableFuture<Integer> size() {
            return CompletableFuture.completedFuture(baseMap.size());
        }

        @Override
        public CompletableFuture<Boolean> containsKey(K key) {
            return CompletableFuture.completedFuture(baseMap.containsKey(key));
        }

        @Override
        public CompletableFuture<Versioned<V>> getOrDefault(K key, V value) {
            return CompletableFuture.completedFuture(makeVersioned(baseMap.getOrDefault(key, value)));
        }
        @Override
        public CompletableFuture<Boolean> containsValue(V value) {
            return CompletableFuture.completedFuture(baseMap.containsValue(value));
        }

        @Override
        public CompletableFuture<Versioned<V>> get(K key) {
            return CompletableFuture.completedFuture(makeVersioned(baseMap.get(key)));
        }

        @Override
        public CompletableFuture<Versioned<V>>
        computeIf(K key, Predicate<? super V> condition,
                  BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return null;
        }

        @Override
        public CompletableFuture<Versioned<V>> put(K key, V value) {
            return CompletableFuture.completedFuture(makeVersioned(baseMap.put(key, value)));
        }

        @Override
        public CompletableFuture<Versioned<V>> putAndGet(K key, V value) {
            return CompletableFuture.completedFuture(makeVersioned(baseMap.put(key, value)));
        }

        @Override
        public CompletableFuture<Versioned<V>> remove(K key) {
            return CompletableFuture.completedFuture(makeVersioned(baseMap.remove(key)));
        }

        @Override
        public CompletableFuture<Void> clear() {
            baseMap.clear();
            return CompletableFuture.allOf();
        }

        @Override
        public CompletableFuture<Set<K>> keySet() {
            return CompletableFuture.completedFuture(baseMap.keySet());
        }

        @Override
        public CompletableFuture<Collection<Versioned<V>>> values() {
            Set<Versioned<V>> valuesAsVersionedCollection =
                    baseMap.values().stream().map(this::makeVersioned)
                            .collect(Collectors.toSet());
            return CompletableFuture.completedFuture(valuesAsVersionedCollection);
        }

        @Override
        public CompletableFuture<Set<Map.Entry<K, Versioned<V>>>> entrySet() {
            Map<K, Versioned<V>> valuesAsVersionedMap = new HashMap<>();
            baseMap.entrySet()
                    .forEach(e -> valuesAsVersionedMap.put(e.getKey(),
                            makeVersioned(e.getValue())));
            return CompletableFuture.completedFuture(valuesAsVersionedMap.entrySet());
        }

        @Override
        public CompletableFuture<Versioned<V>> putIfAbsent(K key, V value) {
            return CompletableFuture.completedFuture(makeVersioned(baseMap.putIfAbsent(key, value)));
        }

        @Override
        public CompletableFuture<Boolean> remove(K key, V value) {
            return CompletableFuture.completedFuture(baseMap.remove(key, value));
        }

        @Override
        public CompletableFuture<Boolean> remove(K key, long version) {
            Object value = baseMap.remove(key);
            return CompletableFuture.completedFuture(value != null);
        }

        @Override
        public CompletableFuture<Versioned<V>> replace(K key, V value) {
            return CompletableFuture.completedFuture(makeVersioned(baseMap.replace(key, value)));
        }

        @Override
        public CompletableFuture<Boolean> replace(K key, V oldValue, V newValue) {
            return CompletableFuture.completedFuture(baseMap.replace(key, oldValue, newValue));
        }

        @Override
        public CompletableFuture<Boolean> replace(K key, long oldVersion, V newValue) {
            return CompletableFuture.completedFuture(baseMap.replace(key, newValue) != null);
        }

        @Override
        public CompletableFuture<Void> addListener(MapEventListener<K, V> listener, Executor e) {
            listeners.add(listener);
            return CompletableFuture.allOf();
        }

        @Override
        public CompletableFuture<Void> removeListener(MapEventListener<K, V> listener) {
            listeners.remove(listener);
            return CompletableFuture.allOf();
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
            return statusChangeListeners;
        }
    }

    @Before
    public void setUp() throws Exception {
        baseMap = new HashMap<>();
        asyncMap1 = new AsyncConsistentMapMock<>(baseMap);
        asyncMap2 = new AsyncConsistentMapMock<>(baseMap);

        pid1 = PartitionId.from(1);
        pid2 = PartitionId.from(2);
        partitions = new HashMap<>();
        serializer = Serializer.using(KryoNamespaces.BASIC);

        asyncMap1.put(KEY1, VALUE1);
        asyncMap2.put(KEY2, VALUE2);
        partitions.put(pid1, asyncMap1);
        partitions.put(pid2, asyncMap2);

        sortedMemberPartitionIds = Lists.newArrayList(partitions.keySet());

        hasher = key -> {
            int hashCode = Hashing.sha256().hashBytes(serializer.encode(key)).asInt();
            return sortedMemberPartitionIds.get(Math.abs(hashCode) % partitions.size());
        };

        partitionedAsyncConsistentMap = new PartitionedAsyncConsistentMap(partitionName,
                partitions, hasher);

    }

    @Test
    public void tester() {
        assertThat(partitionedAsyncConsistentMap.isEmpty().join(), is(false));
        assertThat(partitionedAsyncConsistentMap.name(), is("PartitionManager"));
        asyncMap1.put(TEST3, VALUE3);
        partitions.put(pid1, asyncMap1);
        assertThat(partitionedAsyncConsistentMap.size().join(), is(6));

        assertThat(partitionedAsyncConsistentMap.entrySet().join().size(), is(3));

        asyncMap2.put(TEST4, VALUE4);
        partitions.put(pid2, asyncMap2);
        assertThat(partitionedAsyncConsistentMap.size().join(), is(8));


        assertThat(partitionedAsyncConsistentMap.containsValue(VALUE1).join(), is(true));
        assertThat(partitionedAsyncConsistentMap.containsValue("newValue").join(), is(false));
        assertThat(partitionedAsyncConsistentMap.containsKey(KEY2).join(), is(true));
        assertThat(partitionedAsyncConsistentMap.containsKey("newKey").join(), is(false));

        partitionedAsyncConsistentMap.putAndGet(KEY1, "newOne").join();
        assertThat(partitionedAsyncConsistentMap.containsValue("newOne").join(), is(true));
        partitionedAsyncConsistentMap.remove(KEY1).join();
        assertThat(partitionedAsyncConsistentMap.containsKey(KEY1).join(), is(false));
        partitionedAsyncConsistentMap.putIfAbsent(KEY1, "same").join();
        partitionedAsyncConsistentMap.replace(KEY1, "same", "one").join();
        assertThat(partitionedAsyncConsistentMap.containsValue("one").join(), is(true));
        partitionedAsyncConsistentMap.putIfAbsent("EEE", "five");

        assertThat(partitionedAsyncConsistentMap.get(KEY2).join().value(), is(VALUE2));
        assertThat(partitionedAsyncConsistentMap.getOrDefault(KEY1, "nil").join().value(),
                is(VALUE1));

        assertThat(partitionedAsyncConsistentMap.getOrDefault("newKey", "testDefault").join().value(),
                is("testDefault"));

        assertNotNull(partitionedAsyncConsistentMap.keySet().join());
        assertThat(partitionedAsyncConsistentMap.keySet().join().size(), is(5));
        assertThat(partitionedAsyncConsistentMap.keySet().join(), hasItem("CCC"));


        partitionedAsyncConsistentMap.clear().join();
        assertThat(partitionedAsyncConsistentMap.size().join(), is(0));
        assertThat(partitionedAsyncConsistentMap.isEmpty().join(), is(true));

    }
}
