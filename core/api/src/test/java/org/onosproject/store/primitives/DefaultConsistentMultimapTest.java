/*
 * Copyright 2019-present Open Networking Foundation
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.junit.Test;
import org.onosproject.store.service.AsyncConsistentMultimapAdapter;
import org.onosproject.store.service.ConsistentMultimap;
import org.onosproject.store.service.Versioned;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

/**
 * Tests for DefaultConsistentMultiMap.
 */
public class DefaultConsistentMultimapTest {

    private static final String KEY1 = "AAA";
    private static final String VALUE1 = "111";
    private static final String KEY2 = "BBB";
    private static final String VALUE2 = "222";
    private static final String KEY3 = "CCC";
    private static final String VALUE3 = "333";
    private static final String KEY4 = "DDD";
    private static final String VALUE4 = "444";
    private final List<String> allKeys = Lists.newArrayList(KEY1, KEY2,
                                                            KEY3, KEY4);
    private final List<String> allValues = Lists.newArrayList(VALUE1, VALUE2,
                                                              VALUE3, VALUE4);

    /**
     * Tests the behavior of public APIs of the default consistent multi-map
     * implementation.
     */
    @Test
    public void testBehavior() {
        // Initialize the map
        Multimap<String, String> baseMap = HashMultimap.create();
        AsyncConsistentMultimapMock<String, String> asyncMultiMap = new AsyncConsistentMultimapMock<>(baseMap);
        ConsistentMultimap<String, String> newMap = new DefaultConsistentMultimap<>(asyncMultiMap, 69);

        // Verify is empty
        assertThat(newMap.size(), is(0));
        assertThat(newMap.isEmpty(), is(true));

        // Test multi put
        Map<String, Collection<? extends String>> mapping = Maps.newHashMap();
        // First build the mappings having each key a different mapping
        allKeys.forEach(key -> {
            switch (key) {
                case KEY1:
                    mapping.put(key, Lists.newArrayList(allValues.subList(0, 1)));
                    break;
                case KEY2:
                    mapping.put(key, Lists.newArrayList(allValues.subList(0, 2)));
                    break;
                case KEY3:
                    mapping.put(key, Lists.newArrayList(allValues.subList(0, 3)));
                    break;
                default:
                    mapping.put(key, Lists.newArrayList(allValues.subList(0, 4)));
                    break;
            }
        });
        // Success
        assertThat(newMap.putAll(mapping), is(true));
        // Failure
        assertThat(newMap.putAll(mapping), is(false));
        // Verify operation
        assertThat(newMap.size(), is(10));
        assertThat(newMap.isEmpty(), is(false));
        // verify mapping is ok

        allKeys.forEach(key -> {
            List<String> actual = Lists.newArrayList(Versioned.valueOrNull(newMap.get(key)));
            switch (key) {
                case KEY1:
                    assertThat(actual, containsInAnyOrder(allValues.subList(0, 1).toArray()));
                    break;
                case KEY2:
                    assertThat(actual, containsInAnyOrder(allValues.subList(0, 2).toArray()));
                    break;
                case KEY3:
                    assertThat(actual, containsInAnyOrder(allValues.subList(0, 3).toArray()));
                    break;
                default:
                    assertThat(actual, containsInAnyOrder(allValues.subList(0, 4).toArray()));
                    break;
            }
        });
        // Success
        assertThat(newMap.removeAll(mapping), is(true));
        // Failure
        assertThat(newMap.removeAll(mapping), is(false));
        // Verify operation
        assertThat(newMap.size(), is(0));
        assertThat(newMap.isEmpty(), is(true));
    }

    public static class AsyncConsistentMultimapMock<K, V> extends AsyncConsistentMultimapAdapter<K, V> {
        private final Multimap<K, V> baseMap;
        private static final int DEFAULT_CREATION_TIME = 0;
        private static final int DEFAULT_VERSION = 0;

        AsyncConsistentMultimapMock(Multimap<K, V> newBaseMap) {
            baseMap = newBaseMap;
        }

        Versioned<Collection<? extends V>> makeVersioned(Collection<? extends V> v) {
            return new Versioned<>(v, DEFAULT_VERSION, DEFAULT_CREATION_TIME);
        }

        @Override
        public CompletableFuture<Integer> size() {
            return CompletableFuture.completedFuture(baseMap.size());
        }

        @Override
        public CompletableFuture<Boolean> isEmpty() {
            return CompletableFuture.completedFuture(baseMap.isEmpty());
        }

        @Override
        public CompletableFuture<Boolean> putAll(Map<K, Collection<? extends V>> mapping) {
            CompletableFuture<Boolean> result = CompletableFuture.completedFuture(false);
            for (Map.Entry<K, Collection<? extends V>> entry : mapping.entrySet()) {
                if (baseMap.putAll(entry.getKey(), entry.getValue())) {
                    result = CompletableFuture.completedFuture(true);
                }
            }
            return result;
        }

        @Override
        public CompletableFuture<Versioned<Collection<? extends V>>> get(K key) {
            return CompletableFuture.completedFuture(makeVersioned(baseMap.get(key)));
        }

        @Override
        public CompletableFuture<Boolean> removeAll(Map<K, Collection<? extends V>> mapping) {
            CompletableFuture<Boolean> result = CompletableFuture.completedFuture(false);
            for (Map.Entry<K, Collection<? extends V>> entry : mapping.entrySet()) {
                for (V value : entry.getValue()) {
                    if (baseMap.remove(entry.getKey(), value)) {
                        result = CompletableFuture.completedFuture(true);
                    }
                }
            }
            return result;
        }
    }
}
