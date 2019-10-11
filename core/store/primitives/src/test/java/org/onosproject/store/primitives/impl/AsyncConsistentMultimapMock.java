/*
 * Copyright 2019-present Open Networking Foundation
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.onosproject.store.service.AsyncConsistentMultimapAdapter;
import org.onosproject.store.service.Versioned;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AsyncConsistentMultimapMock<K, V> extends AsyncConsistentMultimapAdapter<K, V> {
    private final Multimap<K, V> baseMap = HashMultimap.create();
    private static final int DEFAULT_CREATION_TIME = 0;
    private static final int DEFAULT_VERSION = 0;

    AsyncConsistentMultimapMock() {
    }

    Versioned<Collection<? extends V>> makeVersioned(Collection<? extends V> v) {
        return new Versioned<>(v, DEFAULT_VERSION, DEFAULT_CREATION_TIME);
    }

    @Override
    public CompletableFuture<Integer> size() {
        return CompletableFuture.completedFuture(baseMap.size());
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return CompletableFuture.completedFuture(baseMap.isEmpty());
    }

    @Override
    public CompletableFuture<Boolean> putAll(Map<K, Collection<? extends V>> mapping) {
        CompletableFuture<Boolean> result = CompletableFuture.completedFuture(false);
        for (Map.Entry<K, Collection<? extends V>> entry : mapping.entrySet()) {
            if (baseMap.putAll(entry.getKey(), entry.getValue())) {
                result = CompletableFuture.completedFuture(true);
            }
        }
        return result;
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> get(K key) {
        return CompletableFuture.completedFuture(makeVersioned(baseMap.get(key)));
    }

    @Override
    public CompletableFuture<Boolean> removeAll(Map<K, Collection<? extends V>> mapping) {
        CompletableFuture<Boolean> result = CompletableFuture.completedFuture(false);
        for (Map.Entry<K, Collection<? extends V>> entry : mapping.entrySet()) {
            for (V value : entry.getValue()) {
                if (baseMap.remove(entry.getKey(), value)) {
                    result = CompletableFuture.completedFuture(true);
                }
            }
        }
        return result;
    }
}


