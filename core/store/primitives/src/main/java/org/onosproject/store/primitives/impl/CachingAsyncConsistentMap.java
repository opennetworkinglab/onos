/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.store.primitives.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Versioned;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * {@code AsyncConsistentMap} that caches entries on read.
 * <p>
 * The cache entries are automatically invalidated when updates are detected either locally or
 * remotely.
 * <p> This implementation only attempts to serve cached entries for {@link AsyncConsistentMap#get get}
 * calls. All other calls skip the cache and directly go the backing map.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class CachingAsyncConsistentMap<K, V> extends DelegatingAsyncConsistentMap<K, V> {

    private final LoadingCache<K, CompletableFuture<Versioned<V>>> cache =
            CacheBuilder.newBuilder()
                        .maximumSize(10000) // TODO: make configurable
                        .build(new CacheLoader<K, CompletableFuture<Versioned<V>>>() {
                            @Override
                            public CompletableFuture<Versioned<V>> load(K key)
                                    throws Exception {
                                return CachingAsyncConsistentMap.super.get(key);
                            }
                        });

    private final MapEventListener<K, V> cacheInvalidator = event -> cache.invalidate(event.key());

    public CachingAsyncConsistentMap(AsyncConsistentMap<K, V> backingMap) {
        super(backingMap);
        super.addListener(cacheInvalidator);
    }

    @Override
    public CompletableFuture<Void> destroy() {
        return super.destroy().thenCompose(v -> removeListener(cacheInvalidator));
    }

    @Override
    public CompletableFuture<Versioned<V>> get(K key) {
        return cache.getUnchecked(key);
    }

    @Override
    public CompletableFuture<Versioned<V>> computeIf(K key,
            Predicate<? super V> condition,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return super.computeIf(key, condition, remappingFunction)
                    .whenComplete((r, e) -> cache.invalidate(key));
    }

    @Override
    public CompletableFuture<Versioned<V>> put(K key, V value) {
        return super.put(key, value)
                    .whenComplete((r, e) -> cache.invalidate(key));
    }

    @Override
    public CompletableFuture<Versioned<V>> putAndGet(K key, V value) {
        return super.putAndGet(key, value)
                    .whenComplete((r, e) -> cache.invalidate(key));
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(K key) {
        return super.remove(key)
                    .whenComplete((r, e) -> cache.invalidate(key));
    }

    @Override
    public CompletableFuture<Void> clear() {
        return super.clear()
                    .whenComplete((r, e) -> cache.invalidateAll());
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        return super.remove(key, value)
                    .whenComplete((r, e) -> {
                        if (r) {
                            cache.invalidate(key);
                        }
                    });
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long version) {
        return super.remove(key, version)
                .whenComplete((r, e) -> {
                    if (r) {
                        cache.invalidate(key);
                    }
                });
    }

    @Override
    public CompletableFuture<Versioned<V>> replace(K key, V value) {
        return super.replace(key, value)
                    .whenComplete((r, e) -> cache.invalidate(key));
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, V oldValue, V newValue) {
        return super.replace(key, oldValue, newValue)
                .whenComplete((r, e) -> {
                    if (r) {
                        cache.invalidate(key);
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long oldVersion, V newValue) {
        return super.replace(key, oldVersion, newValue)
                .whenComplete((r, e) -> {
                    if (r) {
                        cache.invalidate(key);
                    }
                });
    }
}
