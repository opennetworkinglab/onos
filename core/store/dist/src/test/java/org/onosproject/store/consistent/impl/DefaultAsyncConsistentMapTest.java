/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.consistent.impl;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSet;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Transaction;
import org.onosproject.store.service.Versioned;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import net.kuujo.copycat.Task;
import net.kuujo.copycat.cluster.Cluster;
import net.kuujo.copycat.resource.ResourceState;

/**
 *
 */
public class DefaultAsyncConsistentMapTest {

    private static final ApplicationId APP_ID = new DefaultApplicationId(42, "what");

    private static final TestData KEY1A = new TestData("One", "a");
    private static final TestData KEY1B = new TestData("One", "b");

    private static final TestData VALUE2A = new TestData("Two", "a");
    private static final TestData VALUE2B = new TestData("Two", "b");

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testKeySet() throws Exception {
        DefaultAsyncConsistentMap<TestData, TestData> map;
        String name = "map_name";
        Database database = new TestDatabase();
        Serializer serializer = Serializer.forTypes(TestData.class);

        map = new DefaultAsyncConsistentMap<>(name, APP_ID, database, serializer,
                                            false, false, false);
        map.put(KEY1A, VALUE2A);
        map.put(KEY1B, VALUE2A);

        Set<TestData> set = map.keySet().get();
        assertEquals("Should contain 2 keys",
                2, set.size());
        assertThat(set.contains(KEY1A), is(true));
        assertThat(set.contains(KEY1B), is(true));
        assertThat(set.contains(new TestData("One", "a")), is(true));
    }

    @Test
    public void testEntrySet() throws Exception {
        DefaultAsyncConsistentMap<TestData, TestData> map;
        String name = "map_name";
        Database database = new TestDatabase();
        Serializer serializer = Serializer.forTypes(TestData.class);

        map = new DefaultAsyncConsistentMap<>(name, APP_ID, database, serializer,
                                            false, false, false);
        map.put(KEY1A, VALUE2A);
        map.put(KEY1B, VALUE2A);

        assertEquals("Should contain 2 entry",
                     2,
                     map.entrySet().get().size());
    }

    /**
     * Object to be used as a test data.
     *
     * {@link Object#equals(Object)} use only part of it's fields.
     *
     * As a result there can be 2 instances which the
     * serialized bytes are not-equal but
     * {@link Object#equals(Object)}-wise they are equal.
     */
    public static class TestData {

        private final String theKey;

        @SuppressWarnings("unused")
        private final String notUsedForEquals;

        public TestData(String theKey, String notUsedForEquals) {
            this.theKey = theKey;
            this.notUsedForEquals = notUsedForEquals;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(theKey);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TestData) {
                TestData that = (TestData) obj;
                return Objects.equals(this.theKey, that.theKey);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("theKey", theKey)
                    .add("notUsedForEquals", notUsedForEquals)
                    .toString();
        }
    }

    /**
     * {@link Database} implementation for testing.
     *
     * There is only 1 backing Map, {@code mapName} will be ignored.
     */
    public class TestDatabase implements Database {

        Map<String, Versioned<byte[]>> map = new ConcurrentHashMap<>();

        @Override
        public CompletableFuture<Set<String>> maps() {
            return CompletableFuture.completedFuture(ImmutableSet.of());
        }

        @Override
        public CompletableFuture<Map<String, Long>> counters() {
            return CompletableFuture.completedFuture(ImmutableMap.of());
        }

        @Override
        public CompletableFuture<Integer> mapSize(String mapName) {
            return CompletableFuture.completedFuture(map.size());
        }

        @Override
        public CompletableFuture<Boolean> mapIsEmpty(String mapName) {
            return CompletableFuture.completedFuture(map.isEmpty());
        }

        @Override
        public CompletableFuture<Boolean> mapContainsKey(String mapName,
                                                         String key) {
            return CompletableFuture.completedFuture(map.containsKey(key));
        }

        @Override
        public CompletableFuture<Boolean> mapContainsValue(String mapName,
                                                           byte[] value) {
            return CompletableFuture.completedFuture(Maps.transformValues(map, Versioned::value)
                                                     .containsValue(value));
        }

        @Override
        public CompletableFuture<Versioned<byte[]>> mapGet(String mapName,
                                                           String key) {
            return CompletableFuture.completedFuture(map.get(key));
        }

        @Override
        public synchronized CompletableFuture<Result<UpdateResult<String, byte[]>>> mapUpdate(String mapName,
                                                                                 String key,
                                                                                 Match<byte[]> valueMatch,
                                                                                 Match<Long> versionMatch,
                                                                                 byte[] value) {

            boolean updated = false;
            final Versioned<byte[]> oldValue;
            final Versioned<byte[]> newValue;

            Versioned<byte[]> old = map.getOrDefault(key, new Versioned<byte[]>(null, 0));
            if (valueMatch.matches(old.value()) && versionMatch.matches(old.version())) {
                updated = true;
                oldValue = old;
                newValue = new Versioned<>(value, old.version() + 1);
                map.put(key, newValue);
            } else {
                updated = false;
                oldValue = old;
                newValue = old;
            }
            return CompletableFuture.completedFuture(
                             Result.ok(new UpdateResult<String, byte[]>(updated,
                                            mapName, key, oldValue, newValue)));
        }

        @Override
        public CompletableFuture<Result<Void>> mapClear(String mapName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Set<String>> mapKeySet(String mapName) {
            return CompletableFuture.completedFuture(unmodifiableSet(map.keySet()));
        }

        @Override
        public CompletableFuture<Collection<Versioned<byte[]>>> mapValues(String mapName) {
            return CompletableFuture.completedFuture(unmodifiableCollection(map.values()));
        }

        @Override
        public CompletableFuture<Set<Entry<String, Versioned<byte[]>>>> mapEntrySet(String mapName) {
            return CompletableFuture.completedFuture(unmodifiableSet(map.entrySet()));
        }

        @Override
        public CompletableFuture<Long> counterAddAndGet(String counterName,
                                                        long delta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Long> counterGetAndAdd(String counterName,
                                                        long delta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Void> counterSet(String counterName,
                                                  long value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Boolean> counterCompareAndSet(String counterName,
                                                               long expectedValue,
                                                               long update) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Long> counterGet(String counterName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Long> queueSize(String queueName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Void> queuePush(String queueName,
                                                 byte[] entry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<byte[]> queuePop(String queueName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<byte[]> queuePeek(String queueName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<CommitResponse> prepareAndCommit(Transaction transaction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Boolean> prepare(Transaction transaction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<CommitResponse> commit(Transaction transaction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Boolean> rollback(Transaction transaction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String name() {
            return "name";
        }

        @Override
        public ResourceState state() {
            return ResourceState.HEALTHY;
        }

        @Override
        public Cluster cluster() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Database addStartupTask(Task<CompletableFuture<Void>> task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Database addShutdownTask(Task<CompletableFuture<Void>> task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Database> open() {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public CompletableFuture<Void> close() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void registerConsumer(Consumer<StateMachineUpdate> consumer) {
        }

        @Override
        public void unregisterConsumer(Consumer<StateMachineUpdate> consumer) {
        }
    }

}
