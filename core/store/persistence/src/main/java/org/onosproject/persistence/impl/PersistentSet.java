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

package org.onosproject.persistence.impl;

import com.google.common.collect.Iterators;
import org.mapdb.DB;
import org.mapdb.Hasher;
import org.mapdb.Serializer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A set implementation that gets and receives all data from a serialized internal set.
 */
//TODO add locking for reads and writes
public class PersistentSet<E> implements Set<E> {

    private final org.onosproject.store.service.Serializer serializer;

    private final org.mapdb.DB database;

    private final Set<byte[]> items;

    private final String name;

    public PersistentSet(org.onosproject.store.service.Serializer serializer, DB database, String name) {
        this.serializer = checkNotNull(serializer);
        this.database = checkNotNull(database);
        this.name = checkNotNull(name);

        items = database
                .createHashSet(name)
                .serializer(Serializer.BYTE_ARRAY)
                .hasher(Hasher.BYTE_ARRAY)
                .makeOrGet();
    }

    public void readInto(Set<E> items) {
        this.items.forEach(item -> items.add(serializer.decode(item)));
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        checkNotNull(o, "The argument cannot be null");
        return items.contains(serializer.encode(o));
    }

    @Override
    public Iterator<E> iterator() {
        return Iterators.transform(items.iterator(), serializer::decode);
    }

    @Override
    public Object[] toArray() {
        Object[] retArray = new Object[items.size()];
        int index = 0;
        for (byte[] item : items) {
            retArray[index] = serializer.decode(item);
            index++;
        }
        return retArray;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        checkNotNull(a, "The passed in array cannot be null.");
        int index = 0;
        Iterator<byte[]> iterator = items.iterator();
        T[] retArray;
        if (a.length >= items.size()) {
            retArray = a;
        } else {
            retArray = (T[]) new Object[items.size()];
        }
        while (iterator.hasNext()) {
            retArray[index++] = serializer.decode(iterator.next());
        }
        if (retArray.length > items.size()) {
            retArray[index] = null;
        }
        return retArray;
    }

    @Override
    public boolean add(E item) {
        checkNotNull(item, "Item to be added cannot be null.");
        return items.add(serializer.encode(item));
    }

    @Override
    public boolean remove(Object o) {
        checkNotNull(o, "Item to be removed cannot be null.");
        return items.remove(serializer.encode(o));
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        checkNotNull(c, "Collection cannot be internal.");
        for (Object item : c) {
            if (!items.contains(serializer.encode(item))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        checkNotNull(c, "The collection to be added cannot be null.");
        boolean changed = false;
        for (Object item : c) {
            changed = items.add(serializer.encode(item)) || changed;
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        for (byte[] item : items) {
            E deserialized = serializer.decode(item);
            if (!c.contains(deserialized)) {
                changed = items.remove(item) || changed;
            }
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object item : c) {
            changed = items.remove(serializer.encode(item)) || changed;
        }
        return changed;
    }

    @Override
    public void clear() {
        items.clear();
    }

    @Override
    public boolean equals(Object set) {
        //This is not threadsafe and on larger sets incurs a significant processing cost
        if (!(set instanceof Set)) {
            return false;
        }
        Set asSet = (Set) set;
        if (asSet.size() != this.size()) {
            return false;
        }
        for (Object item : this) {
            if (!asSet.contains(item)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
