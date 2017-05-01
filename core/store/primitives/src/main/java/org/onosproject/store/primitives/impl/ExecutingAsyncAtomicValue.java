/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.google.common.collect.Maps;
import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AtomicValueEventListener;

/**
 * {@link AsyncAtomicValue} that executes asynchronous callbacks on a user provided
 * {@link Executor}.
 */
public class ExecutingAsyncAtomicValue<V> extends ExecutingDistributedPrimitive implements AsyncAtomicValue<V> {
    private final AsyncAtomicValue<V> delegateValue;
    private final Executor orderedExecutor;
    private final Map<AtomicValueEventListener<V>, AtomicValueEventListener<V>> listenerMap = Maps.newConcurrentMap();

    public ExecutingAsyncAtomicValue(
            AsyncAtomicValue<V> delegateValue, Executor orderedExecutor, Executor threadPoolExecutor) {
        super(delegateValue, orderedExecutor, threadPoolExecutor);
        this.delegateValue = delegateValue;
        this.orderedExecutor = orderedExecutor;
    }

    @Override
    public CompletableFuture<Boolean> compareAndSet(V expect, V update) {
        return asyncFuture(delegateValue.compareAndSet(expect, update));
    }

    @Override
    public CompletableFuture<V> get() {
        return asyncFuture(delegateValue.get());
    }

    @Override
    public CompletableFuture<V> getAndSet(V value) {
        return asyncFuture(delegateValue.getAndSet(value));
    }

    @Override
    public CompletableFuture<Void> set(V value) {
        return asyncFuture(delegateValue.set(value));
    }

    @Override
    public CompletableFuture<Void> addListener(AtomicValueEventListener<V> listener) {
        AtomicValueEventListener<V> wrappedListener = e -> orderedExecutor.execute(() -> listener.event(e));
        listenerMap.put(listener, wrappedListener);
        return asyncFuture(delegateValue.addListener(wrappedListener));
    }

    @Override
    public CompletableFuture<Void> removeListener(AtomicValueEventListener<V> listener) {
        AtomicValueEventListener<V> wrappedListener = listenerMap.remove(listener);
        if (wrappedListener != null) {
            return asyncFuture(delegateValue.removeListener(wrappedListener));
        }
        return CompletableFuture.completedFuture(null);
    }
}
