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
import java.util.Map.Entry;
import java.util.Set;

import org.onosproject.cluster.NodeId;
import org.onosproject.store.service.Transaction;
import org.onosproject.store.service.Versioned;

import net.kuujo.copycat.state.Command;
import net.kuujo.copycat.state.Initializer;
import net.kuujo.copycat.state.Query;
import net.kuujo.copycat.state.StateContext;

/**
 * Database state.
 *
 */
public interface DatabaseState<K, V> {

  /**
   * Initializes the database state.
   *
   * @param context The map state context.
   */
  @Initializer
  void init(StateContext<DatabaseState<K, V>> context);

  @Query
  Set<String> tableNames();

  @Query
  Map<String, Long> counters();

  @Query
  int size(String tableName);

  @Query
  boolean isEmpty(String tableName);

  @Query
  boolean containsKey(String tableName, K key);

  @Query
  boolean containsValue(String tableName, V value);

  @Query
  Versioned<V> get(String tableName, K key);

  @Command
  Result<Versioned<V>> put(String tableName, K key, V value);

  @Command
  Result<UpdateResult<Versioned<V>>> putAndGet(String tableName, K key, V value);

  @Command
  Result<UpdateResult<Versioned<V>>> putIfAbsentAndGet(String tableName, K key, V value);

  @Command
  Result<Versioned<V>> remove(String tableName, K key);

  @Command
  Result<Void> clear(String tableName);

  @Query
  Set<K> keySet(String tableName);

  @Query
  Collection<Versioned<V>> values(String tableName);

  @Query
  Set<Entry<K, Versioned<V>>> entrySet(String tableName);

  @Command
  Result<Versioned<V>> putIfAbsent(String tableName, K key, V value);

  @Command
  Result<Boolean> remove(String tableName, K key, V value);

  @Command
  Result<Boolean> remove(String tableName, K key, long version);

  @Command
  Result<Boolean> replace(String tableName, K key, V oldValue, V newValue);

  @Command
  Result<Boolean> replace(String tableName, K key, long oldVersion, V newValue);

  @Command
  Result<UpdateResult<Versioned<V>>> replaceAndGet(String tableName, K key, long oldVersion, V newValue);

  @Command
  Long counterAddAndGet(String counterName, long delta);

  @Command
  Long counterGetAndAdd(String counterName, long delta);

  @Query
  Long queueSize(String queueName);

  @Query
  byte[] queuePeek(String queueName);

  @Command
  byte[] queuePop(String queueName, NodeId requestor);

  @Command
  Set<NodeId> queuePush(String queueName, byte[] entry);

  @Query
  Long counterGet(String counterName);

  @Command
  boolean prepareAndCommit(Transaction transaction);

  @Command
  boolean prepare(Transaction transaction);

  @Command
  boolean commit(Transaction transaction);

  @Command
  boolean rollback(Transaction transaction);
}
