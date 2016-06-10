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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.HexString;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapTransaction;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionalMap;
import org.onosproject.store.service.Versioned;

import static com.google.common.base.Preconditions.*;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Default Transactional Map implementation that provides a repeatable reads
 * transaction isolation level.
 *
 * @param <K> key type
 * @param <V> value type.
 */
public class DefaultTransactionalMap<K, V> implements TransactionalMap<K, V>, TransactionParticipant {

    private final TransactionContext txContext;
    private static final String TX_CLOSED_ERROR = "Transaction is closed";
    private final AsyncConsistentMap<K, V> backingMap;
    private final ConsistentMap<K, V> backingConsistentMap;
    private final String name;
    private final Serializer serializer;
    private final Map<K, Versioned<V>> readCache = Maps.newConcurrentMap();
    private final Map<K, V> writeCache = Maps.newConcurrentMap();
    private final Set<K> deleteSet = Sets.newConcurrentHashSet();

    private static final String ERROR_NULL_VALUE = "Null values are not allowed";
    private static final String ERROR_NULL_KEY = "Null key is not allowed";

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

    public DefaultTransactionalMap(
            String name,
            AsyncConsistentMap<K, V> backingMap,
            TransactionContext txContext,
            Serializer serializer) {
        this.name = name;
        this.backingMap = backingMap;
        this.backingConsistentMap = backingMap.asConsistentMap();
        this.txContext = txContext;
        this.serializer = serializer;
    }

    @Override
    public V get(K key) {
        checkState(txContext.isOpen(), TX_CLOSED_ERROR);
        checkNotNull(key, ERROR_NULL_KEY);
        if (deleteSet.contains(key)) {
            return null;
        }
        V latest = writeCache.get(key);
        if (latest != null) {
            return latest;
        } else {
            Versioned<V> v = readCache.computeIfAbsent(key, k -> backingConsistentMap.get(k));
            return v != null ? v.value() : null;
        }
    }

    @Override
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    @Override
    public V put(K key, V value) {
        checkState(txContext.isOpen(), TX_CLOSED_ERROR);
        checkNotNull(value, ERROR_NULL_VALUE);

        V latest = get(key);
        writeCache.put(key, value);
        deleteSet.remove(key);
        return latest;
    }

    @Override
    public V remove(K key) {
        checkState(txContext.isOpen(), TX_CLOSED_ERROR);
        V latest = get(key);
        if (latest != null) {
            writeCache.remove(key);
            deleteSet.add(key);
        }
        return latest;
    }

    @Override
    public boolean remove(K key, V value) {
        checkState(txContext.isOpen(), TX_CLOSED_ERROR);
        checkNotNull(value, ERROR_NULL_VALUE);
        V latest = get(key);
        if (Objects.equal(value, latest)) {
            remove(key);
            return true;
        }
        return false;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        checkState(txContext.isOpen(), TX_CLOSED_ERROR);
        checkNotNull(oldValue, ERROR_NULL_VALUE);
        checkNotNull(newValue, ERROR_NULL_VALUE);
        V latest = get(key);
        if (Objects.equal(oldValue, latest)) {
            put(key, newValue);
            return true;
        }
        return false;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        checkState(txContext.isOpen(), TX_CLOSED_ERROR);
        checkNotNull(value, ERROR_NULL_VALUE);
        V latest = get(key);
        if (latest == null) {
            put(key, value);
        }
        return latest;
    }

    @Override
    public CompletableFuture<Boolean> prepare() {
        return backingMap.prepare(new MapTransaction<>(txContext.transactionId(), updates()));
    }

    @Override
    public CompletableFuture<Void> commit() {
        return backingMap.commit(txContext.transactionId());
    }

    @Override
    public CompletableFuture<Void> rollback() {
        return backingMap.rollback(txContext.transactionId());
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit() {
        return backingMap.prepareAndCommit(new MapTransaction<>(txContext.transactionId(), updates()));
    }

    @Override
    public int totalUpdates() {
        return updates().size();
    }

    @Override
    public boolean hasPendingUpdates() {
        return updatesStream().findAny().isPresent();
    }

    protected Stream<MapUpdate<K, V>> updatesStream() {
        return Stream.concat(
            // 1st stream: delete ops
            deleteSet.stream()
                .map(key -> Pair.of(key, readCache.get(key)))
                .filter(e -> e.getValue() != null)
                .map(e -> MapUpdate.<K, V>newBuilder()
                 .withMapName(name)
                 .withType(MapUpdate.Type.REMOVE_IF_VERSION_MATCH)
                 .withKey(e.getKey())
                 .withCurrentVersion(e.getValue().version())
                 .build()),
            // 2nd stream: write ops
            writeCache.entrySet().stream()
                    .map(e -> {
                        Versioned<V> original = readCache.get(e.getKey());
                        if (original == null) {
                            return MapUpdate.<K, V>newBuilder()
                                    .withMapName(name)
                                    .withType(MapUpdate.Type.PUT_IF_ABSENT)
                                    .withKey(e.getKey())
                                    .withValue(e.getValue())
                                    .build();
                        } else {
                            return MapUpdate.<K, V>newBuilder()
                                    .withMapName(name)
                                    .withType(MapUpdate.Type.PUT_IF_VERSION_MATCH)
                                    .withKey(e.getKey())
                                    .withCurrentVersion(original.version())
                                    .withValue(e.getValue())
                                    .build();
                        }
                    }));
    }

    protected List<MapUpdate<K, V>> updates() {
        return updatesStream().collect(Collectors.toList());
    }

    protected List<MapUpdate<String, byte[]>> toMapUpdates() {
        List<MapUpdate<String, byte[]>> updates = Lists.newLinkedList();
        deleteSet.forEach(key -> {
            Versioned<V> original = readCache.get(key);
            if (original != null) {
                updates.add(MapUpdate.<String, byte[]>newBuilder()
                        .withMapName(name)
                        .withType(MapUpdate.Type.REMOVE_IF_VERSION_MATCH)
                        .withKey(keyCache.getUnchecked(key))
                        .withCurrentVersion(original.version())
                        .build());
            }
        });
        writeCache.forEach((key, value) -> {
            Versioned<V> original = readCache.get(key);
            if (original == null) {
                updates.add(MapUpdate.<String, byte[]>newBuilder()
                        .withMapName(name)
                        .withType(MapUpdate.Type.PUT_IF_ABSENT)
                        .withKey(keyCache.getUnchecked(key))
                        .withValue(serializer.encode(value))
                        .build());
            } else {
                updates.add(MapUpdate.<String, byte[]>newBuilder()
                        .withMapName(name)
                        .withType(MapUpdate.Type.PUT_IF_VERSION_MATCH)
                        .withKey(keyCache.getUnchecked(key))
                        .withCurrentVersion(original.version())
                        .withValue(serializer.encode(value))
                        .build());
            }
        });
        return updates;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("backingMap", backingMap)
                .add("updates", updates())
                .toString();
    }

    /**
     * Discards all changes made to this transactional map.
     */
    protected void abort() {
        readCache.clear();
        writeCache.clear();
        deleteSet.clear();
    }
}
