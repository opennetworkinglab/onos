/*
 * Copyright 2017-present Open Networking Foundation
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

import org.junit.Test;
import org.onosproject.store.service.AsyncConsistentMapAdapter;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Versioned;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Tests for DefaultConsistentMap.
 */
public class DefaultConsistentMapTest {

    private static final int DEFAULT_CREATION_TIME = 0;
    private static final int DEFAULT_VERSION = 0;


    public class AsyncConsistentMapMock<K, V> extends AsyncConsistentMapAdapter<K, V> {
        private final List<MapEventListener<K, V>> listeners;
        Collection<Consumer<Status>> statusChangeListeners = new ArrayList<>();
        private final Map<K, V> baseMap;

        Versioned<V> makeVersioned(V v) {
            return new Versioned<>(v, DEFAULT_VERSION, DEFAULT_CREATION_TIME);
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

            V value = baseMap.get(key);

            if (condition.test(value)) {
                value = baseMap.compute(key, remappingFunction);
            }
            return CompletableFuture.completedFuture(makeVersioned(value));
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

    private static final String KEY1 = "AAA";
    private static final String VALUE1 = "111";
    private static final String KEY2 = "BBB";
    private static final String VALUE2 = "222";
    private static final String KEY3 = "CCC";
    private static final String VALUE3 = "333";
    private static final String KEY4 = "DDD";
    private static final String VALUE4 = "444";

    private int computeFunctionCalls = 0;
    private String computeFunction(String s) {
        computeFunctionCalls++;
        if (KEY4.equals(s)) {
            return VALUE4;
        }
        return "";
    }

    class Listener implements MapEventListener<String, String> {
        final int id;

        Listener(int newId) {
            id = newId;
        }

        @Override
        public void event(MapEvent<String, String> event) {
            // Nothing to do here
        }
    }

    /**
     * Tests the behavior of public APIs of the default consistent map
     * implmentation.
     */
    @Test
    public void testBehavior() {

        Map<String, String> baseMap = new HashMap<>();
        AsyncConsistentMapMock<String, String> asyncMap =
                new AsyncConsistentMapMock<>(baseMap);
        ConsistentMap<String, String> newMap =
                new DefaultConsistentMap<>(asyncMap, 11);
        assertThat(newMap.size(), is(0));
        assertThat(newMap.isEmpty(), is(true));

        newMap.put(KEY1, VALUE1);
        assertThat(newMap.size(), is(1));
        assertThat(newMap.get(KEY1).value(), is(VALUE1));
        assertThat(newMap.containsKey(KEY1), is(true));
        assertThat(newMap.containsKey(VALUE1), is(false));
        assertThat(newMap.containsValue(VALUE1), is(true));
        assertThat(newMap.containsValue(KEY1), is(false));
        assertThat(newMap.keySet(), hasSize(1));
        assertThat(newMap.keySet(), hasItem(KEY1));
        assertThat(newMap.values(), hasSize(1));
        assertThat(newMap.values(), hasItem(new Versioned<>(VALUE1, 0, 0)));
        assertThat(newMap.entrySet(), hasSize(1));
        Map.Entry<String, Versioned<String>> entry = newMap.entrySet().iterator().next();
        assertThat(entry.getKey(), is(KEY1));
        assertThat(entry.getValue().value(), is(VALUE1));

        newMap.putIfAbsent(KEY2, VALUE2);
        assertThat(newMap.entrySet(), hasSize(2));
        assertThat(newMap.get(KEY2).value(), is(VALUE2));
        newMap.putIfAbsent(KEY2, VALUE1);
        assertThat(newMap.entrySet(), hasSize(2));
        assertThat(newMap.get(KEY2).value(), is(VALUE2));

        newMap.putAndGet(KEY3, VALUE3);
        assertThat(newMap.entrySet(), hasSize(3));
        assertThat(newMap.get(KEY3).value(), is(VALUE3));

        newMap.putIfAbsent(KEY3, VALUE1);
        assertThat(newMap.entrySet(), hasSize(3));
        assertThat(newMap.get(KEY3).value(), is(VALUE3));

        assertThat(newMap.computeIfAbsent(KEY4, this::computeFunction).value(), is(VALUE4));
        assertThat(computeFunctionCalls, is(1));
        assertThat(newMap.entrySet(), hasSize(4));
        assertThat(newMap.computeIfAbsent(KEY4, this::computeFunction).value(), is(VALUE4));
        assertThat(computeFunctionCalls, is(1));

        Map javaMap = newMap.asJavaMap();
        assertThat(javaMap.size(), is(newMap.size()));
        assertThat(javaMap.get(KEY1), is(VALUE1));

        assertThat(newMap.toString(), containsString(KEY4 + "=" + VALUE4));

        assertThat(newMap.remove(KEY4).value(), is(VALUE4));
        assertThat(newMap.entrySet(), hasSize(3));
        assertThat(newMap.remove(KEY4).value(), nullValue());
        assertThat(newMap.entrySet(), hasSize(3));

        assertThat(newMap.remove(KEY3, DEFAULT_VERSION), is(true));
        assertThat(newMap.entrySet(), hasSize(2));
        assertThat(newMap.remove(KEY3, DEFAULT_VERSION), is(false));
        assertThat(newMap.entrySet(), hasSize(2));

        assertThat(newMap.remove(KEY2, VALUE2), is(true));
        assertThat(newMap.entrySet(), hasSize(1));
        assertThat(newMap.remove(KEY2, VALUE2), is(false));
        assertThat(newMap.entrySet(), hasSize(1));

        assertThat(newMap.replace(KEY1, VALUE4).value(), is(VALUE1));
        assertThat(newMap.get(KEY1).value(), is(VALUE4));

        assertThat(newMap.replace(KEY1, VALUE4, VALUE2), is(true));
        assertThat(newMap.get(KEY1).value(), is(VALUE2));

        assertThat(newMap.replace(KEY1, DEFAULT_VERSION, VALUE1), is(true));
        assertThat(newMap.get(KEY1).value(), is(VALUE1));

        newMap.clear();
        assertThat(newMap.size(), is(0));

        newMap.compute(KEY1, (a, b) -> VALUE1);
        assertThat(newMap.get(KEY1).value(), is(VALUE1));
        newMap.computeIfPresent(KEY1, (a, b) -> VALUE2);
        assertThat(newMap.get(KEY1).value(), is(VALUE2));

        Listener listener1 = new Listener(1);
        newMap.addListener(listener1, null);
        assertThat(asyncMap.listeners, hasSize(1));
        assertThat(asyncMap.listeners, hasItem(listener1));

        newMap.removeListener(listener1);
        assertThat(asyncMap.listeners, hasSize(0));

        Consumer<DistributedPrimitive.Status> consumer = status -> { };

        newMap.addStatusChangeListener(consumer);
        assertThat(newMap.statusChangeListeners(), hasSize(1));
        assertThat(newMap.statusChangeListeners(), hasItem(consumer));

        newMap.removeStatusChangeListener(consumer);
        assertThat(newMap.statusChangeListeners(), hasSize(0));
        assertThat(newMap.statusChangeListeners(), not(hasItem(consumer)));
    }

}
