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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.onlab.util.Tools;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.AsyncDistributedSet;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.SetEvent;
import org.onosproject.store.service.SetEventListener;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.utils.MeteringAgent;

/**
 * Implementation of {@link AsyncDistributedSet}.
 *
 * @param <E> set entry type
 */
public class DefaultAsyncDistributedSet<E> implements AsyncDistributedSet<E> {

    private static final String CONTAINS = "contains";
    private static final String PRIMITIVE_NAME = "distributedSet";
    private static final String SIZE = "size";
    private static final String IS_EMPTY = "isEmpty";
    private static final String ADD = "add";
    private static final String REMOVE = "remove";
    private static final String CONTAINS_ALL = "containsAll";
    private static final String ADD_ALL = "addAll";
    private static final String RETAIN_ALL = "retainAll";
    private static final String REMOVE_ALL = "removeAll";
    private static final String CLEAR = "clear";
    private static final String GET_AS_IMMUTABLE_SET = "getAsImmutableSet";

    private final String name;
    private final AsyncConsistentMap<E, Boolean> backingMap;
    private final Map<SetEventListener<E>, MapEventListener<E, Boolean>> listenerMapping = Maps.newIdentityHashMap();
    private final MeteringAgent monitor;

    public DefaultAsyncDistributedSet(AsyncConsistentMap<E, Boolean> backingMap, String name, boolean meteringEnabled) {
        this.backingMap = backingMap;
        this.name = name;
        monitor = new MeteringAgent(PRIMITIVE_NAME, name, meteringEnabled);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CompletableFuture<Integer> size() {
        final MeteringAgent.Context timer = monitor.startTimer(SIZE);
        return backingMap.size().whenComplete((r, e) -> timer.stop(null));
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        final MeteringAgent.Context timer = monitor.startTimer(IS_EMPTY);
        return backingMap.isEmpty().whenComplete((r, e) -> timer.stop(null));
    }

    @Override
    public CompletableFuture<Boolean> contains(E element) {
        final MeteringAgent.Context timer = monitor.startTimer(CONTAINS);
        return backingMap.containsKey(element).whenComplete((r, e) -> timer.stop(null));
    }

    @Override
    public CompletableFuture<Boolean> add(E entry) {
        final MeteringAgent.Context timer = monitor.startTimer(ADD);
        return backingMap.putIfAbsent(entry, true).thenApply(Objects::isNull).whenComplete((r, e) -> timer.stop(null));
    }

    @Override
    public CompletableFuture<Boolean> remove(E entry) {
        final MeteringAgent.Context timer = monitor.startTimer(REMOVE);
        return backingMap.remove(entry, true).whenComplete((r, e) -> timer.stop(null));
    }

    @Override
    public CompletableFuture<Boolean> containsAll(Collection<? extends E> c) {
        final MeteringAgent.Context timer = monitor.startTimer(CONTAINS_ALL);
        return Tools.allOf(c.stream().map(this::contains).collect(Collectors.toList())).thenApply(v ->
            v.stream().reduce(Boolean::logicalAnd).orElse(true)).whenComplete((r, e) -> timer.stop(null));
    }

    @Override
    public CompletableFuture<Boolean> addAll(Collection<? extends E> c) {
        final MeteringAgent.Context timer = monitor.startTimer(ADD_ALL);
        return Tools.allOf(c.stream().map(this::add).collect(Collectors.toList())).thenApply(v ->
            v.stream().reduce(Boolean::logicalOr).orElse(false)).whenComplete((r, e) -> timer.stop(null));
    }

    @Override
    public CompletableFuture<Boolean> retainAll(Collection<? extends E> c) {
        final MeteringAgent.Context timer = monitor.startTimer(RETAIN_ALL);
        return backingMap.keySet().thenApply(set -> Sets.difference(set, Sets.newHashSet(c)))
                                  .thenCompose(this::removeAll)
                                  .whenComplete((r, e) -> timer.stop(null));
    }

    @Override
    public CompletableFuture<Boolean> removeAll(Collection<? extends E> c) {
        final MeteringAgent.Context timer = monitor.startTimer(REMOVE_ALL);
        return Tools.allOf(c.stream().map(this::remove).collect(Collectors.toList())).thenApply(v ->
            v.stream().reduce(Boolean::logicalOr).orElse(false)).whenComplete((r, e) -> timer.stop(null));
    }

    @Override
    public CompletableFuture<Void> clear() {
        final MeteringAgent.Context timer = monitor.startTimer(CLEAR);
        return backingMap.clear().whenComplete((r, e) -> timer.stop(null));
    }

    @Override
    public CompletableFuture<? extends Set<E>> getAsImmutableSet() {
        final MeteringAgent.Context timer = monitor.startTimer(GET_AS_IMMUTABLE_SET);
        return backingMap.keySet().thenApply(s -> ImmutableSet.copyOf(s)).whenComplete((r, e) -> timer.stop(null));
    }

    @Override
    public CompletableFuture<Void> addListener(SetEventListener<E> listener) {
        MapEventListener<E, Boolean> mapEventListener = mapEvent -> {
            if (mapEvent.type() == MapEvent.Type.INSERT) {
                listener.event(new SetEvent<>(name, SetEvent.Type.ADD, mapEvent.key()));
            } else if (mapEvent.type() == MapEvent.Type.REMOVE) {
                listener.event(new SetEvent<>(name, SetEvent.Type.REMOVE, mapEvent.key()));
            }
        };
        if (listenerMapping.putIfAbsent(listener, mapEventListener) == null) {
            return backingMap.addListener(mapEventListener);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> removeListener(SetEventListener<E> listener) {
        MapEventListener<E, Boolean> mapEventListener = listenerMapping.remove(listener);
        if (mapEventListener != null) {
            return backingMap.removeListener(mapEventListener);
        }
        return CompletableFuture.completedFuture(null);
    }
}
