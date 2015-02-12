package org.onosproject.store.consistent.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.onosproject.store.service.UpdateOperation;
import org.onosproject.store.service.Versioned;

/**
 * Database proxy.
 */
public interface DatabaseProxy<K, V> {

  /**
   * Gets the table size.
   *
   * @param tableName table name
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Integer> size(String tableName);

  /**
   * Checks whether the table is empty.
   *
   * @param tableName table name
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Boolean> isEmpty(String tableName);

  /**
   * Checks whether the table contains a key.
   *
   * @param tableName table name
   * @param key The key to check.
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Boolean> containsKey(String tableName, K key);

  /**
   * Checks whether the table contains a value.
   *
   * @param tableName table name
   * @param value The value to check.
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Boolean> containsValue(String tableName, V value);

  /**
   * Gets a value from the table.
   *
   * @param tableName table name
   * @param key The key to get.
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Versioned<V>> get(String tableName, K key);

  /**
   * Puts a value in the table.
   *
   * @param tableName table name
   * @param key The key to set.
   * @param value The value to set.
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Versioned<V>> put(String tableName, K key, V value);

  /**
   * Removes a value from the table.
   *
   * @param tableName table name
   * @param key The key to remove.
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Versioned<V>> remove(String tableName, K key);

  /**
   * Clears the table.
   *
   * @param tableName table name
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Void> clear(String tableName);

  /**
   * Gets a set of keys in the table.
   *
   * @param tableName table name
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Set<K>> keySet(String tableName);

  /**
   * Gets a collection of values in the table.
   *
   * @param tableName table name
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Collection<Versioned<V>>> values(String tableName);

  /**
   * Gets a set of entries in the table.
   *
   * @param tableName table name
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Set<Map.Entry<K, Versioned<V>>>> entrySet(String tableName);

  /**
   * Puts a value in the table if the given key does not exist.
   *
   * @param tableName table name
   * @param key The key to set.
   * @param value The value to set if the given key does not exist.
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Versioned<V>> putIfAbsent(String tableName, K key, V value);

  /**
   * Removes a key and if the existing value for that key matches the specified value.
   *
   * @param tableName table name
   * @param key The key to remove.
   * @param value The value to remove.
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Boolean> remove(String tableName, K key, V value);

  /**
   * Removes a key and if the existing version for that key matches the specified version.
   *
   * @param tableName table name
   * @param key The key to remove.
   * @param version The expected version.
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Boolean> remove(String tableName, K key, long version);

  /**
   * Replaces the entry for the specified key only if currently mapped to the specified value.
   *
   * @param tableName table name
   * @param key The key to replace.
   * @param oldValue The value to replace.
   * @param newValue The value with which to replace the given key and value.
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Boolean> replace(String tableName, K key, V oldValue, V newValue);

  /**
   * Replaces the entry for the specified key only if currently mapped to the specified version.
   *
   * @param tableName table name
   * @param key The key to update
   * @param oldVersion existing version in the map for this replace to succeed.
   * @param newValue The value with which to replace the given key and version.
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Boolean> replace(String tableName, K key, long oldVersion, V newValue);

  /**
   * Perform a atomic batch update operation i.e. either all operations in batch succeed or
   * none do and no state changes are made.
   *
   * @param updates list of updates to apply atomically.
   * @return A completable future to be completed with the result once complete.
   */
  CompletableFuture<Boolean> atomicBatchUpdate(List<UpdateOperation<K, V>> updates);
}
