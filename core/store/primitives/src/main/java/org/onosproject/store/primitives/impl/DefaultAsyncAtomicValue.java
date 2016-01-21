/*
 * Copyright 2016 Open Networking Laboratory
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

import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.AtomicValueEvent;
import org.onosproject.store.service.AtomicValueEventListener;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Versioned;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Default implementation of {@link AsyncAtomicValue}.
 *
 * @param <V> value type
 */
public class DefaultAsyncAtomicValue<V> implements AsyncAtomicValue<V> {

    private final Set<AtomicValueEventListener<V>> listeners = new CopyOnWriteArraySet<>();
    private final AsyncConsistentMap<String, V> valueMap;
    private final String name;
    private final MapEventListener<String, V> mapEventListener = new InternalMapEventListener();
    private final MeteringAgent monitor;

    private static final String COMPONENT_NAME = "atomicValue";
    private static final String GET = "get";
    private static final String GET_AND_SET = "getAndSet";
    private static final String SET = "set";
    private static final String COMPARE_AND_SET = "compareAndSet";

    public DefaultAsyncAtomicValue(AsyncConsistentMap<String, V> valueMap,
                              String name,
                              boolean meteringEnabled) {
        this.valueMap = valueMap;
        this.name = name;
        this.monitor = new MeteringAgent(COMPONENT_NAME, name, meteringEnabled);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CompletableFuture<Boolean> compareAndSet(V expect, V update) {
        final MeteringAgent.Context newTimer = monitor.startTimer(COMPARE_AND_SET);
        CompletableFuture<Boolean> response;
        if (expect == null) {
            if (update == null) {
                response = CompletableFuture.completedFuture(true);
            }
            response = valueMap.putIfAbsent(name, update).thenApply(v -> v == null);
        } else {
             response = update == null
                         ? valueMap.remove(name, expect)
                         : valueMap.replace(name, expect, update);
        }
        return response.whenComplete((r, e) -> newTimer.stop(null));
    }

    @Override
    public CompletableFuture<V> get() {
        final MeteringAgent.Context newTimer = monitor.startTimer(GET);
        return valueMap.get(name)
                .thenApply(Versioned::valueOrNull)
                .whenComplete((r, e) -> newTimer.stop(null));
    }

    @Override
    public CompletableFuture<V> getAndSet(V value) {
        final MeteringAgent.Context newTimer = monitor.startTimer(GET_AND_SET);
        CompletableFuture<Versioned<V>> previousValue = value == null ?
                valueMap.remove(name) : valueMap.put(name, value);
        return previousValue.thenApply(Versioned::valueOrNull)
                            .whenComplete((r, e) -> newTimer.stop(null));
    }

    @Override
    public CompletableFuture<Void> set(V value) {
        final MeteringAgent.Context newTimer = monitor.startTimer(SET);
        CompletableFuture<Void> previousValue = value == null ?
                valueMap.remove(name).thenApply(v -> null) : valueMap.put(name, value).thenApply(v -> null);
        return previousValue.whenComplete((r, e) -> newTimer.stop(null));
    }

    @Override
    public CompletableFuture<Void> addListener(AtomicValueEventListener<V> listener) {
        synchronized (listeners) {
            if (listeners.add(listener)) {
                if (listeners.size() == 1) {
                    return valueMap.addListener(mapEventListener);
                }
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> removeListener(AtomicValueEventListener<V> listener) {
        synchronized (listeners) {
            if (listeners.remove(listener)) {
                if (listeners.size() == 0) {
                    return valueMap.removeListener(mapEventListener);
                }
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private class InternalMapEventListener implements MapEventListener<String, V> {

        @Override
        public void event(MapEvent<String, V> mapEvent) {
            V newValue = mapEvent.type() == MapEvent.Type.REMOVE ? null : mapEvent.value().value();
            AtomicValueEvent<V> atomicValueEvent = new AtomicValueEvent<>(name, AtomicValueEvent.Type.UPDATE, newValue);
            listeners.forEach(l -> l.event(atomicValueEvent));
        }
    }
}
