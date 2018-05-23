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
package org.onosproject.store.primitives.impl;

import java.util.concurrent.CompletableFuture;

import org.onosproject.store.service.AsyncAtomicCounterMap;

/**
 * Atomix atomic counter map.
 */
public class AtomixAtomicCounterMap<K> implements AsyncAtomicCounterMap<K> {
    private final io.atomix.core.map.AsyncAtomicCounterMap<K> atomixMap;

    public AtomixAtomicCounterMap(io.atomix.core.map.AsyncAtomicCounterMap<K> atomixMap) {
        this.atomixMap = atomixMap;
    }

    @Override
    public String name() {
        return atomixMap.name();
    }

    @Override
    public CompletableFuture<Long> incrementAndGet(K key) {
        return atomixMap.incrementAndGet(key);
    }

    @Override
    public CompletableFuture<Long> decrementAndGet(K key) {
        return atomixMap.decrementAndGet(key);
    }

    @Override
    public CompletableFuture<Long> getAndIncrement(K key) {
        return atomixMap.getAndIncrement(key);
    }

    @Override
    public CompletableFuture<Long> getAndDecrement(K key) {
        return atomixMap.getAndDecrement(key);
    }

    @Override
    public CompletableFuture<Long> addAndGet(K key, long delta) {
        return atomixMap.addAndGet(key, delta);
    }

    @Override
    public CompletableFuture<Long> getAndAdd(K key, long delta) {
        return atomixMap.getAndAdd(key, delta);
    }

    @Override
    public CompletableFuture<Long> get(K key) {
        return atomixMap.get(key);
    }

    @Override
    public CompletableFuture<Long> put(K key, long newValue) {
        return atomixMap.put(key, newValue);
    }

    @Override
    public CompletableFuture<Long> putIfAbsent(K key, long newValue) {
        return atomixMap.putIfAbsent(key, newValue);
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long expectedOldValue, long newValue) {
        return atomixMap.replace(key, expectedOldValue, newValue);
    }

    @Override
    public CompletableFuture<Long> remove(K key) {
        return atomixMap.remove(key);
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long value) {
        return atomixMap.remove(key, value);
    }

    @Override
    public CompletableFuture<Integer> size() {
        return atomixMap.size();
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return atomixMap.isEmpty();
    }

    @Override
    public CompletableFuture<Void> clear() {
        return atomixMap.clear();
    }
}
