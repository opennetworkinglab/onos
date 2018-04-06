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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.Maps;
import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AtomicValueEvent;
import org.onosproject.store.service.AtomicValueEventListener;

/**
 * An {@code AsyncAtomicValue} that transcodes values.
 */
public class TranscodingAsyncAtomicValue<V1, V2> implements AsyncAtomicValue<V1> {
    private final AsyncAtomicValue<V2> backingValue;
    private final Function<V1, V2> valueEncoder;
    private final Function<V2, V1> valueDecoder;
    private final Map<AtomicValueEventListener<V1>, InternalValueEventListener> listeners = Maps.newIdentityHashMap();

    public TranscodingAsyncAtomicValue(
        AsyncAtomicValue<V2> backingValue, Function<V1, V2> valueEncoder, Function<V2, V1> valueDecoder) {
        this.backingValue = backingValue;
        this.valueEncoder = k -> k == null ? null : valueEncoder.apply(k);
        this.valueDecoder = k -> k == null ? null : valueDecoder.apply(k);
    }

    @Override
    public String name() {
        return backingValue.name();
    }

    @Override
    public CompletableFuture<Boolean> compareAndSet(V1 expect, V1 update) {
        return backingValue.compareAndSet(valueEncoder.apply(expect), valueEncoder.apply(update));
    }

    @Override
    public CompletableFuture<V1> get() {
        return backingValue.get().thenApply(valueDecoder);
    }

    @Override
    public CompletableFuture<V1> getAndSet(V1 value) {
        return backingValue.getAndSet(valueEncoder.apply(value)).thenApply(valueDecoder);
    }

    @Override
    public CompletableFuture<Void> set(V1 value) {
        return backingValue.set(valueEncoder.apply(value));
    }

    @Override
    public CompletableFuture<Void> addListener(AtomicValueEventListener<V1> listener) {
        synchronized (listeners) {
            InternalValueEventListener backingMapListener =
                listeners.computeIfAbsent(listener, k -> new InternalValueEventListener(listener));
            return backingValue.addListener(backingMapListener);
        }
    }

    @Override
    public CompletableFuture<Void> removeListener(AtomicValueEventListener<V1> listener) {
        synchronized (listeners) {
            InternalValueEventListener backingMapListener = listeners.remove(listener);
            if (backingMapListener != null) {
                return backingValue.removeListener(backingMapListener);
            } else {
                return CompletableFuture.completedFuture(null);
            }
        }
    }

    @Override
    public void addStatusChangeListener(Consumer<Status> listener) {
        backingValue.addStatusChangeListener(listener);
    }

    @Override
    public void removeStatusChangeListener(Consumer<Status> listener) {
        backingValue.removeStatusChangeListener(listener);
    }

    @Override
    public Collection<Consumer<Status>> statusChangeListeners() {
        return backingValue.statusChangeListeners();
    }

    private class InternalValueEventListener implements AtomicValueEventListener<V2> {
        private final AtomicValueEventListener<V1> listener;

        InternalValueEventListener(AtomicValueEventListener<V1> listener) {
            this.listener = listener;
        }

        @Override
        public void event(AtomicValueEvent<V2> event) {
            listener.event(new AtomicValueEvent<>(
                event.name(),
                event.newValue() != null ? valueDecoder.apply(event.newValue()) : null,
                event.oldValue() != null ? valueDecoder.apply(event.oldValue()) : null));
        }
    }

}
