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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AtomicValueEventListener;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.Topic;

import com.google.common.collect.Maps;

/**
 * Default implementation of {@link Topic}.
 *
 * @param <T> topic message type.
 */
public class DefaultDistributedTopic<T> implements Topic<T> {

    private final AsyncAtomicValue<T> atomicValue;
    private final Map<Consumer<T>, AtomicValueEventListener<T>> callbacks = Maps.newIdentityHashMap();

    DefaultDistributedTopic(AsyncAtomicValue<T> atomicValue) {
        this.atomicValue = atomicValue;
    }

    @Override
    public String name() {
        return atomicValue.name();
    }

    @Override
    public Type primitiveType() {
        return DistributedPrimitive.Type.TOPIC;
    }

    @Override
    public CompletableFuture<Void> destroy() {
        return atomicValue.destroy();
    }

    @Override
    public CompletableFuture<Void> publish(T message) {
        return atomicValue.set(message);
    }

    @Override
    public CompletableFuture<Void> subscribe(Consumer<T> callback, Executor executor) {
        AtomicValueEventListener<T> valueListener =
                event -> executor.execute(() -> callback.accept(event.newValue()));
        if (callbacks.putIfAbsent(callback, valueListener) == null) {
            return atomicValue.addListener(valueListener);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> unsubscribe(Consumer<T> callback) {
        AtomicValueEventListener<T> valueListener = callbacks.remove(callback);
        if (valueListener != null) {
            return atomicValue.removeListener(valueListener);
        }
        return CompletableFuture.completedFuture(null);
    }
}
