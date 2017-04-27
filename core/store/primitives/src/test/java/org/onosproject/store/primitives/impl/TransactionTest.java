/*
 * Copyright 2017-present Open Networking Laboratory
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.hash.Hashing;
import org.junit.Test;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.CommitStatus;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Version;
import org.onosproject.store.service.Versioned;

import static junit.framework.TestCase.assertNull;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Transaction test.
 */
public class TransactionTest {

    @Test
    public void testTransaction() throws Exception {
        AsyncConsistentMap<String, String> asyncMap = strictMock(AsyncConsistentMap.class);
        TransactionId transactionId = TransactionId.from("foo");
        List<MapUpdate<String, String>> updates = Collections.singletonList(new MapUpdate<>());
        Transaction<MapUpdate<String, String>> transaction = new Transaction<>(transactionId, asyncMap);
        assertEquals(transactionId, transaction.transactionId());

        expect(asyncMap.begin(transactionId))
                .andReturn(CompletableFuture.completedFuture(new Version(1)));
        expect(asyncMap.prepare(new TransactionLog<>(transactionId, 1, updates)))
                .andReturn(CompletableFuture.completedFuture(true));
        expect(asyncMap.commit(transactionId))
                .andReturn(CompletableFuture.completedFuture(null));
        replay(asyncMap);

        assertEquals(Transaction.State.ACTIVE, transaction.state());
        assertEquals(1, transaction.begin().join().value());
        assertEquals(Transaction.State.ACTIVE, transaction.state());
        assertTrue(transaction.prepare(updates).join());
        assertEquals(Transaction.State.PREPARED, transaction.state());
        transaction.commit();
        assertEquals(Transaction.State.COMMITTED, transaction.state());
        verify(asyncMap);
    }

    @Test
    public void testTransactionFailOnOutOfOrderCalls() throws Exception {
        AsyncConsistentMap<String, String> asyncMap = strictMock(AsyncConsistentMap.class);
        TransactionId transactionId = TransactionId.from("foo");
        List<MapUpdate<String, String>> updates = Collections.singletonList(new MapUpdate<>());
        Transaction<MapUpdate<String, String>> transaction = new Transaction<>(transactionId, asyncMap);

        try {
            transaction.prepare(updates);
            fail();
        } catch (IllegalStateException e) {
        }

        try {
            transaction.commit();
            fail();
        } catch (IllegalStateException e) {
        }

        try {
            transaction.rollback();
            fail();
        } catch (IllegalStateException e) {
        }

        expect(asyncMap.begin(transactionId))
                .andReturn(CompletableFuture.completedFuture(new Version(1)));
        expect(asyncMap.prepare(new TransactionLog<>(transactionId, 1, updates)))
                .andReturn(CompletableFuture.completedFuture(true));
        replay(asyncMap);

        assertFalse(transaction.isOpen());
        assertEquals(Transaction.State.ACTIVE, transaction.state());
        assertEquals(1, transaction.begin().join().value());
        assertTrue(transaction.isOpen());
        assertEquals(Transaction.State.ACTIVE, transaction.state());
        assertTrue(transaction.prepare(updates).join());
        assertEquals(Transaction.State.PREPARED, transaction.state());

        try {
            transaction.begin();
            fail();
        } catch (IllegalStateException e) {
        }
        verify(asyncMap);
    }

    @Test
    public void testCoordinatedMapTransaction() throws Exception {
        List<Object> mocks = new ArrayList<>();

        Map<PartitionId, DefaultTransactionalMapParticipant<String, String>> participants = new HashMap<>();
        List<PartitionId> sortedParticipants = new ArrayList<>();
        TransactionId transactionId = TransactionId.from(UUID.randomUUID().toString());
        for (int i = 1; i <= 3; i++) {
            AsyncConsistentMap<String, String> asyncMap = mock(AsyncConsistentMap.class);
            mocks.add(asyncMap);

            ConsistentMap<String, String> consistentMap = new TestConsistentMap<>();
            Transaction<MapUpdate<String, String>> transaction = new Transaction<>(transactionId, asyncMap);
            PartitionId partitionId = PartitionId.from(i);
            participants.put(partitionId, new DefaultTransactionalMapParticipant<>(consistentMap, transaction));
            sortedParticipants.add(partitionId);
        }

        expect(participants.get(PartitionId.from(1)).transaction.transactionalObject
                .begin(anyObject(TransactionId.class)))
                .andReturn(CompletableFuture.completedFuture(new Version(1)));

        expect(participants.get(PartitionId.from(1)).transaction.transactionalObject.prepare(
                new TransactionLog<>(transactionId, 1, Arrays.asList(
                        MapUpdate.<String, String>newBuilder()
                                .withType(MapUpdate.Type.REMOVE_IF_VERSION_MATCH)
                                .withKey("foo")
                                .withVersion(1)
                                .build(),
                        MapUpdate.<String, String>newBuilder()
                                .withType(MapUpdate.Type.REMOVE_IF_VERSION_MATCH)
                                .withKey("baz")
                                .withVersion(2)
                                .build()
                )))).andReturn(CompletableFuture.completedFuture(true));

        expect(participants.get(PartitionId.from(1)).transaction.transactionalObject.commit(transactionId))
                .andReturn(CompletableFuture.completedFuture(null));

        expect(participants.get(PartitionId.from(3)).transaction.transactionalObject
                .begin(anyObject(TransactionId.class)))
                .andReturn(CompletableFuture.completedFuture(new Version(1)));

        expect(participants.get(PartitionId.from(3)).transaction.transactionalObject.prepare(
                new TransactionLog<>(transactionId, 1, Arrays.asList(
                        MapUpdate.<String, String>newBuilder()
                                .withType(MapUpdate.Type.PUT_IF_VERSION_MATCH)
                                .withKey("bar")
                                .withValue("baz")
                                .withVersion(1)
                                .build()
                )))).andReturn(CompletableFuture.completedFuture(true));

        expect(participants.get(PartitionId.from(3)).transaction.transactionalObject.commit(transactionId))
                .andReturn(CompletableFuture.completedFuture(null));

        TransactionManager transactionManager = mock(TransactionManager.class);
        expect(transactionManager.updateState(anyObject(TransactionId.class), anyObject(Transaction.State.class)))
                .andReturn(CompletableFuture.completedFuture(null))
                .anyTimes();
        expect(transactionManager.remove(anyObject(TransactionId.class)))
                .andReturn(CompletableFuture.completedFuture(null))
                .anyTimes();
        mocks.add(transactionManager);

        TransactionCoordinator transactionCoordinator = new TransactionCoordinator(transactionId, transactionManager);

        Hasher<String> hasher = key -> {
            int hashCode = Hashing.sha256().hashBytes(key.getBytes()).asInt();
            return sortedParticipants.get(Math.abs(hashCode) % sortedParticipants.size());
        };

        expect(transactionManager.<String, String>getTransactionalMap(anyString(), anyObject(), anyObject()))
                .andReturn(new PartitionedTransactionalMap(participants, hasher));

        replay(mocks.toArray());

        PartitionedTransactionalMap<String, String> transactionalMap = (PartitionedTransactionalMap)
                transactionCoordinator.getTransactionalMap("foo", Serializer.using(KryoNamespaces.API));

        // Sneak a couple entries in the first partition.
        transactionalMap.partitions.get(PartitionId.from(1)).backingMap.put("foo", "bar");
        transactionalMap.partitions.get(PartitionId.from(1)).backingMap.put("baz", "foo");

        assertTrue(transactionalMap.containsKey("foo"));
        assertEquals("bar", transactionalMap.remove("foo"));
        assertFalse(transactionalMap.containsKey("bar"));
        assertNull(transactionalMap.put("bar", "baz"));
        assertTrue(transactionalMap.containsKey("bar"));
        assertTrue(transactionalMap.containsKey("baz"));
        assertFalse(transactionalMap.remove("baz", "baz"));
        assertTrue(transactionalMap.remove("baz", "foo"));
        assertFalse(transactionalMap.containsKey("baz"));

        assertEquals(CommitStatus.SUCCESS, transactionCoordinator.commit().join());
        verify(mocks.toArray());
    }

    private static class TestConsistentMap<K, V> implements ConsistentMap<K, V> {
        private final Map<K, Versioned<V>> map = new HashMap<>();
        private final AtomicLong version = new AtomicLong();

        @Override
        public String name() {
            return null;
        }

        @Override
        public Type primitiveType() {
            return Type.CONSISTENT_MAP;
        }

        private long nextVersion() {
            return version.incrementAndGet();
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public boolean containsKey(K key) {
            return map.containsKey(key);
        }

        @Override
        public boolean containsValue(V value) {
            return map.containsValue(value);
        }

        @Override
        public Versioned<V> get(K key) {
            return map.get(key);
        }

        @Override
        public Versioned<V> getOrDefault(K key, V defaultValue) {
            return map.getOrDefault(key, new Versioned<>(defaultValue, 0));
        }

        @Override
        public Versioned<V> computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Versioned<V> compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Versioned<V> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Versioned<V> computeIf(K key,
                Predicate<? super V> condition, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Versioned<V> put(K key, V value) {
            return map.put(key, new Versioned<>(value, nextVersion()));
        }

        @Override
        public Versioned<V> putAndGet(K key, V value) {
            return put(key, value);
        }

        @Override
        public Versioned<V> remove(K key) {
            return map.remove(key);
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public Set<K> keySet() {
            return map.keySet();
        }

        @Override
        public Collection<Versioned<V>> values() {
            return map.values();
        }

        @Override
        public Set<Map.Entry<K, Versioned<V>>> entrySet() {
            return map.entrySet();
        }

        @Override
        public Versioned<V> putIfAbsent(K key, V value) {
            return map.putIfAbsent(key, new Versioned<>(value, nextVersion()));
        }

        @Override
        public boolean remove(K key, V value) {
            return map.remove(key, value);
        }

        @Override
        public boolean remove(K key, long version) {
            Versioned<V> value = map.get(key);
            if (value != null && value.version() == version) {
                map.remove(key);
                return true;
            }
            return false;
        }

        @Override
        public Versioned<V> replace(K key, V value) {
            return map.replace(key, new Versioned<>(value, nextVersion()));
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replace(K key, long oldVersion, V newValue) {
            Versioned<V> value = map.get(key);
            if (value != null && value.version() == oldVersion) {
                map.put(key, new Versioned<>(newValue, nextVersion()));
                return true;
            }
            return false;
        }

        @Override
        public void addListener(MapEventListener<K, V> listener, Executor executor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeListener(MapEventListener<K, V> listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<K, V> asJavaMap() {
            throw new UnsupportedOperationException();
        }
    }

}
