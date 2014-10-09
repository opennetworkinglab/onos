package org.onlab.onos.store.common;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onlab.onos.store.serializers.StoreSerializer;

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
