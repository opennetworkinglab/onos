/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.store.service;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Test implementation of topic.
 */
public class TestTopic<T> implements Topic<T> {
    private final String name;
    private final Map<Consumer<T>, Executor> callbacks = Maps.newIdentityHashMap();

    public TestTopic(String name) {
        this.name = name;
    }

    @Override
    public CompletableFuture<Void> publish(T message) {
        callbacks.forEach((k, v) -> {
            v.execute(() -> {
                k.accept(message);
            });
        });
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> subscribe(Consumer<T> callback, Executor executor) {
        callbacks.put(callback, executor);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> unsubscribe(Consumer<T> callback) {
        callbacks.remove(callback);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Type primitiveType() {
        return Type.TOPIC;
    }
}
