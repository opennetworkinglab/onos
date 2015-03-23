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

import static com.google.common.base.Preconditions.*;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.HexString;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Versioned;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * AsyncConsistentMap implementation that is backed by a Raft consensus
 * based database.
 *
 * @param <K> type of key.
 * @param <V> type of value.
 */
public class DefaultAsyncConsistentMap<K, V> implements AsyncConsistentMap<K, V> {

    private final String name;
    private final Database database;
    private final Serializer serializer;

    private static final String ERROR_NULL_KEY = "Key cannot be null";
    private static final String ERROR_NULL_VALUE = "Null values are not allowed";

    private final LoadingCache<K, String> keyCache = CacheBuilder.newBuilder()
            .softValues()
            .build(new CacheLoader<K, String>() {

                @Override
                public String load(K key) {
                    return HexString.toHexString(serializer.encode(key));
                }
            });

    protected K dK(String key) {
        return serializer.decode(HexString.fromHexString(key));
    }

    public DefaultAsyncConsistentMap(String name,
            Database database,
            Serializer serializer) {
        this.name = checkNotNull(name, "map name cannot be null");
        this.database = checkNotNull(database, "database cannot be null");
        this.serializer = checkNotNull(serializer, "serializer cannot be null");
    }

    @Override
    public CompletableFuture<Integer> size() {
        return database.size(name);
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return database.isEmpty(name);
    }

    @Override
    public CompletableFuture<Boolean> containsKey(K key) {
        checkNotNull(key, ERROR_NULL_KEY);
        return database.containsKey(name, keyCache.getUnchecked(key));
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        checkNotNull(value, ERROR_NULL_VALUE);
        return database.containsValue(name, serializer.encode(value));
    }

    @Override
    public CompletableFuture<Versioned<V>> get(K key) {
        checkNotNull(key, ERROR_NULL_KEY);
        return database.get(name, keyCache.getUnchecked(key))
            .thenApply(v -> v != null
            ? new Versioned<>(serializer.decode(v.value()), v.version(), v.creationTime()) : null);
    }

    @Override
    public CompletableFuture<Versioned<V>> put(K key, V value) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        return database.put(name, keyCache.getUnchecked(key), serializer.encode(value))
                .thenApply(v -> v != null
                ? new Versioned<>(serializer.decode(v.value()), v.version(), v.creationTime()) : null);
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(K key) {
        checkNotNull(key, ERROR_NULL_KEY);
        return database.remove(name, keyCache.getUnchecked(key))
                .thenApply(v -> v != null
                ? new Versioned<>(serializer.decode(v.value()), v.version(), v.creationTime()) : null);
    }

    @Override
    public CompletableFuture<Void> clear() {
        return database.clear(name);
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        return database.keySet(name)
                .thenApply(s -> s
                .stream()
                .map(this::dK)
                .collect(Collectors.toSet()));
    }

    @Override
    public CompletableFuture<Collection<Versioned<V>>> values() {
        return database.values(name).thenApply(c -> c
            .stream()
            .map(v -> new Versioned<V>(serializer.decode(v.value()), v.version(), v.creationTime()))
            .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Set<Entry<K, Versioned<V>>>> entrySet() {
        return database.entrySet(name).thenApply(s -> s
                .stream()
                .map(this::fromRawEntry)
                .collect(Collectors.toSet()));
    }

    @Override
    public CompletableFuture<Versioned<V>> putIfAbsent(K key, V value) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        return database.putIfAbsent(
                name, keyCache.getUnchecked(key), serializer.encode(value)).thenApply(v ->
                v != null ?
                new Versioned<>(serializer.decode(v.value()), v.version(), v.creationTime()) : null);
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        return database.remove(name, keyCache.getUnchecked(key), serializer.encode(value));
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long version) {
        checkNotNull(key, ERROR_NULL_KEY);
        return database.remove(name, keyCache.getUnchecked(key), version);

    }

    @Override
    public CompletableFuture<Boolean> replace(K key, V oldValue, V newValue) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(newValue, ERROR_NULL_VALUE);
        byte[] existing = oldValue != null ? serializer.encode(oldValue) : null;
        return database.replace(name, keyCache.getUnchecked(key), existing, serializer.encode(newValue));
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long oldVersion, V newValue) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(newValue, ERROR_NULL_VALUE);
        return database.replace(name, keyCache.getUnchecked(key), oldVersion, serializer.encode(newValue));
    }

    private Map.Entry<K, Versioned<V>> fromRawEntry(Map.Entry<String, Versioned<byte[]>> e) {
        return Pair.of(
                dK(e.getKey()),
                new Versioned<>(
                        serializer.decode(e.getValue().value()),
                        e.getValue().version(),
                        e.getValue().creationTime()));
    }
}