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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Set;

import org.onlab.util.HexString;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionalMap;
import org.onosproject.store.service.UpdateOperation;
import org.onosproject.store.service.Versioned;

import static com.google.common.base.Preconditions.*;

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
public class DefaultTransactionalMap<K, V> implements TransactionalMap<K, V> {

    private final TransactionContext txContext;
    private static final String TX_CLOSED_ERROR = "Transaction is closed";
    private final ConsistentMap<K, V> backingMap;
    private final String name;
    private final Serializer serializer;
    private final Map<K, Versioned<V>> readCache = Maps.newConcurrentMap();
    private final Map<K, V> writeCache = Maps.newConcurrentMap();
    private final Set<K> deleteSet = Sets.newConcurrentHashSet();

    public DefaultTransactionalMap(
            String name,
            ConsistentMap<K, V> backingMap,
            TransactionContext txContext,
            Serializer serializer) {
        this.name = name;
        this.backingMap = backingMap;
        this.txContext = txContext;
        this.serializer = serializer;
    }

    @Override
    public V get(K key) {
        checkState(txContext.isOpen(), TX_CLOSED_ERROR);
        if (deleteSet.contains(key)) {
            return null;
        } else if (writeCache.containsKey(key)) {
            return writeCache.get(key);
        } else {
            if (!readCache.containsKey(key)) {
                readCache.put(key, backingMap.get(key));
            }
            Versioned<V> v = readCache.get(key);
            return v != null ? v.value() : null;
        }
    }

    @Override
    public V put(K key, V value) {
        checkState(txContext.isOpen(), TX_CLOSED_ERROR);
        Versioned<V> original = readCache.get(key);
        V recentUpdate = writeCache.put(key, value);
        deleteSet.remove(key);
        return recentUpdate == null ? (original != null ? original.value() : null) : recentUpdate;
    }

    @Override
    public V remove(K key) {
        checkState(txContext.isOpen(), TX_CLOSED_ERROR);
        Versioned<V> original = readCache.get(key);
        V recentUpdate = writeCache.remove(key);
        deleteSet.add(key);
        return recentUpdate == null ? (original != null ? original.value() : null) : recentUpdate;
    }

    @Override
    public boolean remove(K key, V value) {
        V currentValue = get(key);
        if (value.equals(currentValue)) {
            remove(key);
            return true;
        }
        return false;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        V currentValue = get(key);
        if (oldValue.equals(currentValue)) {
            put(key, newValue);
            return true;
        }
        return false;
    }

    @Override
    public int size() {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(V value) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public V putIfAbsent(K key, V value) {
        V currentValue = get(key);
        if (currentValue == null) {
            put(key, value);
            return null;
        }
        return currentValue;
    }

    protected List<UpdateOperation<String, byte[]>> prepareDatabaseUpdates() {
        List<UpdateOperation<K, V>> updates = Lists.newLinkedList();
        deleteSet.forEach(key -> {
            Versioned<V> original = readCache.get(key);
            if (original != null) {
                updates.add(UpdateOperation.<K, V>newBuilder()
                        .withTableName(name)
                        .withType(UpdateOperation.Type.REMOVE_IF_VERSION_MATCH)
                        .withKey(key)
                        .withCurrentVersion(original.version())
                        .build());
            }
        });
        writeCache.forEach((key, value) -> {
            Versioned<V> original = readCache.get(key);
            if (original == null) {
                updates.add(UpdateOperation.<K, V>newBuilder()
                        .withTableName(name)
                        .withType(UpdateOperation.Type.PUT_IF_ABSENT)
                        .withKey(key)
                        .withValue(value)
                        .build());
            } else {
                updates.add(UpdateOperation.<K, V>newBuilder()
                        .withTableName(name)
                        .withType(UpdateOperation.Type.PUT_IF_VERSION_MATCH)
                        .withKey(key)
                        .withCurrentVersion(original.version())
                        .withValue(value)
                        .build());
            }
        });
        return updates.stream().map(this::toRawUpdateOperation).collect(Collectors.toList());
    }

    private UpdateOperation<String, byte[]> toRawUpdateOperation(UpdateOperation<K, V> update) {

        UpdateOperation.Builder<String, byte[]> rawUpdate = UpdateOperation.<String, byte[]>newBuilder();

        rawUpdate = rawUpdate.withKey(HexString.toHexString(serializer.encode(update.key())))
            .withCurrentVersion(update.currentVersion())
            .withType(update.type());

        rawUpdate = rawUpdate.withTableName(update.tableName());

        if (update.value() != null) {
            rawUpdate = rawUpdate.withValue(serializer.encode(update.value()));
        }

        if (update.currentValue() != null) {
            rawUpdate = rawUpdate.withCurrentValue(serializer.encode(update.currentValue()));
        }

        return rawUpdate.build();
    }

    /**
     * Discards all changes made to this transactional map.
     */
    protected void rollback() {
        readCache.clear();
        writeCache.clear();
        deleteSet.clear();
    }
}