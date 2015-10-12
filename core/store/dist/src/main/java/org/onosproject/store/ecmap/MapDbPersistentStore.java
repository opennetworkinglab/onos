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

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Hasher;
import org.mapdb.Serializer;
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
    }

    @Override
    public void readInto(Map<K, MapValue<V>> items) {
        this.items.forEach((keyBytes, valueBytes) ->
                              items.put(serializer.decode(keyBytes),
                                        serializer.decode(valueBytes)));
    }

    @Override
    public void update(K key, MapValue<V> value) {
        executor.submit(() -> updateInternal(key, value));
    }

    @Override
    public void remove(K key) {
        executor.submit(() -> removeInternal(key));
    }

    private void updateInternal(K key, MapValue<V> newValue) {
        byte[] keyBytes = serializer.encode(key);

        items.compute(keyBytes, (k, existingBytes) -> {
            MapValue<V> existing = existingBytes == null ? null :
                                      serializer.decode(existingBytes);
            if (existing == null || newValue.isNewerThan(existing)) {
                return serializer.encode(newValue);
            } else {
                return existingBytes;
            }
        });
        database.commit();
    }

    private void removeInternal(K key) {
        byte[] keyBytes = serializer.encode(key);
        items.remove(keyBytes);
        database.commit();
    }
}