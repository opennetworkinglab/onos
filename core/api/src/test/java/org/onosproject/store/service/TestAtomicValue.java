/*
 * Copyright 2020-present Open Networking Foundation
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class TestAtomicValue<V> implements AsyncAtomicValue<V> {

    private AtomicReference<V> ref;
    private String name;

    TestAtomicValue(String name) {
        ref = new AtomicReference<>();
        this.name = name;
    }

    @Override
    public CompletableFuture<Boolean> compareAndSet(V expect, V update) {
        return CompletableFuture.completedFuture(ref.compareAndSet(expect, update));
    }

    @Override
    public CompletableFuture<V> get() {
        return CompletableFuture.completedFuture(ref.get());
    }

    @Override
    public CompletableFuture<V> getAndSet(V value) {
        return CompletableFuture.completedFuture(ref.getAndSet(value));
    }

    @Override
    public CompletableFuture<Void> set(V value) {
        ref.set(value);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> addListener(AtomicValueEventListener<V> listener) {
        // Unimplemented
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> removeListener(AtomicValueEventListener<V> listener) {
        // Unimplemented
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String name() {
        return name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AtomicValueBuilder {

        @Override
        public AsyncAtomicValue build() {
            return new TestAtomicValue<>("");
        }
    }
}
