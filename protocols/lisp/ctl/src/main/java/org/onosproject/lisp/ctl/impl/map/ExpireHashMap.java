/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.lisp.ctl.impl.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of ExpireMap.
 */
public class ExpireHashMap<K, V> implements ExpireMap<K, V> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final long DEFAULT_TTL = 60000L;
    private final ConcurrentMap<K, ExpiredObject<K, V>> map = new ConcurrentHashMap<>();
    private final Lock writeLock = new ReentrantLock();
    private final Timer timer = new Timer("ExpireMapTimer", true);

    /**
     * An expired object that associates with a TimerTask instance.
     *
     * @param <K1> key type K1
     * @param <V1> value type V1
     */
    class ExpiredObject<K1, V1> {
        private final V1 value;
        private final ExpiryTask<K1> task;
        private final long ttl;

        public ExpiredObject(K1 key, V1 value) {
            this(key, value, DEFAULT_TTL);
        }

        ExpiredObject(K1 key, V1 value, long ttl) {
            this.value = value;
            this.task = new ExpiryTask<>(key);
            this.ttl = ttl;
            timer.schedule(this.task, ttl);
        }

        ExpiryTask<K1> getTask() {
            return task;
        }

        V1 getValue() {
            return value;
        }

        long getTtl() {
            return ttl;
        }
    }

    /**
     * A TimerTask that removes its associated map entry from the internal map.
     *
     * @param <K2> object key
     */
    class ExpiryTask<K2> extends TimerTask {
        private final K2 key;

        ExpiryTask(K2 key) {
            this.key = key;
        }

        K2 getKey() {
            return key;
        }

        @Override
        public void run() {
            log.info("Removing element with key [{}]", key);
            try {
                writeLock.lock();
                if (map.containsKey(key)) {
                    map.remove(getKey());
                }
            } finally {
                writeLock.unlock();
            }
        }
    }

    @Override
    public void put(K key, V value, long expireMs) {
        try {
            writeLock.lock();

            // if we have a value which is previously associated with the given
            // key, we simply replace it with new value, and invalidate the
            // previously associated value
            final ExpiredObject<K, V> object =
                    map.putIfAbsent(key, new ExpiredObject<>(key, value, expireMs));

            if (object != null) {
                object.getTask().cancel();
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void put(K key, V value) {
        put(key, value, DEFAULT_TTL);
    }

    @Override
    public V get(K key) {
        return map.containsKey(key) ? map.get(key).getValue() : null;
    }

    @Override
    public void clear() {
        try {
            writeLock.lock();
            for (ExpiredObject<K, V> object : map.values()) {
                object.getTask().cancel();
            }
            map.clear();
            timer.purge();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        Collection<V> values = Collections.emptyList();
        map.values().forEach(v -> values.add(v.getValue()));
        return values;
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public V remove(K key) {
        final ExpiredObject<K, V> object;
        try {
            writeLock.lock();
            object = map.remove(key);
            if (object != null) {
                object.getTask().cancel();
            }
        } finally {
            writeLock.unlock();
        }
        return (object == null ? null : object.getValue());
    }

    @Override
    public int size() {
        return map.size();
    }
}
