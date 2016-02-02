/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.store.primitives.resources.impl;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * A batch updates to an {@code AsyncConsistentMap} be committed as a transaction.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class TransactionalMapUpdate<K, V> {
    private final TransactionId transactionId;
    private final Collection<MapUpdate<K, V>> updates;
    private boolean indexPopulated = false;
    private final Map<K, V> keyValueIndex = Maps.newHashMap();

    public TransactionalMapUpdate(TransactionId transactionId, Collection<MapUpdate<K, V>> updates) {
        this.transactionId = transactionId;
        this.updates = ImmutableList.copyOf(updates);
        populateIndex();
    }

    /**
     * Returns the transaction identifier.
     * @return transaction id
     */
    public TransactionId transactionId() {
        return transactionId;
    }

    /**
     * Returns the collection of map updates.
     * @return map updates
     */
    public Collection<MapUpdate<K, V>> batch() {
        return updates;
    }

    /**
     * Returns the value that will be associated with the key after this transaction commits.
     * @param key key
     * @return value that will be associated with the value once this transaction commits
     */
    public V valueForKey(K key) {
        if (!indexPopulated) {
            // We do not synchronize as we don't expect this called to be made from multiple threads.
            populateIndex();
        }
        return keyValueIndex.get(key);
    }

    /**
     * Populates the internal key -> value mapping.
     */
    private synchronized void populateIndex() {
        updates.forEach(mapUpdate -> {
            if (mapUpdate.value() != null) {
                keyValueIndex.put(mapUpdate.key(), mapUpdate.value());
            }
        });
        indexPopulated = true;
    }
}
