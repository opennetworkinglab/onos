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
package org.onosproject.store.service;

import java.util.List;
import java.util.function.Function;

import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Collection of map updates to be committed atomically.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class MapTransaction<K, V> {

    private final TransactionId transactionId;
    private final List<MapUpdate<K, V>> updates;

    public MapTransaction(TransactionId transactionId, List<MapUpdate<K, V>> updates) {
        this.transactionId = transactionId;
        this.updates = ImmutableList.copyOf(updates);
    }

    /**
     * Returns the transaction identifier.
     *
     * @return transaction id
     */
    public TransactionId transactionId() {
        return transactionId;
    }

    /**
     * Returns the list of map updates.
     *
     * @return map updates
     */
    public List<MapUpdate<K, V>> updates() {
        return updates;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("transactionId", transactionId)
                .add("updates", updates)
                .toString();
    }

    /**
     * Maps this instance to another {@code MapTransaction} with different key and value types.
     *
     * @param keyMapper function for mapping key types
     * @param valueMapper function for mapping value types
     * @return newly typed instance
     *
     * @param <S> key type of returned instance
     * @param <T> value type of returned instance
     */
    public <S, T> MapTransaction<S, T> map(Function<K, S> keyMapper, Function<V, T> valueMapper) {
        return new MapTransaction<>(transactionId, Lists.transform(updates, u -> u.map(keyMapper, valueMapper)));
    }
}
