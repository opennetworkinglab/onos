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

import java.util.Collection;
import java.util.Map;

import com.google.common.base.MoreObjects;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.service.TransactionalMap;

/**
 * Partitioned transactional map.
 */
public class PartitionedTransactionalMap<K, V> implements TransactionalMap<K, V> {
    protected final Map<PartitionId, TransactionalMapParticipant<K, V>> partitions;
    protected final Hasher<K> hasher;

    public PartitionedTransactionalMap(
            Map<PartitionId, TransactionalMapParticipant<K, V>> partitions, Hasher<K> hasher) {
        this.partitions = partitions;
        this.hasher = hasher;
    }

    /**
     * Returns the collection of map partitions.
     *
     * @return a collection of map partitions
     */
    @SuppressWarnings("unchecked")
    Collection<TransactionParticipant> participants() {
        return (Collection) partitions.values();
    }

    /**
     * Returns the partition for the given key.
     *
     * @param key the key for which to return the partition
     * @return the partition for the given key
     */
    private TransactionalMap<K, V> partition(K key) {
        return partitions.get(hasher.hash(key));
    }

    @Override
    public V get(K key) {
        return partition(key).get(key);
    }

    @Override
    public boolean containsKey(K key) {
        return partition(key).containsKey(key);
    }

    @Override
    public V put(K key, V value) {
        return partition(key).put(key, value);
    }

    @Override
    public V remove(K key) {
        return partition(key).remove(key);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return partition(key).putIfAbsent(key, value);
    }

    @Override
    public boolean remove(K key, V value) {
        return partition(key).remove(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return partition(key).replace(key, oldValue, newValue);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("partitions", partitions.values())
                .toString();
    }
}