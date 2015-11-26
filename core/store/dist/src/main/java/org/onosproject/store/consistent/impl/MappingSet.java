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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Iterators;

/**
 * Set view backed by Set with element type {@code <BACK>} but returns
 * element as {@code <OUT>} for convenience.
 *
 * @param <BACK> Backing {@link Set} element type.
 *        MappingSet will follow this type's equality behavior.
 * @param <OUT> external facing element type.
 *        MappingSet will ignores equality defined by this type.
 */
class MappingSet<BACK, OUT> implements Set<OUT> {

    private final Set<BACK> backedSet;
    private final Function<OUT, BACK> toBack;
    private final Function<BACK, OUT> toOut;

    public MappingSet(Set<BACK> backedSet,
                      Function<Set<BACK>, Set<BACK>> supplier,
                      Function<OUT, BACK> toBack, Function<BACK, OUT> toOut) {
        this.backedSet = supplier.apply(backedSet);
        this.toBack = toBack;
        this.toOut = toOut;
    }

    @Override
    public int size() {
        return backedSet.size();
    }

    @Override
    public boolean isEmpty() {
        return backedSet.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return backedSet.contains(toBack.apply((OUT) o));
    }

    @Override
    public Iterator<OUT> iterator() {
        return Iterators.transform(backedSet.iterator(), toOut::apply);
    }

    @Override
    public Object[] toArray() {
        return backedSet.stream()
                .map(toOut)
                .toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return backedSet.stream()
                .map(toOut)
                .toArray(size -> {
                    if (size < a.length) {
                        return (T[]) new Object[size];
                    } else {
                        Arrays.fill(a, null);
                        return a;
                    }
                });
    }

    @Override
    public boolean add(OUT e) {
        return backedSet.add(toBack.apply(e));
    }

    @Override
    public boolean remove(Object o) {
        return backedSet.remove(toBack.apply((OUT) o));
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream()
            .map(e -> toBack.apply((OUT) e))
            .allMatch(backedSet::contains);
    }

    @Override
    public boolean addAll(Collection<? extends OUT> c) {
        return backedSet.addAll(c.stream().map(toBack).collect(Collectors.toList()));
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return backedSet.retainAll(c.stream()
                .map(x -> toBack.apply((OUT) x))
                .collect(Collectors.toList()));
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return backedSet.removeAll(c.stream()
                .map(x -> toBack.apply((OUT) x))
                .collect(Collectors.toList()));
    }

    @Override
    public void clear() {
        backedSet.clear();
    }
}
