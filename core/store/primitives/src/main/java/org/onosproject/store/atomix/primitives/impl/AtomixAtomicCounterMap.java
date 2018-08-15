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
package org.onosproject.store.atomix.primitives.impl;

import java.util.concurrent.CompletableFuture;

import org.onosproject.store.service.AsyncAtomicCounterMap;

import static org.onosproject.store.atomix.primitives.impl.AtomixFutures.adaptMapFuture;

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
        return adaptMapFuture(atomixMap.incrementAndGet(key));
    }

    @Override
    public CompletableFuture<Long> decrementAndGet(K key) {
        return adaptMapFuture(atomixMap.decrementAndGet(key));
    }

    @Override
    public CompletableFuture<Long> getAndIncrement(K key) {
        return adaptMapFuture(atomixMap.getAndIncrement(key));
    }

    @Override
    public CompletableFuture<Long> getAndDecrement(K key) {
        return adaptMapFuture(atomixMap.getAndDecrement(key));
    }

    @Override
    public CompletableFuture<Long> addAndGet(K key, long delta) {
        return adaptMapFuture(atomixMap.addAndGet(key, delta));
    }

    @Override
    public CompletableFuture<Long> getAndAdd(K key, long delta) {
        return adaptMapFuture(atomixMap.getAndAdd(key, delta));
    }

    @Override
    public CompletableFuture<Long> get(K key) {
        return adaptMapFuture(atomixMap.get(key));
    }

    @Override
    public CompletableFuture<Long> put(K key, long newValue) {
        return adaptMapFuture(atomixMap.put(key, newValue));
    }

    @Override
    public CompletableFuture<Long> putIfAbsent(K key, long newValue) {
        return adaptMapFuture(atomixMap.putIfAbsent(key, newValue));
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long expectedOldValue, long newValue) {
        return adaptMapFuture(atomixMap.replace(key, expectedOldValue, newValue));
    }

    @Override
    public CompletableFuture<Long> remove(K key) {
        return adaptMapFuture(atomixMap.remove(key));
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long value) {
        return adaptMapFuture(atomixMap.remove(key, value));
    }

    @Override
    public CompletableFuture<Integer> size() {
        return adaptMapFuture(atomixMap.size());
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return adaptMapFuture(atomixMap.isEmpty());
    }

    @Override
    public CompletableFuture<Void> clear() {
        return adaptMapFuture(atomixMap.clear());
    }
}
