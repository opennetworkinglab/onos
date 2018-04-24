/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.store.primitives;

import org.onosproject.store.service.AsyncAtomicCounterMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AsyncAtomicCounterMapAdapter<K> implements AsyncAtomicCounterMap<K> {
    private Map<K, Long> map = new HashMap<>();

    @Override
    public CompletableFuture<Long> incrementAndGet(K key) {
        Long value = map.getOrDefault(key, 0L) + 1;
        map.put(key, value);
        return CompletableFuture.completedFuture(value);

    }

    @Override
    public CompletableFuture<Long> decrementAndGet(K key) {
        Long value = map.getOrDefault(key, 0L) - 1;
        map.put(key, value);
        return CompletableFuture.completedFuture(value);

    }

    @Override
    public CompletableFuture<Long> getAndIncrement(K key) {
        Long value = map.getOrDefault(key, 0L);
        map.put(key, value + 1);
        return CompletableFuture.completedFuture(value);

    }

    @Override
    public CompletableFuture<Long> getAndDecrement(K key) {
        Long value = map.getOrDefault(key, 0L);
        map.put(key, value - 1);
        return CompletableFuture.completedFuture(value);
    }

    @Override
    public CompletableFuture<Long> addAndGet(K key, long delta) {
        Long value = map.getOrDefault(key, 0L) + delta;
        map.put(key, value);
        return CompletableFuture.completedFuture(value);
    }

    @Override
    public CompletableFuture<Long> getAndAdd(K key, long delta) {
        Long value = map.getOrDefault(key, 0L);
        map.put(key, value + delta);
        return CompletableFuture.completedFuture(value);
    }

    @Override
    public CompletableFuture<Long> get(K key) {
        Long value = map.getOrDefault(key, 0L);
        return CompletableFuture.completedFuture(value);
    }

    @Override
    public CompletableFuture<Long> put(K key, long newValue) {
        Long value = map.getOrDefault(key, 0L);
        map.put(key, newValue);
        return CompletableFuture.completedFuture(value);
    }

    @Override
    public CompletableFuture<Long> putIfAbsent(K key, long newValue) {
        Long value = map.putIfAbsent(key, newValue);
        if (value == null) {
            value = 0L;
        }
        return CompletableFuture.completedFuture(value);
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long expectedOldValue, long newValue) {
        boolean value = map.replace(key, expectedOldValue, newValue);
        return CompletableFuture.completedFuture(value);
    }

    @Override
    public CompletableFuture<Long> remove(K key) {
        Long value = map.remove(key);
        return CompletableFuture.completedFuture(value);
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long value) {
        boolean result = map.remove(key, value);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<Integer> size() {
        int value = map.size();
        return CompletableFuture.completedFuture(value);
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        boolean value = map.isEmpty();
        return CompletableFuture.completedFuture(value);
    }



    @Override
    public CompletableFuture<Void> clear() {
        map.clear();
        return CompletableFuture.completedFuture(null);

    }

    @Override
    public String name() {
        return null;
    }
}
