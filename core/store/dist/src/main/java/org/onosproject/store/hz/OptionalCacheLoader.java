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
package org.onosproject.store.hz;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.store.serializers.StoreSerializer;

import com.google.common.base.Optional;
import com.google.common.cache.CacheLoader;
import com.hazelcast.core.IMap;

/**
 * CacheLoader to wrap Map value with Optional,
 * to handle negative hit on underlying IMap.
 *
 * @param <K> IMap key type after deserialization
 * @param <V> IMap value type after deserialization
 */
public final class OptionalCacheLoader<K, V> extends
        CacheLoader<K, Optional<V>> {

    private final StoreSerializer serializer;
    private IMap<byte[], byte[]> rawMap;

    /**
     * Constructor.
     *
     * @param serializer to use for serialization
     * @param rawMap underlying IMap
     */
    public OptionalCacheLoader(StoreSerializer serializer, IMap<byte[], byte[]> rawMap) {
        this.serializer = checkNotNull(serializer);
        this.rawMap = checkNotNull(rawMap);
    }

    @Override
    public Optional<V> load(K key) throws Exception {
        byte[] keyBytes = serializer.encode(key);
        byte[] valBytes = rawMap.get(keyBytes);
        if (valBytes == null) {
            return Optional.absent();
        }
        V dev = serializer.decode(valBytes);
        return Optional.of(dev);
    }
}
