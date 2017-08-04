/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.persistence.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.mapdb.DB;
import org.mapdb.Hasher;
import org.onosproject.store.service.Serializer;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * A map implementation that stores and receives all data from a serialized internal map.
 */
public class PersistentMap<K, V> implements Map<K, V> {

    private final Serializer serializer;

    private final org.mapdb.DB database;

    private final Map<byte[], byte[]> items;

    private final String name;

    public PersistentMap(Serializer serializer, DB database, String name) {
        this.serializer = checkNotNull(serializer);
        this.database = checkNotNull(database);
        this.name = checkNotNull(name);

        items = database
                .createHashMap(name)
                .keySerializer(org.mapdb.Serializer.BYTE_ARRAY)
                .valueSerializer(org.mapdb.Serializer.BYTE_ARRAY)
                .hasher(Hasher.BYTE_ARRAY)
                .makeOrGet();
    }

    /**
     * Reads this set in deserialized form into the provided map.
     *
     * @param items the map to be populated
     */
    public void readInto(Map<K, V> items) {
        this.items.forEach((keyBytes, valueBytes) ->
                                   items.put(serializer.decode(keyBytes),
                                             serializer.decode(valueBytes)));
    }

    @Override
    public V remove(Object key) {
        checkNotNull(key, "Key can not be null.");
        V removed = get(key);
        items.remove(serializer.encode(key));
        return removed;
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        checkNotNull(key, "Key cannot be null.");
        return items.containsKey(serializer.encode(key));
    }

    @Override
    public boolean containsValue(Object value) {
        checkNotNull(value, "Value cannot be null.");
        byte[] serialized = serializer.encode(value);
        for (byte[] compareValue : items.values()) {
            boolean same = true;
            if (compareValue == null) {
                same = false;
            } else if (compareValue.length != serialized.length) {
                same = false;
            } else {
                for (int i = 0; i < serialized.length; i++) {
                    if (serialized[i] != compareValue[i]) {
                        same = false;
                        break;
                    }
                }
            }
            if (same) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        checkNotNull(key, "Key cannot be null.");
        byte[] bytes = items.get(serializer.encode(key));
        return bytes == null ? null : serializer.decode(bytes);
    }

    @Override
    public V put(K key, V value) {
        checkNotNull(key, "Key cannot be null.");
        checkNotNull(value, "Value cannot be null.");
        byte[] prevVal = items.put(serializer.encode(key), serializer.encode(value));
        if (prevVal == null) {
            return null;
        }
        return serializer.decode(prevVal);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        checkNotNull(m, "The passed in map cannot be null.");
        m.forEach((k, v) -> items.put(serializer.encode(k), serializer.encode(v)));
    }

    @Override
    public void clear() {
        items.clear();
    }

    @Override
    public Set<K> keySet() {
        Set<K> keys = Sets.newHashSet();
        items.keySet().forEach(k -> keys.add(serializer.decode(k)));
        return keys;
    }

    @Override
    public Collection<V> values() {
        Collection<V> values = Sets.newHashSet();
        items.values().forEach(v -> values.add(serializer.decode(v)));
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entries = Sets.newHashSet();
        items.entrySet().
                forEach(e -> entries.add(Maps.immutableEntry(serializer.decode(e.getKey()),
                                                             serializer.decode(e.getValue()))));
        return entries;
    }

    @Override
    public boolean equals(Object map) {
        //This is not threadsafe and on larger maps incurs a significant processing cost
        if (!(map instanceof Map)) {
            return false;
        }
        Map asMap = (Map) map;
        if (this.size() != asMap.size()) {
            return false;
        }
        for (Entry entry : this.entrySet()) {
            Object key = entry.getKey();
            if (!asMap.containsKey(key) || !asMap.get(key).equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}