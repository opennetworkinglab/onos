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

import org.onosproject.store.service.Transaction;
import org.onosproject.store.service.Versioned;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Database proxy.
 */
public interface DatabaseProxy<K, V> {

    /**
     * Returns a set of all map names.
     *
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Set<String>> maps();

    /**
     * Returns a mapping from counter name to next value.
     *
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Map<String, Long>> counters();

    /**
     * Returns the number of entries in map.
     *
     * @param mapName map name
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Integer> mapSize(String mapName);

    /**
     * Checks whether the map is empty.
     *
     * @param mapName map name
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Boolean> mapIsEmpty(String mapName);

    /**
     * Checks whether the map contains a key.
     *
     * @param mapName map name
     * @param key     key to check.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Boolean> mapContainsKey(String mapName, K key);

    /**
     * Checks whether the map contains a value.
     *
     * @param mapName map name
     * @param value   The value to check.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Boolean> mapContainsValue(String mapName, V value);

    /**
     * Gets a value from the map.
     *
     * @param mapName map name
     * @param key     The key to get.
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Versioned<V>> mapGet(String mapName, K key);

    /**
     * Updates the map.
     *
     * @param mapName      map name
     * @param key          The key to set
     * @param valueMatch   match for checking existing value
     * @param versionMatch match for checking existing version
     * @param value        new value
     * @return A completable future to be completed with the result once complete
     */
    CompletableFuture<Result<UpdateResult<K, V>>> mapUpdate(
            String mapName, K key, Match<V> valueMatch, Match<Long> versionMatch, V value);

    /**
     * Clears the map.
     *
     * @param mapName map name
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Result<Void>> mapClear(String mapName);

    /**
     * Gets a set of keys in the map.
     *
     * @param mapName map name
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Set<K>> mapKeySet(String mapName);

    /**
     * Gets a collection of values in the map.
     *
     * @param mapName map name
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Collection<Versioned<V>>> mapValues(String mapName);

    /**
     * Gets a set of entries in the map.
     *
     * @param mapName map name
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Set<Map.Entry<K, Versioned<V>>>> mapEntrySet(String mapName);

    /**
     * Atomically add the given value to current value of the specified counter.
     *
     * @param counterName counter name
     * @param delta       value to add
     * @return updated value
     */
    CompletableFuture<Long> counterAddAndGet(String counterName, long delta);

    /**
     * Atomically add the given value to current value of the specified counter.
     *
     * @param counterName counter name
     * @param delta       value to add
     * @return previous value
     */
    CompletableFuture<Long> counterGetAndAdd(String counterName, long delta);


    /**
     * Atomically sets the given value to current value of the specified counter.
     *
     * @param counterName counter name
     * @param value       value to set
     * @return void future
     */
    CompletableFuture<Void> counterSet(String counterName, long value);

    /**
     * Atomically sets the given counter to the specified update value if and only if the current value is equal to the
     * expected value.
     * @param counterName counter name
     * @param expectedValue value to use for equivalence check
     * @param update value to set if expected value is current value
     * @return true if an update occurred, false otherwise
     */
    CompletableFuture<Boolean> counterCompareAndSet(String counterName, long expectedValue, long update);

    /**
     * Returns the current value of the specified atomic counter.
     *
     * @param counterName counter name
     * @return current value
     */
    CompletableFuture<Long> counterGet(String counterName);

    /**
     * Returns the size of queue.
     *
     * @param queueName queue name
     * @return queue size
     */
    CompletableFuture<Long> queueSize(String queueName);

    /**
     * Inserts an entry into the queue.
     *
     * @param queueName queue name
     * @param entry     queue entry
     * @return void future
     */
    CompletableFuture<Void> queuePush(String queueName, byte[] entry);

    /**
     * Removes an entry from the queue if the queue is non-empty.
     *
     * @param queueName queue name
     * @return entry future. Can be completed with null if queue is empty
     */
    CompletableFuture<byte[]> queuePop(String queueName);

    /**
     * Returns but does not remove an entry from the queue.
     *
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
    CompletableFuture<CommitResponse> prepareAndCommit(Transaction transaction);

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
    CompletableFuture<CommitResponse> commit(Transaction transaction);

    /**
     * Rollback the specified transaction. A successful rollback implies
     * all previously acquired locks for the affected resources are released.
     *
     * @param transaction transaction to rollback
     * @return A completable future to be completed with the result once complete
     */
    CompletableFuture<Boolean> rollback(Transaction transaction);
}
