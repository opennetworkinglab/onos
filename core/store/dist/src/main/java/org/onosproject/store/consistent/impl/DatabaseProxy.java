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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.onosproject.cluster.NodeId;
import org.onosproject.store.service.Transaction;
import org.onosproject.store.service.Versioned;

/**
 * Database proxy.
 */
public interface DatabaseProxy<K, V> {

    /**
     * Returns a set of all tables names.
     *
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Set<String>> tableNames();

    /**
     * Returns a mapping from counter name to next value.
     *
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Map<String, Long>> counters();

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
     * @param key       The key to check.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Boolean> containsKey(String tableName, K key);

    /**
     * Checks whether the table contains a value.
     *
     * @param tableName table name
     * @param value     The value to check.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Boolean> containsValue(String tableName, V value);

    /**
     * Gets a value from the table.
     *
     * @param tableName table name
     * @param key       The key to get.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Versioned<V>> get(String tableName, K key);

    /**
     * Puts a value in the table.
     *
     * @param tableName table name
     * @param key       The key to set.
     * @param value     The value to set.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Result<Versioned<V>>> put(String tableName, K key, V value);

    /**
     * Puts a value in the table.
     *
     * @param tableName table name
     * @param key       The key to set.
     * @param value     The value to set.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Result<UpdateResult<Versioned<V>>>> putAndGet(String tableName, K key, V value);

    /**
     * Puts a value in the table.
     *
     * @param tableName table name
     * @param key       The key to set.
     * @param value     The value to set.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Result<UpdateResult<Versioned<V>>>> putIfAbsentAndGet(String tableName, K key, V value);

    /**
     * Removes a value from the table.
     *
     * @param tableName table name
     * @param key       The key to remove.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Result<Versioned<V>>> remove(String tableName, K key);

    /**
     * Clears the table.
     *
     * @param tableName table name
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Result<Void>> clear(String tableName);

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
     * @param key       The key to set.
     * @param value     The value to set if the given key does not exist.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Result<Versioned<V>>> putIfAbsent(String tableName, K key, V value);

    /**
     * Removes a key and if the existing value for that key matches the specified value.
     *
     * @param tableName table name
     * @param key       The key to remove.
     * @param value     The value to remove.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Result<Boolean>> remove(String tableName, K key, V value);

    /**
     * Removes a key and if the existing version for that key matches the specified version.
     *
     * @param tableName table name
     * @param key       The key to remove.
     * @param version   The expected version.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Result<Boolean>> remove(String tableName, K key, long version);

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified value.
     *
     * @param tableName table name
     * @param key       The key to replace.
     * @param oldValue  The value to replace.
     * @param newValue  The value with which to replace the given key and value.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Result<Boolean>> replace(String tableName, K key, V oldValue, V newValue);

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified version.
     *
     * @param tableName  table name
     * @param key        The key to update
     * @param oldVersion existing version in the map for this replace to succeed.
     * @param newValue   The value with which to replace the given key and version.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Result<Boolean>> replace(String tableName, K key, long oldVersion, V newValue);

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified version.
     *
     * @param tableName  table name
     * @param key        The key to update
     * @param oldVersion existing version in the map for this replace to succeed.
     * @param newValue   The value with which to replace the given key and version.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Result<UpdateResult<Versioned<V>>>> replaceAndGet(String tableName,
            K key, long oldVersion,
            V newValue);

    /**
     * Atomically add the given value to current value of the specified counter.
     *
     * @param counterName counter name
     * @param delta value to add
     * @return updated value
     */
    CompletableFuture<Long> counterAddAndGet(String counterName, long delta);

    /**
     * Atomically add the given value to current value of the specified counter.
     *
     * @param counterName counter name
     * @param delta value to add
     * @return previous value
     */
    CompletableFuture<Long> counterGetAndAdd(String counterName, long delta);

    /**
     * Returns the current value of the specified atomic counter.
     *
     * @param counterName counter name
     * @return current value
     */
    CompletableFuture<Long> counterGet(String counterName);

    /**
     * Returns the size of queue.
     * @param queueName queue name
     * @return queue size
     */
    CompletableFuture<Long> queueSize(String queueName);

    /**
     * Inserts an entry into the queue.
     * @param queueName queue name
     * @param entry queue entry
     * @return set of nodes to notify about the queue update
     */
    CompletableFuture<Set<NodeId>> queuePush(String queueName, byte[] entry);

    /**
     * Removes an entry from the queue if the queue is non-empty.
     * @param queueName queue name
     * @param nodeId If the queue is empty the identifier of node to notify when an entry becomes available
     * @return entry. Can be null if queue is empty
     */
    CompletableFuture<byte[]> queuePop(String queueName, NodeId nodeId);

    /**
     * Returns but does not remove an entry from the queue.
     * @param queueName queue name
     * @return entry. Can be null if queue is empty
     */
    CompletableFuture<byte[]> queuePeek(String queueName);

    /**
     * Prepare and commit the specified transaction.
     *
     * @param transaction transaction to commit (after preparation)
     * @return A completable future to be completed with the result once complete
     */
    CompletableFuture<Boolean> prepareAndCommit(Transaction transaction);

    /**
     * Prepare the specified transaction for commit. A successful prepare implies
     * all the affected resources are locked thus ensuring no concurrent updates can interfere.
     *
     * @param transaction transaction to prepare (for commit)
     * @return A completable future to be completed with the result once complete. The future is completed
     * with true if the transaction is successfully prepared i.e. all pre-conditions are met and
     * applicable resources locked.
     */
    CompletableFuture<Boolean> prepare(Transaction transaction);

    /**
     * Commit the specified transaction. A successful commit implies
     * all the updates are applied, are now durable and are now visible externally.
     *
     * @param transaction transaction to commit
     * @return A completable future to be completed with the result once complete
     */
    CompletableFuture<Boolean> commit(Transaction transaction);

    /**
     * Rollback the specified transaction. A successful rollback implies
     * all previously acquired locks for the affected resources are released.
     *
     * @param transaction transaction to rollback
     * @return A completable future to be completed with the result once complete
     */
    CompletableFuture<Boolean> rollback(Transaction transaction);
}
