package org.onlab.onos.store.common;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onlab.onos.store.serializers.KryoSerializationService;

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

    private final KryoSerializationService kryoSerializationService;
    private IMap<byte[], byte[]> rawMap;

    /**
     * Constructor.
     *
     * @param kryoSerializationService to use for serialization
     * @param rawMap underlying IMap
     */
    public OptionalCacheLoader(KryoSerializationService kryoSerializationService, IMap<byte[], byte[]> rawMap) {
        this.kryoSerializationService = checkNotNull(kryoSerializationService);
        this.rawMap = checkNotNull(rawMap);
    }

    @Override
    public Optional<V> load(K key) throws Exception {
        byte[] keyBytes = kryoSerializationService.encode(key);
        byte[] valBytes = rawMap.get(keyBytes);
        if (valBytes == null) {
            return Optional.absent();
        }
        V dev = kryoSerializationService.decode(valBytes);
        return Optional.of(dev);
    }
}
