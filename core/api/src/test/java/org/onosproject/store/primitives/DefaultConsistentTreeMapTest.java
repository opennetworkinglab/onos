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

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Versioned;

import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.store.primitives.TestingCompletableFutures.ErrorState.NONE;

public class DefaultConsistentTreeMapTest {

    private static final String MIN_KEY = "0";
    private static final String KEY1 = "A";
    private static final String VALUE1 = "A";
    private static final String KEY2 = "B";
    private static final String VALUE2 = "B";
    private static final String KEY3 = "C";
    private static final String VALUE3 = "C";
    private static final String KEY4 = "D";
    private static final String VALUE4 = "D";
    private static final String KEY5 = "E";
    private static final String VALUE5 = "E";
    private static final String MAX_KEY = "Z";
    private static final String NO_SUCH_VALUE = "BAD VALUE";
    private static final String NO_SUCH_KEY = "BAD KEY";
    private static final String DEFAULT_VALUE = "DEFAULT";

    private DefaultConsistentTreeMap<String> treeMap;
    private TestAsyncConsistentTreeMap<String> asyncMap;

    private static class TestAsyncConsistentTreeMap<V> extends AsyncConsistentTreeMapAdapter<V> {
        LinkedList<MapEventListener<String, V>> listeners = new LinkedList<>();

        @Override
        public CompletableFuture<Void> addListener(MapEventListener<String, V> listener,
                                                   Executor executor) {
            listeners.add(listener);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> removeListener(MapEventListener<String, V> listener) {
            listeners.remove(listener);
            return CompletableFuture.completedFuture(null);
        }
    }

    class Listener implements MapEventListener<String, String> {
        @Override
        public void event(MapEvent<String, String> event) {
            // Nothing to do here
        }
    }

    private DefaultConsistentTreeMap<String> createMap() {
        asyncMap = new TestAsyncConsistentTreeMap<>();
        DefaultConsistentTreeMap<String> map = new DefaultConsistentTreeMap<>(asyncMap, 1000L);
        assertThat(map, notNullValue());
        assertThat(map.isEmpty(), is(true));
        map.putIfAbsent(KEY1, VALUE1);
        map.putIfAbsent(KEY2, VALUE2);
        map.putIfAbsent(KEY3, VALUE3);
        return map;
    }

    static class VersionedMatcher extends TypeSafeMatcher<Versioned<String>> {
        String expectedValue;

        VersionedMatcher(String expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public boolean matchesSafely(Versioned<String> value) {
            return expectedValue.equals(value.value());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("<Versioned{value=" + expectedValue + ",...");
        }
    }

    private static VersionedMatcher matchesVersioned(String expectedValue) {
        return new VersionedMatcher(expectedValue);
    }

    @Before
    public void setUpMap() {
        treeMap = createMap();
    }

    @Test
    public void testKeys() {
        assertThat(treeMap.size(), is(3));
        assertThat(treeMap.navigableKeySet(), hasSize(3));

        assertThat(treeMap.firstKey(), is(VALUE1));
        assertThat(treeMap.lastKey(), is(VALUE3));
        assertThat(treeMap.lowerKey(KEY2), is(VALUE1));
        assertThat(treeMap.higherKey(KEY2), is(VALUE3));
        assertThat(treeMap.floorKey(MAX_KEY), is(VALUE3));
        assertThat(treeMap.ceilingKey(MIN_KEY), is(VALUE1));

        assertThat(treeMap.containsKey(KEY2), is(true));
        assertThat(treeMap.containsKey(MAX_KEY), is(false));
    }

    private void checkEntry(Map.Entry<String, Versioned<String>> entry,
                            String expectedKey,
                            String expectedValue) {
        assertThat(entry.getKey(), is(expectedKey));
        assertThat(entry.getValue(), matchesVersioned(expectedValue));
    }

    @Test
    public void testEntries() {
        assertThat(treeMap.size(), is(3));

        checkEntry(treeMap.firstEntry(), KEY1, VALUE1);
        checkEntry(treeMap.lastEntry(), KEY3, VALUE3);
        checkEntry(treeMap.lowerEntry(KEY2), KEY1, VALUE1);
        checkEntry(treeMap.higherEntry(KEY2), KEY3, VALUE3);
        checkEntry(treeMap.floorEntry(MAX_KEY), KEY3, VALUE3);
        checkEntry(treeMap.ceilingEntry(MIN_KEY), KEY1, VALUE1);

        checkEntry(treeMap.pollFirstEntry(), KEY1, VALUE1);
        assertThat(treeMap.size(), is(2));

        checkEntry(treeMap.pollLastEntry(), KEY3, VALUE3);
        assertThat(treeMap.size(), is(1));
    }

    @Test
    public void testGets() {
        assertThat(treeMap.get(KEY2), matchesVersioned(VALUE2));
        assertThat(treeMap.containsValue(VALUE3), is(true));
        assertThat(treeMap.getOrDefault(KEY3, DEFAULT_VALUE), matchesVersioned(VALUE3));
        assertThat(treeMap.getOrDefault(NO_SUCH_KEY, DEFAULT_VALUE), matchesVersioned(DEFAULT_VALUE));
        assertThat(treeMap.compute(KEY4, (k, v) -> v == null ? VALUE4 : ""), matchesVersioned(VALUE4));
        assertThat(treeMap.computeIf(KEY4, Objects::isNull, (k, v) -> NO_SUCH_VALUE), matchesVersioned(VALUE4));
        assertThat(treeMap.computeIfPresent(KEY4, (k, v) -> NO_SUCH_VALUE), matchesVersioned(NO_SUCH_VALUE));
        assertThat(treeMap.computeIfAbsent(KEY2, (v) -> NO_SUCH_VALUE), matchesVersioned(VALUE2));
        treeMap.put(KEY5, VALUE5);
        assertThat(treeMap.putAndGet(KEY5, VALUE1), matchesVersioned(VALUE5));
        assertThat(treeMap.get(KEY5), matchesVersioned(VALUE1));
    }

    @Test
    public void testSets() {
        Set<String> keys = treeMap.keySet();
        assertThat(keys, hasSize(3));
        assertThat(keys, hasItems(KEY1, KEY2, KEY3));

        Set<String> values = treeMap.values().stream().map(Versioned::value).collect(Collectors.toSet());
        assertThat(values, hasSize(3));
        assertThat(values, hasItems(VALUE1, VALUE2, VALUE3));

        Set<String> valuesFromEntries = treeMap.entrySet().stream()
                .map(entry -> entry.getValue().value()).collect(Collectors.toSet());
        assertThat(valuesFromEntries, hasSize(3));
        assertThat(valuesFromEntries, hasItems(VALUE1, VALUE2, VALUE3));

        Set<String> keysFromEntries = treeMap.entrySet().stream()
                .map(Map.Entry::getKey).collect(Collectors.toSet());
        assertThat(keysFromEntries, hasSize(3));
        assertThat(keysFromEntries, hasItems(KEY1, KEY2, KEY3));
    }

    @Test
    public void testRemoves() {
        treeMap.remove(KEY1);
        assertThat(treeMap.size(), is(2));

        treeMap.remove(KEY2, 1L);
        assertThat(treeMap.size(), is(1));

        treeMap.remove(KEY3, VALUE3);
        assertThat(treeMap.size(), is(0));
    }

    @Test
    public void testClear() {
        treeMap.clear();
        assertThat(treeMap.size(), is(0));
    }

    @Test
    public void testReplaces() {
        treeMap.replace(KEY1, VALUE2);
        assertThat(treeMap.get(KEY1), matchesVersioned(VALUE2));

        treeMap.replace(KEY2, 1L, VALUE1);
        assertThat(treeMap.get(KEY2), matchesVersioned(VALUE1));

        treeMap.replace(KEY3, VALUE3, VALUE5);
        assertThat(treeMap.get(KEY3), matchesVersioned(VALUE5));
    }

    @Test
    public void testJavaMap() {
        Map<String, String> javaMap = treeMap.asJavaMap();
        assertThat(javaMap.entrySet(), hasSize(3));
        assertThat(javaMap.values(), hasItems(VALUE1, VALUE2, VALUE3));
        assertThat(javaMap.keySet(), hasItems(KEY1, KEY2, KEY3));
    }

    @Test
    public void testSubMap() {
        treeMap.putIfAbsent(KEY4, VALUE4);
        treeMap.putIfAbsent(KEY5, VALUE5);

        Map<String, String> subMap = treeMap.subMap(KEY2, KEY4, true, true);
        assertThat(subMap.entrySet(), hasSize(3));
        assertThat(subMap.values(), hasItems(VALUE2, VALUE3, VALUE4));
        assertThat(subMap.keySet(), hasItems(KEY2, KEY3, KEY4));
    }

    @Test
    public void testListeners() {
        Listener listener1 = new Listener();
        Listener listener2 = new Listener();

        assertThat(asyncMap.listeners, hasSize(0));
        treeMap.addListener(listener1);
        assertThat(asyncMap.listeners, hasSize(1));
        treeMap.addListener(listener2);
        assertThat(asyncMap.listeners, hasSize(2));

        treeMap.removeListener(listener1);
        treeMap.removeListener(listener2);
        assertThat(asyncMap.listeners, hasSize(0));
    }

    class ConsistentTreeMapWithError<K> extends AsyncConsistentTreeMapAdapter<K> {

        TestingCompletableFutures.ErrorState errorState = NONE;

        void setErrorState(TestingCompletableFutures.ErrorState errorState) {
            this.errorState = errorState;
        }

        ConsistentTreeMapWithError() {
            super();
        }

        @Override
        public CompletableFuture<String> lowerKey(String key) {
            return TestingCompletableFutures.createStringFuture(errorState);
        }
    }

    @Test(expected = ConsistentMapException.Timeout.class)
    public void testTimeout() {
        ConsistentTreeMapWithError<String> consistentMap =
                new ConsistentTreeMapWithError<>();
        consistentMap.setErrorState(TestingCompletableFutures.ErrorState.TIMEOUT_EXCEPTION);
        DefaultConsistentTreeMap<String> map =
                new DefaultConsistentTreeMap<>(consistentMap, 1000);

        map.lowerKey(KEY1);
    }


    @Test(expected = ConsistentMapException.Interrupted.class)
    public void testInterrupted() {
        ConsistentTreeMapWithError<String> consistentMap =
                new ConsistentTreeMapWithError<>();
        consistentMap.setErrorState(TestingCompletableFutures.ErrorState.INTERRUPTED_EXCEPTION);
        DefaultConsistentTreeMap<String> map =
                new DefaultConsistentTreeMap<>(consistentMap, 1000);

        map.lowerKey(KEY1);
    }

    @Test(expected = ConsistentMapException.class)
    public void testExecutionError() {
        ConsistentTreeMapWithError<String> consistentMap =
                new ConsistentTreeMapWithError<>();
        consistentMap.setErrorState(TestingCompletableFutures.ErrorState.EXECUTION_EXCEPTION);
        DefaultConsistentTreeMap<String> map =
                new DefaultConsistentTreeMap<>(consistentMap, 1000);

        map.lowerKey(KEY1);
    }

}