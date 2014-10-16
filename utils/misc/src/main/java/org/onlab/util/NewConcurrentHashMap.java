package org.onlab.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.concurrent.ConcurrentInitializer;

/**
 * Creates an instance of new ConcurrentHashMap on each {@link #get()} call.
 * <p>
 * To be used with
 * {@link org.apache.commons.lang3.concurrent.ConcurrentUtils#createIfAbsent()
 *  ConcurrentUtils#createIfAbsent}
 *
 * @param <K> ConcurrentHashMap key type
 * @param <V> ConcurrentHashMap value type
 */
public final class NewConcurrentHashMap<K, V>
    implements  ConcurrentInitializer<ConcurrentMap<K, V>> {

    public static final NewConcurrentHashMap<?, ?> INSTANCE = new NewConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <K, V> NewConcurrentHashMap<K, V> ifNeeded() {
        return (NewConcurrentHashMap<K, V>) INSTANCE;
    }

    @Override
    public ConcurrentMap<K, V> get() {
        return new ConcurrentHashMap<>();
    }
}
