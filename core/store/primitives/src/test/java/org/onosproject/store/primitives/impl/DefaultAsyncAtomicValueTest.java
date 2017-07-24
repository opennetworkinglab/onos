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
package org.onosproject.store.primitives.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.util.Tools;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Version;
import org.onosproject.store.service.Versioned;
import org.onosproject.utils.MeteringAgent;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;


public class DefaultAsyncAtomicValueTest {
    DefaultAsyncAtomicValue defaultAsyncAtomicValue;
    DefaultAsyncAtomicValue defaultAsyncAtomicValue1;


    private AsyncConsistentMap<String, byte[]> asyncMap;
    private Map<String, Versioned<byte[]>> map;

    private Serializer serializer;
    private MeteringAgent meteringAgent;


    private static final String NAME = "atomicValue";
    private static final String NAME1 = "atomicValue1";
    private static final String TEST = "foo";
    private static final String TEST1 = "bar";
    private static final int INTNAME = 20;
    private static final long VERSION1 = 1;

    private final byte[] value1 = Tools.getBytesUtf8(NAME);
    private final byte[] value2 = Tools.getBytesUtf8(NAME1);
    private final byte[] value3 = Tools.getBytesUtf8("tester");
    private final byte[] defaultValue = Tools.getBytesUtf8("default");

    @Before
    public void setUp() throws Exception {
        map = new HashMap<>();
        asyncMap = new AsyncConsistentMap<String, byte[]>() {
            @Override
            public CompletableFuture<Integer> size() {
                return CompletableFuture.completedFuture(map.size());
            }

            @Override
            public CompletableFuture<Boolean> containsKey(String key) {
                return CompletableFuture.completedFuture(map.containsKey(key));
            }

            @Override
            public CompletableFuture<Boolean> containsValue(byte[] value) {
                return CompletableFuture.completedFuture(map.containsValue(value));
            }

            @Override
            public CompletableFuture<Versioned<byte[]>> get(String key) {
                return CompletableFuture.completedFuture(map.get(key));
            }

            @Override
            public CompletableFuture<Versioned<byte[]>> getOrDefault(String key, byte[] defaultValue) {
                return CompletableFuture.completedFuture(map.getOrDefault(key, new Versioned<byte[]>(defaultValue,
                        VERSION1)));
            }

            @Override
            public CompletableFuture<Versioned<byte[]>> computeIf(String key, Predicate<? super byte[]> condition,
                                                                  BiFunction<? super String, ? super byte[],
                                                                          ? extends byte[]> remappingFunction) {
                return null;
            }

            @Override
            public CompletableFuture<Versioned<byte[]>> put(String key, byte[] value) {
                return CompletableFuture.completedFuture(map.put(key, new Versioned<byte[]>(value, VERSION1)));
            }


            @Override
            public CompletableFuture<Versioned<byte[]>> putAndGet(String key, byte[] value) {
                return null;
            }

            @Override
            public CompletableFuture<Versioned<byte[]>> remove(String key) {
                return CompletableFuture.completedFuture(map.remove(key));
            }

            @Override
            public CompletableFuture<Void> clear() {
                return null;
            }

            @Override
            public CompletableFuture<Set<String>> keySet() {
                return null;
            }

            @Override
            public CompletableFuture<Collection<Versioned<byte[]>>> values() {
                return CompletableFuture.completedFuture(map.values());
            }

            @Override
            public CompletableFuture<Set<Map.Entry<String, Versioned<byte[]>>>> entrySet() {
                return CompletableFuture.completedFuture(map.entrySet());
            }

            @Override
            public CompletableFuture<Versioned<byte[]>> putIfAbsent(String key, byte[] value) {
                return CompletableFuture.completedFuture(map.putIfAbsent(key, new Versioned<byte[]>(value, 2)));
            }

            @Override
            public CompletableFuture<Boolean> remove(String key, byte[] value) {
                return CompletableFuture.completedFuture(map.remove(key, value));
            }

            @Override
            public CompletableFuture<Boolean> remove(String key, long version) {
                Versioned versioned = map.get(key);
                if (versioned.version() == version) {
                    map.remove(key);
                    return CompletableFuture.completedFuture(true);
                }
                return CompletableFuture.completedFuture(false);
            }

            @Override
            public CompletableFuture<Versioned<byte[]>> replace(String key, byte[] value) {
                return CompletableFuture.completedFuture(map.replace(key, new Versioned<byte[]>(value, VERSION1)));
            }

            @Override
            public CompletableFuture<Boolean> replace(String key, byte[] oldValue, byte[] newValue) {
                Versioned<byte[]> currentValue = map.get(key);
                if (currentValue == null) {
                    return CompletableFuture.completedFuture(false);
                }

                if (Arrays.equals(currentValue.value(), oldValue)) {
                    map.put(key, new Versioned<>(newValue, VERSION1));
                    return CompletableFuture.completedFuture(true);
                }
                return CompletableFuture.completedFuture(false);
            }

            @Override
            public CompletableFuture<Boolean> replace(String key, long oldVersion, byte[] newValue) {
                Versioned versioned = map.get(key);
                if (versioned != null && versioned.version() == oldVersion) {
                    map.put(key, new Versioned<byte[]>(newValue, VERSION1));
                    return CompletableFuture.completedFuture(true);
                }
                return CompletableFuture.completedFuture(false);
            }

            @Override
            public CompletableFuture<Void> addListener(MapEventListener<String, byte[]> listener, Executor executor) {
                return null;
            }

            @Override
            public CompletableFuture<Void> removeListener(MapEventListener<String, byte[]> listener) {
                return null;
            }

            @Override
            public String name() {
                return null;
            }

            @Override
            public CompletableFuture<Version> begin(TransactionId transactionId) {
                return null;
            }

            @Override
            public CompletableFuture<Boolean> prepare(TransactionLog<MapUpdate<String, byte[]>> transactionLog) {
                return null;
            }

            @Override
            public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<MapUpdate<String,
                    byte[]>> transactionLog) {
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
        };

        serializer = Serializer.using(KryoNamespaces.BASIC);
        meteringAgent = new MeteringAgent(NAME, "*", false);
        defaultAsyncAtomicValue = new DefaultAsyncAtomicValue(NAME, serializer,
                asyncMap, meteringAgent);
        defaultAsyncAtomicValue1 = new DefaultAsyncAtomicValue(NAME1, serializer,
                asyncMap, meteringAgent);

    }

    @After
    public void tearDown() throws Exception {
        defaultAsyncAtomicValue.destroy();
    }

    @Test
    public void testAsyncMapping() {
        assertThat(asyncMap.size().join(), is(0));
        asyncMap.put(TEST, value1);
        asyncMap.put(TEST1, value2);
        asyncMap.put("default", defaultValue);

        assertThat(asyncMap.getOrDefault("noMatch", defaultValue).join().value(),
                is(asyncMap.get("default").join().value()));

        assertThat(asyncMap.size().join(), is(3));
        assertThat(asyncMap.get(TEST).join().value(), is(value1));

        assertThat(asyncMap.getOrDefault(TEST, Tools.getBytesUtf8("newTest")).join().value(),
                is(asyncMap.get(TEST).join().value()));

        assertThat(asyncMap.containsKey(TEST).join(), is(true));

        asyncMap.put(TEST, value3);
        assertThat(asyncMap.get(TEST).join().value(), is(value3));
        asyncMap.putIfAbsent(TEST, value3);
        assertThat(asyncMap.size().join(), is(3));

        asyncMap.replace(TEST, value3, value1);
        assertThat(asyncMap.get(TEST).join().value(), is(value1));

        asyncMap.replace(TEST, VERSION1, value3);
        assertThat(asyncMap.get(TEST).join().value(), is(value3));

        asyncMap.replace(TEST, value3, defaultValue);
        assertThat(asyncMap.get(TEST).join().value(), is(defaultValue));
        asyncMap.replace(TEST, value1);
        assertThat(asyncMap.get(TEST).join().value(), is(value1));

        asyncMap.remove(TEST, value2);

        assertThat(asyncMap.size().join(), is(3));
    }

    @Test
    public void testAsync() {
        asyncMap.put(TEST, value1);
        asyncMap.put(TEST1, value2);

        assertNull(defaultAsyncAtomicValue.get().join());
        defaultAsyncAtomicValue = new DefaultAsyncAtomicValue(NAME, serializer,
                asyncMap, meteringAgent);
        assertThat(defaultAsyncAtomicValue.name(), is(NAME));
        defaultAsyncAtomicValue.set(null);
        assertNull(defaultAsyncAtomicValue.get().join());

        defaultAsyncAtomicValue.set(INTNAME);
        assertThat(defaultAsyncAtomicValue.get().join(), is(INTNAME));

        defaultAsyncAtomicValue.set(value1);
        assertThat(defaultAsyncAtomicValue.get().join(), is(value1));

        defaultAsyncAtomicValue.compareAndSet(value1, value3).join();
        assertThat(defaultAsyncAtomicValue.get().join(), is(value3));

        assertThat(defaultAsyncAtomicValue.compareAndSet(value3, value1).join(),
                is(true));
        assertThat(defaultAsyncAtomicValue.get().join(), is(value1));

        defaultAsyncAtomicValue.getAndSet(null);
        assertNull(defaultAsyncAtomicValue.get().join());

        defaultAsyncAtomicValue.getAndSet(value3);
        assertThat(defaultAsyncAtomicValue.get().join(), is(value3));
    }
}