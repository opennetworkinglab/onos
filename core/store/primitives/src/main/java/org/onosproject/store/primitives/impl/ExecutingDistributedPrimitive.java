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
import java.util.function.Consumer;

import com.google.common.collect.Maps;
import org.onlab.util.Tools;
import org.onosproject.store.service.DistributedPrimitive;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for primitives that delegate asynchronous callbacks to a user provided {@link Executor}.
 */
public abstract class ExecutingDistributedPrimitive
        extends DelegatingDistributedPrimitive {
    private final DistributedPrimitive primitive;
    private final Executor orderedExecutor;
    private final Executor threadPoolExecutor;
    private final Map<Consumer<Status>, Consumer<Status>> listenerMap = Maps.newConcurrentMap();

    protected ExecutingDistributedPrimitive(
            DistributedPrimitive primitive, Executor orderedExecutor, Executor threadPoolExecutor) {
        super(primitive);
        this.primitive = primitive;
        this.orderedExecutor = checkNotNull(orderedExecutor);
        this.threadPoolExecutor = checkNotNull(threadPoolExecutor);
    }

    /**
     * Creates a future to be completed asynchronously on the provided ordered and thread pool executors.
     *
     * @param future the future to be completed asynchronously
     * @param <T> future result type
     * @return a new {@link CompletableFuture} to be completed asynchronously using the primitive thread model
     */
    protected <T> CompletableFuture<T> asyncFuture(CompletableFuture<T> future) {
        return Tools.orderedFuture(future, orderedExecutor, threadPoolExecutor);
    }

    @Override
    public CompletableFuture<Void> destroy() {
        return asyncFuture(primitive.destroy());
    }

    @Override
    public void addStatusChangeListener(Consumer<DistributedPrimitive.Status> listener) {
        Consumer<DistributedPrimitive.Status> wrappedListener =
                status -> orderedExecutor.execute(() -> listener.accept(status));
        listenerMap.put(listener, wrappedListener);
        primitive.addStatusChangeListener(wrappedListener);
    }

    @Override
    public void removeStatusChangeListener(Consumer<DistributedPrimitive.Status> listener) {
        Consumer<DistributedPrimitive.Status> wrappedListener = listenerMap.remove(listener);
        if (wrappedListener != null) {
            primitive.removeStatusChangeListener(wrappedListener);
        }
    }
}
