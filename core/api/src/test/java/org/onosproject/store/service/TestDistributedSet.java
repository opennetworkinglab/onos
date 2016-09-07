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

package org.onosproject.store.service;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onosproject.store.primitives.DefaultDistributedSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Test implementation of the distributed set.
 */
public final class TestDistributedSet<E> extends DistributedSetAdapter<E> {
    private final List<SetEventListener<E>> listeners;
    private final Set<E> set;
    private final String setName;

    /**
     * Public constructor.
     *
     * @param setName name to be assigned to this set
     */
    public TestDistributedSet(String setName) {
        set = new HashSet<>();
        listeners = new LinkedList<>();
        this.setName = setName;
    }

    /**
     * Notify all listeners of a set event.
     *
     * @param event the SetEvent
     */
    private void notifyListeners(SetEvent<E> event) {
        listeners.forEach(
                listener -> listener.event(event)
        );
    }

    @Override
    public CompletableFuture<Void> addListener(SetEventListener<E> listener) {
        listeners.add(listener);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> removeListener(SetEventListener<E> listener) {
        listeners.remove(listener);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> add(E element) {
        SetEvent<E> event =
                new SetEvent<>(setName, SetEvent.Type.ADD, element);
        notifyListeners(event);
        return CompletableFuture.completedFuture(set.add(element));
    }

    @Override
    public CompletableFuture<Boolean> remove(E element) {
        SetEvent<E> event =
                new SetEvent<>(setName, SetEvent.Type.REMOVE, element);
        notifyListeners(event);
        return CompletableFuture.completedFuture(set.remove(element));
    }

    @Override
    public CompletableFuture<Integer> size() {
        return CompletableFuture.completedFuture(set.size());
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return CompletableFuture.completedFuture(set.isEmpty());
    }

    @Override
    public CompletableFuture<Void> clear() {
        removeAll(ImmutableSet.copyOf(set));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> contains(E element) {
        return CompletableFuture.completedFuture(set.contains(element));
    }

    @Override
    public CompletableFuture<Boolean> addAll(Collection<? extends E> c) {
        c.forEach(this::add);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> containsAll(Collection<? extends E> c) {
        return CompletableFuture.completedFuture(set.containsAll(c));
    }

    @Override
    public CompletableFuture<Boolean> retainAll(Collection<? extends E> c) {
        Set notInSet2;
        notInSet2 = Sets.difference(set, (Set<?>) c);
        return removeAll(ImmutableSet.copyOf(notInSet2));
    }

    @Override
    public CompletableFuture<Boolean> removeAll(Collection<? extends E> c) {
        c.forEach(this::remove);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<? extends Set<E>> getAsImmutableSet() {
        return CompletableFuture.completedFuture(ImmutableSet.copyOf(set));
    }

    @Override
    public String name() {
        return this.setName;
    }

    @Override
    public DistributedSet<E> asDistributedSet() {
        return new DefaultDistributedSet<>(this, 0);
    }

    @Override
    public DistributedSet<E> asDistributedSet(long timeoutMillis) {
        return new DefaultDistributedSet<>(this, timeoutMillis);
    }

    /**
     * Returns a new Builder instance.
     *
     * @return Builder
     **/
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder constructor that instantiates a TestDistributedSet.
     *
     * @param <E>
     */
    public static class Builder<E> extends DistributedSetBuilder<E> {
        @Override
        public AsyncDistributedSet<E> build() {
            return new TestDistributedSet(name());
        }
    }
}
