/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onlab.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import com.google.common.collect.Iterators;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A Set providing additional get, insertOrReplace and conditionalRemove methods.
 */
public class ExtendedSet<E> implements Set<E> {

    private final Map<E, E> map;

    /**
     * Constructs a new instance by backing it with the supplied Map.
     * <p>
     * Constructed ExtendedSet will have the same concurrency properties as that of the supplied Map.
     *
     * @param map input map.
     */
    public ExtendedSet(Map<E, E> map) {
        this.map = map;
    }

    /**
     * Returns set element that is equal to the specified object.
     * @param o object
     * @return set element that is equal to the input argument or null if no such set element exists
     */
    public E get(Object o) {
        return map.get(o);
    }

    /**
     * Inserts the entry if it is not already in the set otherwise replaces the existing entry
     * if the supplied predicate evaluates to true.
     * @param entry entry to add
     * @param entryTest predicate that is used to evaluate if the existing entry should be replaced
     * @return true if the set is updated; false otherwise
     */
    public boolean insertOrReplace(E entry, Predicate<E> entryTest) {
        AtomicBoolean updated = new AtomicBoolean(false);
        map.compute(checkNotNull(entry), (k, v) -> {
            if (v == null || entryTest.test(v)) {
                updated.set(true);
                return entry;
            }
            return v;
        });
        return updated.get();
    }

    /**
     * Removes the entry if the supplied predicate evaluates to true.
     * @param entry entry to remove
     * @param entryTest predicate that is used to evaluated aginst the existing entry. Return value of
     * true implies value should be removed.
     * @return true if the set is updated; false otherwise
     */
    public boolean conditionalRemove(E entry, Predicate<E> entryTest) {
        AtomicBoolean updated = new AtomicBoolean(false);
        map.compute(entry, (k, v) -> {
            if (entryTest.test(v)) {
                updated.set(true);
                return null;
            }
            return v;
        });
        return updated.get();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<E> iterator() {
        return Iterators.transform(map.entrySet().iterator(), Map.Entry::getValue);
    }

    @Override
    public Object[] toArray() {
        return map.values().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return map.values().toArray(a);
    }

    @Override
    public boolean add(E e) {
        return map.putIfAbsent(e, e) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream()
                .map(map::containsKey)
                .reduce(Boolean::logicalAnd)
                .orElse(true);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return c.stream()
                .map(e -> map.putIfAbsent(e, e) == null)
                .reduce(Boolean::logicalOr)
                .orElse(false);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return c.stream()
                .filter(e -> !map.containsKey(e))
                .map(e -> map.remove(e) != null)
                .reduce(Boolean::logicalOr)
                .orElse(false);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return c.stream()
                .map(e -> map.remove(e) != null)
                .reduce(Boolean::logicalOr)
                .orElse(false);
    }

    @Override
    public void clear() {
        map.clear();
    }
}