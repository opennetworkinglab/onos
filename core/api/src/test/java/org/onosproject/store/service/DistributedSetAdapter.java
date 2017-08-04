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

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Testing adapter for the distributed set.
 */
public class DistributedSetAdapter<E> implements AsyncDistributedSet<E> {
    @Override
    public CompletableFuture<Void> addListener(SetEventListener<E> listener) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removeListener(SetEventListener<E> listener) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> add(E element) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> remove(E element) {
        return null;
    }

    @Override
    public CompletableFuture<Integer> size() {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return null;
    }

    @Override
    public CompletableFuture<Void> clear() {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> contains(E element) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> addAll(Collection<? extends E> c) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> containsAll(Collection<? extends E> c) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> retainAll(Collection<? extends E> c) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> removeAll(Collection<? extends E> c) {
        return null;
    }

    @Override
    public CompletableFuture<? extends Set<E>> getAsImmutableSet() {
        return null;
    }

    @Override
    public String name() {
        return null;
    }
}
