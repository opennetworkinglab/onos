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
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.onosproject.store.service.UpdateOperation;
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
  public void init(StateContext<DatabaseState<K, V>> context);

  @Query
  Set<String> tableNames();

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
  Versioned<V> put(String tableName, K key, V value);

  @Command
  Versioned<V> remove(String tableName, K key);

  @Command
  void clear(String tableName);

  @Query
  Set<K> keySet(String tableName);

  @Query
  Collection<Versioned<V>> values(String tableName);

  @Query
  Set<Entry<K, Versioned<V>>> entrySet(String tableName);

  @Command
  Versioned<V> putIfAbsent(String tableName, K key, V value);

  @Command
  boolean remove(String tableName, K key, V value);

  @Command
  boolean remove(String tableName, K key, long version);

  @Command
  boolean replace(String tableName, K key, V oldValue, V newValue);

  @Command
  boolean replace(String tableName, K key, long oldVersion, V newValue);

  @Command
  boolean batchUpdate(List<UpdateOperation<K, V>> updates);
}
