package org.onlab.onos.store.device.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Optional;
import com.google.common.cache.ForwardingLoadingCache.SimpleForwardingLoadingCache;
import com.google.common.cache.LoadingCache;

public class AbsentInvalidatingLoadingCache<K, V> extends
        SimpleForwardingLoadingCache<K, Optional<V>> {

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

    // TODO should we be also checking getAll, etc.
}
