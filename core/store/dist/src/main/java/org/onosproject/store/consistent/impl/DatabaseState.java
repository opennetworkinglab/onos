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

import net.kuujo.copycat.state.Command;
import net.kuujo.copycat.state.Initializer;
import net.kuujo.copycat.state.Query;
import net.kuujo.copycat.state.StateContext;
import org.onosproject.store.service.Transaction;
import org.onosproject.store.service.Versioned;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
  Set<String> maps();

  @Query
  Map<String, Long> counters();

  @Query
  int mapSize(String mapName);

  @Query
  boolean mapIsEmpty(String mapName);

  @Query
  boolean mapContainsKey(String mapName, K key);

  @Query
  boolean mapContainsValue(String mapName, V value);

  @Query
  Versioned<V> mapGet(String mapName, K key);

  @Command
  Result<UpdateResult<K, V>> mapUpdate(String mapName, K key, Match<V> valueMatch, Match<Long> versionMatch, V value);

  @Command
  Result<Void> mapClear(String mapName);

  @Query
  Set<K> mapKeySet(String mapName);

  @Query
  Collection<Versioned<V>> mapValues(String mapName);

  @Query
  Set<Entry<K, Versioned<V>>> mapEntrySet(String mapName);

  @Command
  Long counterAddAndGet(String counterName, long delta);

  @Command
  Boolean counterCompareAndSet(String counterName, long expectedValue, long updateValue);

  @Command
  Long counterGetAndAdd(String counterName, long delta);

  @Query
  Long queueSize(String queueName);

  @Query
  byte[] queuePeek(String queueName);

  @Command
  byte[] queuePop(String queueName);

  @Command
  void queuePush(String queueName, byte[] entry);

  @Query
  Long counterGet(String counterName);

  @Command
  CommitResponse prepareAndCommit(Transaction transaction);

  @Command
  boolean prepare(Transaction transaction);

  @Command
  CommitResponse commit(Transaction transaction);

  @Command
  boolean rollback(Transaction transaction);
}
