/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.store.service;

import com.google.common.util.concurrent.AtomicLongMap;

/**
 * Test implementation of the atomic counter map.
 */
public final class TestAtomicCounterMap<K> extends AtomicCounterMapAdapter<K> {

    // Map name
    private final String atomicCounterMapName;
    // Atomic long map from guava
    private AtomicLongMap<K> map;

    private TestAtomicCounterMap(String name) {
        // Init name, map using create
        atomicCounterMapName = name;
        map = AtomicLongMap.create();
    }

    @Override
    public long incrementAndGet(K key) {
        // Forward directly to the guava map
        return map.incrementAndGet(key);
    }

    @Override
    public long decrementAndGet(K key) {
        // Forward directly to the guava map
        return map.decrementAndGet(key);
    }

    @Override
    public long getAndIncrement(K key) {
        // Forward directly to the guava map
        return map.getAndIncrement(key);
    }

    @Override
    public long getAndDecrement(K key) {
        // Forward directly to the guava map
        return map.getAndDecrement(key);
    }

    @Override
    public long addAndGet(K key, long delta) {
        // Forward directly to the guava map
        return map.addAndGet(key, delta);
    }

    @Override
    public long getAndAdd(K key, long delta) {
        // Forward directly to the guava map
        return map.getAndAdd(key, delta);
    }

    @Override
    public long get(K key) {
        // Forward directly to the guava map
        return map.get(key);
    }

    @Override
    public long put(K key, long newValue) {
        // Forward directly to the guava map
        return map.put(key, newValue);
    }

    @Override
    // Coarse implementation, should we take the lock ?
    public long putIfAbsent(K key, long newValue) {
        // If it does not exist
        if (!map.containsKey(key)) {
            // Just do a put
            return map.put(key, newValue);
        } else {
            // Let's return the existing value
            return map.get(key);
        }
    }

    @Override
    // Coarse implementation, should we take the lock ?
    public boolean replace(K key, long expectedOldValue, long newValue) {
        // If the value exists and it the expected one
        if (map.containsKey(key) && map.get(key) == expectedOldValue) {
            // Let's put the value
            map.put(key, newValue);
            // Done, return true
            return true;

        } else if (!map.containsKey(key) && expectedOldValue == 0) {
            // If the value does not exist, and old value is 0
            map.put(key, newValue);
            // Done, return true
            return true;
        } else {
            // replace is not possible, just return false
            return false;
        }
    }

    @Override
    public long remove(K key) {
        // Forward directly to the guava map
        return map.remove(key);
    }

    @Override
    // Coarse implementation, should we take the lock ?
    public boolean remove(K key, long value) {
        // If the value exists and it is equal to value
        if (map.containsKey(key) && map.get(key) == value) {
            // Let's remove the value
            map.remove(key);
            // Done, return true
            return true;
        } else {
            // remove is not possible, just return false
            return false;
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public String name() {
        return atomicCounterMapName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder<K> extends AtomicCounterMapBuilder<K> {

        @Override
        public AtomicCounterMap<K> build() {
            return new TestAtomicCounterMap<>(name());
        }

        @Override
        public AsyncAtomicCounterMap<K> buildAsyncMap() {
            return null;
        }
    }

}
