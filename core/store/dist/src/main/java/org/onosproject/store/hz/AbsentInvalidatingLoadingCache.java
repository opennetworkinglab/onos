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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Optional;
import com.google.common.cache.ForwardingLoadingCache.SimpleForwardingLoadingCache;
import com.google.common.cache.LoadingCache;

/**
 * Wrapper around LoadingCache to handle negative hit scenario.
 * <p>
 * When the LoadingCache returned Absent,
 * this implementation will invalidate the entry immediately to avoid
 * caching negative hits.
 *
 * @param <K> Cache key type
 * @param <V> Cache value type. (Optional{@literal <V>})
 */
public class AbsentInvalidatingLoadingCache<K, V> extends
        SimpleForwardingLoadingCache<K, Optional<V>> {

    /**
     * Constructor.
     *
     * @param delegate actual {@link LoadingCache} to delegate loading.
     */
    public AbsentInvalidatingLoadingCache(LoadingCache<K, Optional<V>> delegate) {
        super(delegate);
    }

    @Override
    public Optional<V> get(K key) throws ExecutionException {
        Optional<V> v = super.get(key);
        if (!v.isPresent()) {
            invalidate(key);
        }
        return v;
    }

    @Override
    public Optional<V> getUnchecked(K key) {
        Optional<V> v = super.getUnchecked(key);
        if (!v.isPresent()) {
            invalidate(key);
        }
        return v;
    }

    @Override
    public Optional<V> apply(K key) {
        return getUnchecked(key);
    }

    @Override
    public Optional<V> getIfPresent(Object key) {
        Optional<V> v = super.getIfPresent(key);
        if (!v.isPresent()) {
            invalidate(key);
        }
        return v;
    }

    @Override
    public Optional<V> get(K key, Callable<? extends Optional<V>> valueLoader)
            throws ExecutionException {

        Optional<V> v = super.get(key, valueLoader);
        if (!v.isPresent()) {
            invalidate(key);
        }
        return v;
    }
}
