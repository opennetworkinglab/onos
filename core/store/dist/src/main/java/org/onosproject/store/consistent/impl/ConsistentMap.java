package org.onosproject.store.consistent.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

/**
 * A distributed, strongly consistent map.
 * <p>
 * This map offers strong read-after-update (where update == create/update/delete)
 * consistency. All operations to the map are serialized and applied in a consistent
 * manner.
 * <p>
 * The stronger consistency comes at the expense of availability in
 * the event of a network partition. A network partition can be either due to
 * a temporary disruption in network connectivity between participating nodes
 * or due to a node being temporarily down.
 * </p><p>
 * All values stored in this map are versioned and the API supports optimistic
 * concurrency by allowing conditional updates that take into consideration
 * the version or value that was previously read.
 * </p><p>
 * The map also supports atomic batch updates (transactions). One can provide a list
 * of updates to be applied atomically if and only if all the operations are guaranteed
 * to succeed i.e. all their preconditions are met. For example, the precondition
 * for a putIfAbsent API call is absence of a mapping for the key. Similarly, the
 * precondition for a conditional replace operation is the presence of an expected
 * version or value
 * </p><p>
 * This map does not allow null values. All methods can throw a ConsistentMapException
 * (which extends RuntimeException) to indicate failures.
 *
 */
public interface ConsistentMap<K, V> {

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
     * Returns the value (and version) to which the specified key is mapped, or null if this
     * map contains no mapping for the key.
     *
     * @param key the key whose associated value (and version) is to be returned
     * @return the value (and version) to which the specified key is mapped, or null if
     * this map contains no mapping for the key
     */
    Versioned<V> get(K key);

    /**
     * Associates the specified value with the specified key in this map (optional operation).
     * If the map previously contained a mapping for the key, the old value is replaced by the
     * specified value.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value (and version) associated with key, or null if there was
     * no mapping for key.
     */
    Versioned<V> put(K key, V value);

    /**
     * Removes the mapping for a key from this map if it is present (optional operation).
     *
     * @param key key whose value is to be removed from the map
     * @return the value (and version) to which this map previously associated the key,
     * or null if the map contained no mapping for the key.
     */
    Versioned<V> remove(K key);

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
     * Returns the collection of values (and associated versions) contained in this map.
     * This method differs from the behavior of java.util.Map.values() in that
     * what is returned is a unmodifiable snapshot view of the values in the ConsistentMap.
     * Attempts to modify the returned collection, whether direct or via its iterator,
     * result in an UnsupportedOperationException.
     *
     * @return a collection of the values (and associated versions) contained in this map
     */
    Collection<Versioned<V>> values();

    /**
     * Returns the set of entries contained in this map.
     * This method differs from the behavior of java.util.Map.entrySet() in that
     * what is returned is a unmodifiable snapshot view of the entries in the ConsistentMap.
     * Attempts to modify the returned set, whether direct or via its iterator,
     * result in an UnsupportedOperationException.
     *
     * @return set of entries contained in this map.
     */
    Set<Entry<K, Versioned<V>>> entrySet();

    /**
     * If the specified key is not already associated with a value
     * associates it with the given value and returns null, else returns the current value.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key or null
     * if key does not already mapped to a value.
     */
    Versioned<V> putIfAbsent(K key, V value);

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
     * Removes the entry for the specified key only if its current
     * version in the map is equal to the specified version.
     *
     * @param key key with which the specified version is associated
     * @param version version expected to be associated with the specified key
     * @return true if the value was removed
     */
    boolean remove(K key, long version);

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

    /**
     * Replaces the entry for the specified key only if it is currently mapped to the
     * specified version.
     *
     * @param key key key with which the specified value is associated
     * @param oldVersion version expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return true if the value was replaced
     */
    boolean replace(K key, long oldVersion, V newValue);

    /**
     * Atomically apply the specified list of updates to the map.
     * If any of the updates cannot be applied due to a precondition
     * violation, none of the updates will be applied and the state of
     * the map remains unaltered.
     *
     * @param updates list of updates to apply atomically.
     * @return true if the map was updated.
     */
    boolean batchUpdate(List<UpdateOperation<K, V>> updates);
}
