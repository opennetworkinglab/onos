/**
 *    Copyright 2012 Big Switch Networks, Inc.
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
 * Iterator over all values in an iterator of iterators.
 *
 * @param <T> the type of elements returned by this iterator
 */
public class MultiIterator<T> implements Iterator<T> {
    Iterator<Iterator<T>> subIterator;
    Iterator<T> current = null;

    public MultiIterator(Iterator<Iterator<T>> subIterator) {
        super();
        this.subIterator = subIterator;
    }

    @Override
    public boolean hasNext() {
        if (current == null) {
            if (subIterator.hasNext()) {
                current = subIterator.next();
            } else {
                return false;
            }
        }
        while (!current.hasNext() && subIterator.hasNext()) {
            current = subIterator.next();
        }

        return current.hasNext();
    }

    @Override
    public T next() {
        if (hasNext()) {
            return current.next();
        }
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        if (hasNext()) {
            current.remove();
        }
        throw new NoSuchElementException();
    }
}
