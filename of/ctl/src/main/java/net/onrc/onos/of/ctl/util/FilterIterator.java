/**
 *    Copyright 2012, Big Switch Networks, Inc.
 *    Originally created by David Erickson, Stanford University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.onrc.onos.of.ctl.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator that will filter values from an iterator and return only
 * those values that match the predicate.
 */
public abstract class FilterIterator<T> implements Iterator<T> {
    protected Iterator<T> subIterator;
    protected T next;

    /**
     * Construct a filter iterator from the given sub iterator.
     *
     * @param subIterator the sub iterator over which we'll filter
     */
    public FilterIterator(Iterator<T> subIterator) {
        super();
        this.subIterator = subIterator;
    }

    /**
     * Check whether the given value should be returned by the
     * filter.
     *
     * @param value the value to check
     * @return true if the value should be included
     */
    protected abstract boolean matches(T value);

    // ***********
    // Iterator<T>
    // ***********

    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }

        while (subIterator.hasNext()) {
            next = subIterator.next();
            if (matches(next)) {
                return true;
            }
        }
        next = null;
        return false;
    }

    @Override
    public T next() {
        if (hasNext()) {
            T cur = next;
            next = null;
            return cur;
        }
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
