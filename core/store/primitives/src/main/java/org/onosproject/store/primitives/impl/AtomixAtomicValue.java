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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Maps;
import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AtomicValueEvent;
import org.onosproject.store.service.AtomicValueEventListener;

/**
 * Atomix atomic value.
 */
public class AtomixAtomicValue<V> implements AsyncAtomicValue<V> {
    private final io.atomix.core.value.AsyncAtomicValue<V> atomixValue;
    private final Map<AtomicValueEventListener<V>, io.atomix.core.value.AtomicValueEventListener<V>> listenerMap =
        Maps.newIdentityHashMap();

    public AtomixAtomicValue(io.atomix.core.value.AsyncAtomicValue<V> atomixValue) {
        this.atomixValue = atomixValue;
    }

    @Override
    public String name() {
        return atomixValue.name();
    }

    @Override
    public CompletableFuture<Boolean> compareAndSet(V expect, V update) {
        return atomixValue.compareAndSet(expect, update);
    }

    @Override
    public CompletableFuture<V> get() {
        return atomixValue.get();
    }

    @Override
    public CompletableFuture<V> getAndSet(V value) {
        return atomixValue.getAndSet(value);
    }

    @Override
    public CompletableFuture<Void> set(V value) {
        return atomixValue.set(value);
    }

    @Override
    public synchronized CompletableFuture<Void> addListener(AtomicValueEventListener<V> listener) {
        io.atomix.core.value.AtomicValueEventListener<V> atomixListener = event ->
            listener.event(new AtomicValueEvent<V>(
                name(),
                event.newValue(),
                event.oldValue()));
        listenerMap.put(listener, atomixListener);
        return atomixValue.addListener(atomixListener);
    }

    @Override
    public synchronized CompletableFuture<Void> removeListener(AtomicValueEventListener<V> listener) {
        io.atomix.core.value.AtomicValueEventListener<V> atomixListener = listenerMap.remove(listener);
        if (atomixListener != null) {
            return atomixValue.removeListener(atomixListener);
        }
        return CompletableFuture.completedFuture(null);
    }
}
