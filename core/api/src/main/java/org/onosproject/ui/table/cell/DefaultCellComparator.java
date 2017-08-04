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

package org.onosproject.ui.table.cell;

import org.onosproject.ui.table.CellComparator;

/**
 * A default cell comparator.
 * <p>
 * Verifies that the objects being compared are the same class.
 * Looks to see if the objects being compared implement comparable and, if so,
 * delegates to that; otherwise, implements a lexicographical compare function
 * (i.e. string sorting). Uses the objects' toString() method and then
 * compares the resulting strings. Note that null values are acceptable and
 * are considered "smaller" than any non-null value.
 */
public final class DefaultCellComparator extends AbstractCellComparator {

    // non-instantiable
    private DefaultCellComparator() { }

    @Override
    @SuppressWarnings("unchecked")
    protected int nonNullCompare(Object o1, Object o2) {
        if (o1 instanceof Comparable) {
            // if o2 is not the same class as o1, then compareTo will
            // throw ClassCastException for us
            return ((Comparable) o1).compareTo(o2);
        }
        return o1.toString().compareTo(o2.toString());
    }

    /**
     * An instance of this class.
     */
    public static final CellComparator INSTANCE = new DefaultCellComparator();
}
