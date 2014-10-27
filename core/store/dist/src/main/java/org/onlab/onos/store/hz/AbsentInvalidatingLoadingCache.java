package org.onlab.onos.store.hz;

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

    // TODO should we be also checking getAll, etc.
}
