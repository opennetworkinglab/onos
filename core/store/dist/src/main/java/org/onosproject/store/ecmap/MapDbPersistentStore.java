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

package org.onosproject.store.ecmap;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Hasher;
import org.mapdb.Serializer;
import org.onosproject.store.Timestamp;
import org.onosproject.store.impl.Timestamped;
import org.onosproject.store.serializers.KryoSerializer;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * MapDB based implementation of a persistent store.
 */
class MapDbPersistentStore<K, V> implements PersistentStore<K, V> {

    private final ExecutorService executor;
    private final KryoSerializer serializer;

    private final DB database;

    private final Map<byte[], byte[]> items;
    private final Map<byte[], byte[]> tombstones;

    /**
     * Creates a new MapDB based persistent store.
     *
     * @param filename filename of the database on disk
     * @param executor executor to use for tasks that write to the disk
     * @param serializer serializer for keys and values
     */
    MapDbPersistentStore(String filename, ExecutorService executor,
                         KryoSerializer serializer) {
        this.executor = checkNotNull(executor);
        this.serializer = checkNotNull(serializer);

        File databaseFile = new File(filename);

        database = DBMaker.newFileDB(databaseFile).make();

        items = database.createHashMap("items")
                .keySerializer(Serializer.BYTE_ARRAY)
                .valueSerializer(Serializer.BYTE_ARRAY)
                .hasher(Hasher.BYTE_ARRAY)
                .makeOrGet();

        tombstones = database.createHashMap("tombstones")
                .keySerializer(Serializer.BYTE_ARRAY)
                .valueSerializer(Serializer.BYTE_ARRAY)
                .hasher(Hasher.BYTE_ARRAY)
                .makeOrGet();
    }

    @Override
    public void readInto(Map<K, Timestamped<V>> items, Map<K, Timestamp> tombstones) {
        this.items.forEach((keyBytes, valueBytes) ->
                              items.put(serializer.decode(keyBytes),
                                               serializer.decode(valueBytes)));

        this.tombstones.forEach((keyBytes, valueBytes) ->
                                   tombstones.put(serializer.decode(keyBytes),
                                                    serializer.decode(valueBytes)));
    }

    @Override
    public void put(K key, V value, Timestamp timestamp) {
        executor.submit(() -> putInternal(key, value, timestamp));
    }

    private void putInternal(K key, V value, Timestamp timestamp) {
        byte[] keyBytes = serializer.encode(key);
        byte[] removedBytes = tombstones.get(keyBytes);

        Timestamp removed = removedBytes == null ? null :
                            serializer.decode(removedBytes);
        if (removed != null && removed.isNewerThan(timestamp)) {
            return;
        }

        final MutableBoolean updated = new MutableBoolean(false);

        items.compute(keyBytes, (k, existingBytes) -> {
            Timestamped<V> existing = existingBytes == null ? null :
                                      serializer.decode(existingBytes);
            if (existing != null && existing.isNewerThan(timestamp)) {
                updated.setFalse();
                return existingBytes;
            } else {
                updated.setTrue();
                return serializer.encode(new Timestamped<>(value, timestamp));
            }
        });

        boolean success = updated.booleanValue();

        if (success && removed != null) {
            tombstones.remove(keyBytes, removedBytes);
        }

        database.commit();
    }

    @Override
    public void remove(K key, Timestamp timestamp) {
        executor.submit(() -> removeInternal(key, timestamp));
    }

    private void removeInternal(K key, Timestamp timestamp) {
        byte[] keyBytes = serializer.encode(key);

        final MutableBoolean updated = new MutableBoolean(false);

        items.compute(keyBytes, (k, existingBytes) -> {
            Timestamp existing = existingBytes == null ? null :
                                 serializer.decode(existingBytes);
            if (existing != null && existing.isNewerThan(timestamp)) {
                updated.setFalse();
                return existingBytes;
            } else {
                updated.setTrue();
                // remove from items map
                return null;
            }
        });

        if (!updated.booleanValue()) {
            return;
        }

        byte[] timestampBytes = serializer.encode(timestamp);
        byte[] removedBytes = tombstones.get(keyBytes);

        Timestamp removedTimestamp = removedBytes == null ? null :
                                     serializer.decode(removedBytes);
        if (removedTimestamp == null) {
            tombstones.putIfAbsent(keyBytes, timestampBytes);
        } else if (timestamp.isNewerThan(removedTimestamp)) {
            tombstones.replace(keyBytes, removedBytes, timestampBytes);
        }

        database.commit();
    }

}
