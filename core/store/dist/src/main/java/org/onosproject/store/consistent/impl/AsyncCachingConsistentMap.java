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

import java.util.concurrent.CompletableFuture;

import org.onosproject.core.ApplicationId;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Versioned;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Extension of {@link DefaultAsyncConsistentMap} that provides a weaker read consistency
 * guarantee in return for better read performance.
 * <p>
 * For read/write operations that are local to a node this map implementation provides
 * guarantees similar to a ConsistentMap. However for read/write operations executed
 * across multiple nodes this implementation only provides eventual consistency.
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

    @Override
    protected void beforeUpdate(K key) {
        super.beforeUpdate(key);
        cache.invalidate(key);
    }
}