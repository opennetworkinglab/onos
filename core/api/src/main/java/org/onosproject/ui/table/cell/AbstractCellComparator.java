/*
 * Copyright 2015-present Open Networking Laboratory
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
 * Base implementation of a {@link CellComparator}. This class takes care
 * of dealing with null inputs; subclasses should implement their comparison
 * knowing that both inputs are guaranteed to be non-null.
 */
public abstract class AbstractCellComparator implements CellComparator {

    @Override
    public int compare(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return 0;       // o1 == o2
        }
        if (o1 == null) {
            return -1;      // o1 < o2
        }
        if (o2 == null) {
            return 1;       // o1 > o2
        }
        return nonNullCompare(o1, o2);
    }

    /**
     * Compares its two arguments for order.  Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.
     * <p>
     * Note that both objects are guaranteed to be non-null.
     *
     * @see java.util.Comparator#compare(Object, Object)
     *
     * @param o1 the first object to be compared
     * @param o2 the second object to be compared
     * @return an integer representing relative ordering
     * @throws ClassCastException if the arguments' types prevent them from
     *         being compared by this comparator
     */
    protected abstract int nonNullCompare(Object o1, Object o2);
}
