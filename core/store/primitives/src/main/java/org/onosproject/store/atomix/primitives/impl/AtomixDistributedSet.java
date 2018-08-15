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
package org.onosproject.store.atomix.primitives.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import org.onosproject.store.service.AsyncDistributedSet;
import org.onosproject.store.service.SetEvent;
import org.onosproject.store.service.SetEventListener;

import static org.onosproject.store.atomix.primitives.impl.AtomixFutures.adaptFuture;

/**
 * Atomix distributed set.
 */
public class AtomixDistributedSet<E> implements AsyncDistributedSet<E> {
    private final io.atomix.core.set.AsyncDistributedSet<E> atomixSet;
    private final Map<SetEventListener<E>, io.atomix.core.collection.CollectionEventListener<E>> listenerMap =
        Maps.newIdentityHashMap();

    public AtomixDistributedSet(io.atomix.core.set.AsyncDistributedSet<E> atomixSet) {
        this.atomixSet = atomixSet;
    }

    @Override
    public String name() {
        return atomixSet.name();
    }

    @Override
    public CompletableFuture<Integer> size() {
        return adaptFuture(atomixSet.size());
    }

    @Override
    public CompletableFuture<Boolean> add(E element) {
        return adaptFuture(atomixSet.add(element));
    }

    @Override
    public CompletableFuture<Boolean> remove(E element) {
        return adaptFuture(atomixSet.remove(element));
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return adaptFuture(atomixSet.isEmpty());
    }

    @Override
    public CompletableFuture<Void> clear() {
        return adaptFuture(atomixSet.clear());
    }

    @Override
    public CompletableFuture<Boolean> contains(E element) {
        return adaptFuture(atomixSet.contains(element));
    }

    @Override
    public CompletableFuture<Boolean> addAll(Collection<? extends E> c) {
        return adaptFuture(atomixSet.addAll(c));
    }

    @Override
    public CompletableFuture<Boolean> containsAll(Collection<? extends E> c) {
        return adaptFuture(atomixSet.containsAll(c));
    }

    @Override
    public CompletableFuture<Boolean> retainAll(Collection<? extends E> c) {
        return adaptFuture(atomixSet.retainAll(c));
    }

    @Override
    public CompletableFuture<Boolean> removeAll(Collection<? extends E> c) {
        return adaptFuture(atomixSet.removeAll(c));
    }

    @Override
    public CompletableFuture<? extends Set<E>> getAsImmutableSet() {
        return CompletableFuture.completedFuture(atomixSet.stream().collect(Collectors.toSet()));
    }

    @Override
    public synchronized CompletableFuture<Void> addListener(SetEventListener<E> listener) {
        io.atomix.core.collection.CollectionEventListener<E> atomixListener = event ->
            listener.event(new SetEvent<E>(
                name(),
                SetEvent.Type.valueOf(event.type().name()),
                event.element()));
        listenerMap.put(listener, atomixListener);
        return adaptFuture(atomixSet.addListener(atomixListener));
    }

    @Override
    public CompletableFuture<Void> removeListener(SetEventListener<E> listener) {
        io.atomix.core.collection.CollectionEventListener<E> atomixListener = listenerMap.remove(listener);
        if (atomixListener != null) {
            return adaptFuture(atomixSet.removeListener(atomixListener));
        }
        return CompletableFuture.completedFuture(null);
    }
}
