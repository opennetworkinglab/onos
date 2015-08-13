/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.consistent.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.SetEvent;
import org.onosproject.store.service.SetEventListener;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of distributed set that is backed by a ConsistentMap.

 * @param <E> set element type
 */
public class DefaultDistributedSet<E> implements DistributedSet<E> {

    private static final String CONTAINS = "contains";
    private static final String PRIMITIVE_NAME = "distributedSet";
    private static final String SIZE = "size";
    private static final String IS_EMPTY = "isEmpty";
    private static final String ITERATOR = "iterator";
    private static final String TO_ARRAY = "toArray";
    private static final String ADD = "add";
    private static final String REMOVE = "remove";
    private static final String CONTAINS_ALL = "containsAll";
    private static final String ADD_ALL = "addAll";
    private static final String RETAIN_ALL = "retainAll";
    private static final String REMOVE_ALL = "removeAll";
    private static final String CLEAR = "clear";

    private final String name;
    private final ConsistentMap<E, Boolean> backingMap;
    private final Map<SetEventListener<E>, MapEventListener<E, Boolean>> listenerMapping = Maps.newIdentityHashMap();
    private final MeteringAgent monitor;

    public DefaultDistributedSet(String name, boolean meteringEnabled, ConsistentMap<E, Boolean> backingMap) {
        this.name = name;
        this.backingMap = backingMap;
        monitor = new MeteringAgent(PRIMITIVE_NAME, name, meteringEnabled);
    }

    @Override
    public int size() {
        final MeteringAgent.Context timer = monitor.startTimer(SIZE);
        try {
            return backingMap.size();
        } finally {
            timer.stop(null);
        }
    }

    @Override
    public boolean isEmpty() {
        final MeteringAgent.Context timer = monitor.startTimer(IS_EMPTY);
        try {
            return backingMap.isEmpty();
        } finally {
            timer.stop(null);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        final MeteringAgent.Context timer = monitor.startTimer(CONTAINS);
        try {
            return backingMap.containsKey((E) o);
        } finally {
            timer.stop(null);
        }
    }

    @Override
    public Iterator<E> iterator() {
        final MeteringAgent.Context timer = monitor.startTimer(ITERATOR);
        //Do we have to measure this guy?
        try {
            return backingMap.keySet().iterator();
        } finally {
            timer.stop(null);
        }
    }

    @Override
    public Object[] toArray() {
        final MeteringAgent.Context timer = monitor.startTimer(TO_ARRAY);
        try {
            return backingMap.keySet().stream().toArray();
        } finally {
            timer.stop(null);
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        final MeteringAgent.Context timer = monitor.startTimer(TO_ARRAY);
        try {
            return backingMap.keySet().stream().toArray(size -> a);
        } finally {
            timer.stop(null);
        }
    }

    @Override
    public boolean add(E e) {
        final MeteringAgent.Context timer = monitor.startTimer(ADD);
        try {
            return backingMap.putIfAbsent(e, true) == null;
        } finally {
            timer.stop(null);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
        final MeteringAgent.Context timer = monitor.startTimer(REMOVE);
        try {
            return backingMap.remove((E) o) != null;
        } finally {
            timer.stop(null);
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        final MeteringAgent.Context timer = monitor.startTimer(CONTAINS_ALL);
        try {
           return c.stream()
                .allMatch(this::contains);
        } finally {
            timer.stop(null);
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        final MeteringAgent.Context timer = monitor.startTimer(ADD_ALL);
        try {
            return c.stream()
                .map(this::add)
                .reduce(Boolean::logicalOr)
                .orElse(false);
        } finally {
            timer.stop(null);
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        final MeteringAgent.Context timer = monitor.startTimer(RETAIN_ALL);
        try {
            Set<?> retainSet = Sets.newHashSet(c);
            return backingMap.keySet()
                .stream()
                .filter(k -> !retainSet.contains(k))
                .map(this::remove)
                .reduce(Boolean::logicalOr)
                .orElse(false);
        } finally {
            timer.stop(null);
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        final MeteringAgent.Context timer = monitor.startTimer(REMOVE_ALL);
        try {
            Set<?> removeSet = Sets.newHashSet(c);
            return backingMap.keySet()
                .stream()
                .filter(removeSet::contains)
                .map(this::remove)
                .reduce(Boolean::logicalOr)
                .orElse(false);
        } finally {
            timer.stop(null);
        }
    }

    @Override
    public void clear() {
        final MeteringAgent.Context timer = monitor.startTimer(CLEAR);
        try {
            backingMap.clear();
        } finally {
            timer.stop(null);
        }
    }

    @Override
    public void addListener(SetEventListener<E> listener) {
        MapEventListener<E, Boolean> mapEventListener = mapEvent -> {
            if (mapEvent.type() == MapEvent.Type.INSERT) {
                listener.event(new SetEvent<>(name, SetEvent.Type.ADD, mapEvent.key()));
            } else if (mapEvent.type() == MapEvent.Type.REMOVE) {
                listener.event(new SetEvent<>(name, SetEvent.Type.REMOVE, mapEvent.key()));
            }
        };
        if (listenerMapping.putIfAbsent(listener, mapEventListener) == null) {
            backingMap.addListener(mapEventListener);
        }
    }

    @Override
    public void removeListener(SetEventListener<E> listener) {
        MapEventListener<E, Boolean> mapEventListener = listenerMapping.remove(listener);
        if (mapEventListener != null) {
            backingMap.removeListener(mapEventListener);
        }
    }
}
