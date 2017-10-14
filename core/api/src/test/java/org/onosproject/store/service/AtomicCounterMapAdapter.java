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

/**
 * Testing adapter for the atomic counter map.
 */
public class AtomicCounterMapAdapter<K> implements AtomicCounterMap<K> {

    @Override
    public long incrementAndGet(K key) {
        return 0;
    }

    @Override
    public long decrementAndGet(K key) {
        return 0;
    }

    @Override
    public long getAndIncrement(K key) {
        return 0;
    }

    @Override
    public long getAndDecrement(K key) {
        return 0;
    }

    @Override
    public long addAndGet(K key, long delta) {
        return 0;
    }

    @Override
    public long getAndAdd(K key, long delta) {
        return 0;
    }

    @Override
    public long get(K key) {
        return 0;
    }

    @Override
    public long put(K key, long newValue) {
        return 0;
    }

    @Override
    public long putIfAbsent(K key, long newValue) {
        return 0;
    }

    @Override
    public boolean replace(K key, long expectedOldValue, long newValue) {
        return false;
    }

    @Override
    public long remove(K key) {
        return 0;
    }

    @Override
    public boolean remove(K key, long value) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public String name() {
        return null;
    }
}
