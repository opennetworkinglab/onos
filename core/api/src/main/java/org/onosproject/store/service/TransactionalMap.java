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

package org.onosproject.store.service;

import java.util.Collection;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Transactional Map data structure.
 * <p>
 * A TransactionalMap is created by invoking {@link TransactionContext#createTransactionalMap createTransactionalMap}
 * method. All operations performed on this map with in a transaction boundary are invisible externally
 * until the point when the transaction commits. A commit usually succeeds in the absence of conflicts.
 *
 * @param <K> type of key.
 * @param <V> type of value.
 */
public interface TransactionalMap<K, V> {

    /**
     * Returns the number of entries in the map.
     *
     * @return map size.
     */
    int size();

    /**
     * Returns true if the map is empty.
     *
     * @return true if map has no entries, false otherwise.
     */
    boolean isEmpty();

    /**
     * Returns true if this map contains a mapping for the specified key.
     *
     * @param key key
     * @return true if map contains key, false otherwise.
     */
    boolean containsKey(K key);

    /**
     * Returns true if this map contains the specified value.
     *
     * @param value value
     * @return true if map contains value, false otherwise.
     */
    boolean containsValue(V value);

    /**
     * Returns the value to which the specified key is mapped, or null if this
     * map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or null if
     * this map contains no mapping for the key
     */
    V get(K key);

    /**
     * Associates the specified value with the specified key in this map (optional operation).
     * If the map previously contained a mapping for the key, the old value is replaced by the
     * specified value.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with key, or null if there was
     * no mapping for key.
     */
    V put(K key, V value);

    /**
     * Removes the mapping for a key from this map if it is present (optional operation).
     *
     * @param key key whose value is to be removed from the map
     * @return the value to which this map previously associated the key,
     * or null if the map contained no mapping for the key.
     */
    V remove(K key);

    /**
     * Removes all of the mappings from this map (optional operation).
     * The map will be empty after this call returns.
     */
    void clear();

    /**
     * Returns a Set view of the keys contained in this map.
     * This method differs from the behavior of java.util.Map.keySet() in that
     * what is returned is a unmodifiable snapshot view of the keys in the ConsistentMap.
     * Attempts to modify the returned set, whether direct or via its iterator,
     * result in an UnsupportedOperationException.
     *
     * @return a set of the keys contained in this map
     */
    Set<K> keySet();

    /**
     * Returns the collection of values contained in this map.
     * This method differs from the behavior of java.util.Map.values() in that
     * what is returned is a unmodifiable snapshot view of the values in the ConsistentMap.
     * Attempts to modify the returned collection, whether direct or via its iterator,
     * result in an UnsupportedOperationException.
     *
     * @return a collection of the values contained in this map
     */
    Collection<V> values();

    /**
     * Returns the set of entries contained in this map.
     * This method differs from the behavior of java.util.Map.entrySet() in that
     * what is returned is a unmodifiable snapshot view of the entries in the ConsistentMap.
     * Attempts to modify the returned set, whether direct or via its iterator,
     * result in an UnsupportedOperationException.
     *
     * @return set of entries contained in this map.
     */
    Set<Entry<K, V>> entrySet();

    /**
     * If the specified key is not already associated with a value
     * associates it with the given value and returns null, else returns the current value.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key or null
     * if key does not already mapped to a value.
     */
    V putIfAbsent(K key, V value);

    /**
     * Removes the entry for the specified key only if it is currently
     * mapped to the specified value.
     *
     * @param key key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     * @return true if the value was removed
     */
    boolean remove(K key, V value);

    /**
     * Replaces the entry for the specified key only if currently mapped
     * to the specified value.
     *
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return true if the value was replaced
     */
    boolean replace(K key, V oldValue, V newValue);
}