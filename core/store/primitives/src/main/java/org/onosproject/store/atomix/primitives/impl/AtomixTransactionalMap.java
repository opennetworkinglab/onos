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

import org.onosproject.store.service.TransactionalMap;

/**
 * Atomix transactional map.
 */
public class AtomixTransactionalMap<K, V> implements TransactionalMap<K, V> {
    private final io.atomix.core.transaction.TransactionalMap<K, V> atomixMap;

    public AtomixTransactionalMap(io.atomix.core.transaction.TransactionalMap<K, V> atomixMap) {
        this.atomixMap = atomixMap;
    }

    @Override
    public V get(K key) {
        return atomixMap.get(key);
    }

    @Override
    public boolean containsKey(K key) {
        return atomixMap.containsKey(key);
    }

    @Override
    public V put(K key, V value) {
        return atomixMap.put(key, value);
    }

    @Override
    public V remove(K key) {
        return atomixMap.remove(key);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return atomixMap.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(K key, V value) {
        return atomixMap.remove(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return atomixMap.replace(key, oldValue, newValue);
    }
}
