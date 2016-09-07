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

package org.onosproject.store.primitives.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.AtomicValueEvent;
import org.onosproject.store.service.AtomicValueEventListener;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Versioned;
import org.onosproject.utils.MeteringAgent;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

/**
 * Default implementation of a {@code AsyncAtomicValue}.
 *
 * @param <V> value type
 */
public class DefaultAsyncAtomicValue<V> implements AsyncAtomicValue<V> {

    private final String name;
    private final Serializer serializer;
    private final AsyncConsistentMap<String, byte[]> backingMap;
    private final Map<AtomicValueEventListener<V>, MapEventListener<String, byte[]>> listeners =
            Maps.newIdentityHashMap();
    private final MeteringAgent monitor;

    private static final String COMPONENT_NAME = "atomicValue";
    private static final String GET = "get";
    private static final String GET_AND_SET = "getAndSet";
    private static final String SET = "set";
    private static final String COMPARE_AND_SET = "compareAndSet";
    private static final String ADD_LISTENER = "addListener";
    private static final String REMOVE_LISTENER = "removeListener";
    private static final String NOTIFY_LISTENER = "notifyListener";
    private static final String DESTROY = "destroy";

    public DefaultAsyncAtomicValue(String name, Serializer serializer, AsyncConsistentMap<String, byte[]> backingMap) {
        this.name = checkNotNull(name, "name must not be null");
        this.serializer = checkNotNull(serializer, "serializer must not be null");
        this.backingMap = checkNotNull(backingMap, "backingMap must not be null");
        this.monitor = new MeteringAgent(COMPONENT_NAME, name, true);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CompletableFuture<Void> destroy() {
        final MeteringAgent.Context newTimer = monitor.startTimer(DESTROY);
        return backingMap.remove(name)
                         .whenComplete((r, e) -> newTimer.stop(e))
                         .thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Boolean> compareAndSet(V expect, V update) {
        final MeteringAgent.Context newTimer = monitor.startTimer(COMPARE_AND_SET);
        return backingMap.replace(name, serializer.encode(expect), serializer.encode(update))
                         .whenComplete((r, e) -> newTimer.stop(e));
    }

    @Override
    public CompletableFuture<V> get() {
        final MeteringAgent.Context newTimer = monitor.startTimer(GET);
        return backingMap.get(name)
                         .thenApply(Versioned::valueOrNull)
                         .thenApply(v -> v == null ? null : serializer.<V>decode(v))
                         .whenComplete((r, e) -> newTimer.stop(e));
    }

    @Override
    public CompletableFuture<V> getAndSet(V value) {
        final MeteringAgent.Context newTimer = monitor.startTimer(GET_AND_SET);
        if (value == null) {
            return backingMap.remove(name)
                             .thenApply(Versioned::valueOrNull)
                             .thenApply(v -> v == null ? null : serializer.<V>decode(v))
                             .whenComplete((r, e) -> newTimer.stop(e));
        }
        return backingMap.put(name, serializer.encode(value))
                         .thenApply(Versioned::valueOrNull)
                         .thenApply(v -> v == null ? null : serializer.<V>decode(v))
                         .whenComplete((r, e) -> newTimer.stop(e));
    }

    @Override
    public CompletableFuture<Void> set(V value) {
        final MeteringAgent.Context newTimer = monitor.startTimer(SET);
        if (value == null) {
            return backingMap.remove(name)
                             .whenComplete((r, e) -> newTimer.stop(e))
                             .thenApply(v -> null);

        }
        return backingMap.put(name, serializer.encode(value))
                         .whenComplete((r, e) -> newTimer.stop(e))
                         .thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Void> addListener(AtomicValueEventListener<V> listener) {
        checkNotNull(listener, "listener must not be null");
        final MeteringAgent.Context newTimer = monitor.startTimer(ADD_LISTENER);
        MapEventListener<String, byte[]> mapListener =
                listeners.computeIfAbsent(listener, key -> new InternalMapValueEventListener(listener));
        return backingMap.addListener(mapListener).whenComplete((r, e) -> newTimer.stop(e));
    }

    @Override
    public CompletableFuture<Void> removeListener(AtomicValueEventListener<V> listener) {
        checkNotNull(listener, "listener must not be null");
        final MeteringAgent.Context newTimer = monitor.startTimer(REMOVE_LISTENER);
        MapEventListener<String, byte[]> mapListener = listeners.remove(listener);
        if (mapListener != null) {
            return backingMap.removeListener(mapListener)
                             .whenComplete((r, e) -> newTimer.stop(e));
        } else {
            newTimer.stop(null);
            return CompletableFuture.completedFuture(null);
        }
    }

    private class InternalMapValueEventListener implements MapEventListener<String, byte[]> {

        private final AtomicValueEventListener<V> listener;

        InternalMapValueEventListener(AtomicValueEventListener<V> listener) {
            this.listener = listener;
        }

        @Override
        public void event(MapEvent<String, byte[]> event) {
            if (event.key().equals(name)) {
                final MeteringAgent.Context newTimer = monitor.startTimer(NOTIFY_LISTENER);
                byte[] rawNewValue = Versioned.valueOrNull(event.newValue());
                byte[] rawOldValue = Versioned.valueOrNull(event.oldValue());
                try {
                    listener.event(new AtomicValueEvent<>(name,
                            rawNewValue == null ? null : serializer.decode(rawNewValue),
                                    rawOldValue == null ? null : serializer.decode(rawOldValue)));
                    newTimer.stop(null);
                } catch (Exception e) {
                    newTimer.stop(e);
                    Throwables.propagate(e);
                }
            }
        }
    }
}