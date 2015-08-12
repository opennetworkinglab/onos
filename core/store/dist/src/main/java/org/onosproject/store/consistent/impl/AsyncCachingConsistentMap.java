package org.onosproject.store.consistent.impl;

import java.util.concurrent.CompletableFuture;

import org.onosproject.core.ApplicationId;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Versioned;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Extension of DefaultAsyncConsistentMap that provides a weaker read consistency
 * guarantee in return for better read performance.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class AsyncCachingConsistentMap<K, V> extends DefaultAsyncConsistentMap<K, V> {

    private final LoadingCache<K, CompletableFuture<Versioned<V>>> cache =
            CacheBuilder.newBuilder()
                        .maximumSize(10000) // TODO: make configurable
                        .build(new CacheLoader<K, CompletableFuture<Versioned<V>>>() {
                            @Override
                            public CompletableFuture<Versioned<V>> load(K key)
                                    throws Exception {
                                return AsyncCachingConsistentMap.super.get(key);
                            }
                        });

    public AsyncCachingConsistentMap(String name,
            ApplicationId applicationId,
            Database database,
            Serializer serializer,
            boolean readOnly,
            boolean purgeOnUninstall,
            boolean meteringEnabled) {
        super(name, applicationId, database, serializer, readOnly, purgeOnUninstall, meteringEnabled);
        addListener(event -> cache.invalidate(event.key()));
    }

    @Override
    public CompletableFuture<Versioned<V>> get(K key) {
        CompletableFuture<Versioned<V>> cachedValue = cache.getIfPresent(key);
        if (cachedValue != null) {
            if (cachedValue.isCompletedExceptionally()) {
                cache.invalidate(key);
            } else {
                return cachedValue;
            }
        }
        return cache.getUnchecked(key);
    }
}