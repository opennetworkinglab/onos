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
package org.onosproject.store.primitives.resources.impl;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.onlab.util.Tools;
import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AtomicValueEvent;
import org.onosproject.store.service.AtomicValueEventListener;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Versioned;

import com.google.common.collect.Sets;

/**
 * Implementation of {@link AsyncAtomicValue} backed by {@link AtomixConsistentMap}.
 */
public class AtomixValue implements AsyncAtomicValue<String> {

    private final String name;
    private final AtomixConsistentMap atomixMap;
    private MapEventListener<String, byte[]> mapEventListener;
    private final Set<AtomicValueEventListener<String>> listeners = Sets.newIdentityHashSet();

    AtomixValue(String name, AtomixConsistentMap atomixMap) {
        this.name = name;
        this.atomixMap = atomixMap;
    }

    @Override
    public CompletableFuture<Boolean> compareAndSet(String expect, String update) {
        return atomixMap.replace(name, Tools.getBytesUtf8(expect), Tools.getBytesUtf8(update));
    }

    @Override
    public CompletableFuture<String> get() {
        return atomixMap.get(name)
                        .thenApply(v -> v != null ? Tools.toStringUtf8(v.value()) : null);
    }

    @Override
    public CompletableFuture<String> getAndSet(String value) {
        return atomixMap.put(name, Tools.getBytesUtf8(value))
                        .thenApply(v -> v != null ? Tools.toStringUtf8(v.value()) : null);
    }

    @Override
    public CompletableFuture<Void> set(String value) {
        return getAndSet(value).thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Void> addListener(AtomicValueEventListener<String> listener) {
        // TODO: synchronization
        if (mapEventListener == null) {
            mapEventListener = event -> {
                Versioned<byte[]> newValue = event.newValue();
                Versioned<byte[]> oldValue = event.oldValue();
                if (Objects.equals(event.key(), name)) {
                    listener.event(new AtomicValueEvent<>(name,
                            newValue == null ? null : Tools.toStringUtf8(newValue.value()),
                            oldValue == null ? null : Tools.toStringUtf8(oldValue.value())));
                }
            };
            return atomixMap.addListener(mapEventListener).whenComplete((r, e) -> {
                if (e == null) {
                    listeners.add(listener);
                } else {
                    mapEventListener = null;
                }
            });
        } else {
            listeners.add(listener);
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public CompletableFuture<Void> removeListener(AtomicValueEventListener<String> listener) {
        // TODO: synchronization
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            return atomixMap.removeListener(mapEventListener);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public String name() {
        return null;
    }
}