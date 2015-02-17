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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.HexString;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.UpdateOperation;
import org.onosproject.store.service.Versioned;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * ConsistentMap implementation that is backed by a Raft consensus
 * based database.
 *
 * @param <K> type of key.
 * @param <V> type of value.
 */
public class ConsistentMapImpl<K, V> implements ConsistentMap<K, V> {

    private final String name;
    private final DatabaseProxy<String, byte[]> proxy;
    private final Serializer serializer;

    private static final int OPERATION_TIMEOUT_MILLIS = 1000;
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

    ConsistentMapImpl(String name,
            DatabaseProxy<String, byte[]> proxy,
            Serializer serializer) {
        this.name = checkNotNull(name, "map name cannot be null");
        this.proxy = checkNotNull(proxy, "database proxy cannot be null");
        this.serializer = checkNotNull(serializer, "serializer cannot be null");
    }

    @Override
    public int size() {
        return complete(proxy.size(name));
    }

    @Override
    public boolean isEmpty() {
        return complete(proxy.isEmpty(name));
    }

    @Override
    public boolean containsKey(K key) {
        checkNotNull(key, ERROR_NULL_KEY);
        return complete(proxy.containsKey(name, keyCache.getUnchecked(key)));
    }

    @Override
    public boolean containsValue(V value) {
        checkNotNull(value, ERROR_NULL_VALUE);
        return complete(proxy.containsValue(name, serializer.encode(value)));
    }

    @Override
    public Versioned<V> get(K key) {
        checkNotNull(key, ERROR_NULL_KEY);
        Versioned<byte[]> value = complete(proxy.get(name, keyCache.getUnchecked(key)));
        return (value != null) ? new Versioned<>(serializer.decode(value.value()), value.version()) : null;
    }

    @Override
    public Versioned<V> put(K key, V value) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        Versioned<byte[]> previousValue =
                complete(proxy.put(name, keyCache.getUnchecked(key), serializer.encode(value)));
        return (previousValue != null) ?
                new Versioned<>(serializer.decode(previousValue.value()), previousValue.version()) : null;

    }

    @Override
    public Versioned<V> remove(K key) {
        checkNotNull(key, ERROR_NULL_KEY);
        Versioned<byte[]> value = complete(proxy.remove(name, keyCache.getUnchecked(key)));
        return (value != null) ? new Versioned<>(serializer.decode(value.value()), value.version()) : null;
    }

    @Override
    public void clear() {
        complete(proxy.clear(name));
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(complete(proxy.keySet(name))
                .stream()
                .map(this::dK)
                .collect(Collectors.toSet()));
    }

    @Override
    public Collection<Versioned<V>> values() {
        return Collections.unmodifiableList(complete(proxy.values(name))
            .stream()
            .map(v -> new Versioned<V>(serializer.decode(v.value()), v.version()))
            .collect(Collectors.toList()));
    }

    @Override
    public Set<Entry<K, Versioned<V>>> entrySet() {
        return Collections.unmodifiableSet(complete(proxy.entrySet(name))
                .stream()
                .map(this::fromRawEntry)
                .collect(Collectors.toSet()));
    }

    @Override
    public Versioned<V> putIfAbsent(K key, V value) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        Versioned<byte[]> existingValue = complete(proxy.putIfAbsent(
                name, keyCache.getUnchecked(key), serializer.encode(value)));
        return (existingValue != null) ?
                new Versioned<>(serializer.decode(existingValue.value()), existingValue.version()) : null;
    }

    @Override
    public boolean remove(K key, V value) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        return complete(proxy.remove(name, keyCache.getUnchecked(key), serializer.encode(value)));
    }

    @Override
    public boolean remove(K key, long version) {
        checkNotNull(key, ERROR_NULL_KEY);
        return complete(proxy.remove(name, keyCache.getUnchecked(key), version));

    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(newValue, ERROR_NULL_VALUE);
        byte[] existing = oldValue != null ? serializer.encode(oldValue) : null;
        return complete(proxy.replace(name, keyCache.getUnchecked(key), existing, serializer.encode(newValue)));
    }

    @Override
    public boolean replace(K key, long oldVersion, V newValue) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(newValue, ERROR_NULL_VALUE);
        return complete(proxy.replace(name, keyCache.getUnchecked(key), oldVersion, serializer.encode(newValue)));
    }

    @Override
    public boolean batchUpdate(List<UpdateOperation<K, V>> updates) {
        checkNotNull(updates, "updates cannot be null");
        return complete(proxy.atomicBatchUpdate(updates
                .stream()
                .map(this::toRawUpdateOperation)
                .collect(Collectors.toList())));
    }

    private static <T> T complete(CompletableFuture<T> future) {
        try {
            return future.get(OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConsistentMapException.Interrupted();
        } catch (TimeoutException e) {
            throw new ConsistentMapException.Timeout();
        } catch (ExecutionException e) {
            throw new ConsistentMapException(e.getCause());
        }
    }

    private Map.Entry<K, Versioned<V>> fromRawEntry(Map.Entry<String, Versioned<byte[]>> e) {
        return Pair.of(
                dK(e.getKey()),
                new Versioned<>(
                        serializer.decode(e.getValue().value()),
                        e.getValue().version()));
    }

    private UpdateOperation<String, byte[]> toRawUpdateOperation(UpdateOperation<K, V> update) {

        checkArgument(name.equals(update.tableName()), "Unexpected table name");

        UpdateOperation.Builder<String, byte[]> rawUpdate = UpdateOperation.<String, byte[]>newBuilder();

        rawUpdate = rawUpdate.withKey(keyCache.getUnchecked(update.key()))
            .withCurrentVersion(update.currentVersion())
            .withType(update.type());

        rawUpdate = rawUpdate.withTableName(update.tableName());

        if (update.value() != null) {
            rawUpdate = rawUpdate.withValue(serializer.encode(update.value()));
        } else {
            checkState(update.type() == UpdateOperation.Type.REMOVE
                    || update.type() == UpdateOperation.Type.REMOVE_IF_VERSION_MATCH,
                    ERROR_NULL_VALUE);
        }

        if (update.currentValue() != null) {
            rawUpdate = rawUpdate.withCurrentValue(serializer.encode(update.currentValue()));
        }

        return rawUpdate.build();
    }
}