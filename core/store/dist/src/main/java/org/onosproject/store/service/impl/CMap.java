/*
 * Copyright 2014 Open Networking Laboratory
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

package org.onosproject.store.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;

import java.util.Map;

import org.onosproject.store.serializers.StoreSerializer;
import org.onosproject.store.service.DatabaseAdminService;
import org.onosproject.store.service.DatabaseException;
import org.onosproject.store.service.DatabaseService;
import org.onosproject.store.service.VersionedValue;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.FluentIterable;

/**
 * Map like interface wrapper around DatabaseService.
 *
 * @param <K> Key type of the map.
 *       The type must have toString(), which can uniquely identify the entry.
 * @param <V> Value type
 */
public class CMap<K, V> {

    @SuppressWarnings("unused")
    private final DatabaseAdminService dbAdminService;

    private final DatabaseService dbService;

    private final String tableName;
    private final StoreSerializer serializer;

    private final LoadingCache<K, String> keyCache;

    /**
     * Creates a CMap instance.
     * It will create the table if necessary.
     *
     * @param dbAdminService DatabaseAdminService to use for this instance
     * @param dbService DatabaseService to use for this instance
     * @param tableName table which this Map corresponds to
     * @param serializer Value serializer
     */
    public CMap(DatabaseAdminService dbAdminService,
                DatabaseService dbService,
                String tableName,
                StoreSerializer serializer) {

        this.dbAdminService = checkNotNull(dbAdminService);
        this.dbService = checkNotNull(dbService);
        this.tableName = checkNotNull(tableName);
        this.serializer = checkNotNull(serializer);

        boolean tableReady = false;
        do {
            try {
                if (!dbAdminService.listTables().contains(tableName)) {
                    dbAdminService.createTable(tableName);
                }
                tableReady = true;
            } catch (DatabaseException e) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    throw new DatabaseException(e1);
                }
            }
        } while (!tableReady);

        keyCache = CacheBuilder.newBuilder()
                    .softValues()
                    .build(new CacheLoader<K, String>() {

                        @Override
                        public String load(K key) {
                            return key.toString();
                        }
                    });
    }

    protected String sK(K key) {
        return keyCache.getUnchecked(key);
    }

    protected byte[] sV(V val) {
        return serializer.encode(val);
    }

    protected V dV(byte[] valBytes) {
        return serializer.decode(valBytes);
    }

    /**
     * Puts an entry to the map, if not already present.
     *
     * @param key the key of the value to put if absent
     * @param value the value to be put if previous value does not exist
     * @return true if put was successful.
     */
    public boolean putIfAbsent(K key, V value) {
        return dbService.putIfAbsent(tableName, sK(key), sV(value));
    }

    /**
     * Removes an entry associated to specified key.
     *
     * @param key key of the value to remove
     * @return previous value in the map for the key
     */
    public V remove(K key) {
        VersionedValue removed = dbService.remove(tableName, sK(key));
        if (removed == null) {
            return null;
        }
        return dV(removed.value());
    }

    /**
     * Returns the size of the map.
     *
     * @return size of the map
     */
    public long size() {
        // TODO this is very inefficient
        return dbService.getAll(tableName).size();
    }

    /**
     * Returns all the values contained in the map.
     *
     * @return values containd in this map
     */
    public Iterable<V> values() {
        Map<String, VersionedValue> all = dbService.getAll(tableName);
        return FluentIterable.from(all.values())
                .transform(new Function<VersionedValue, V>() {

                    @Override
                    public V apply(VersionedValue input) {
                        if (input == null) {
                            return null;
                        }
                        return dV(input.value());
                    }
                })
                .filter(notNull());
    }

    /**
     * Gets the value in the map.
     *
     * @param key to get from the map
     * @return value associated with the key, null if not such entry
     */
    public V get(K key) {
        VersionedValue vv = dbService.get(tableName, sK(key));
        if (vv == null) {
            return null;
        }
        return dV(vv.value());
    }

    /**
     * Replaces the value in the map if the value matches the expected.
     *
     * @param key of the entry to replace
     * @param oldVal value expected to be in the map
     * @param newVal value to be replaced with
     * @return true if successfully replaced
     */
    public boolean replace(K key, V oldVal, V newVal) {
        return dbService.putIfValueMatches(tableName, sK(key), sV(oldVal), sV(newVal));
    }

    /**
     * Puts a value int the map.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return previous value or null if not such entry
     */
    public V put(K key, V value) {
        VersionedValue vv = dbService.put(tableName, sK(key), sV(value));
        if (vv == null) {
            return null;
        }
        return dV(vv.value());
    }
}
