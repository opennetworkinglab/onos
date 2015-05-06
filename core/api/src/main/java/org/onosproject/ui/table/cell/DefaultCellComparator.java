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
 *
 */

package org.onosproject.ui.table.cell;

import org.onosproject.ui.table.CellComparator;

/**
 * A default cell comparator. Implements a lexicographical compare function
 * (i.e. string sorting). Uses the objects' toString() method and then
 * compares the resulting strings. Note that null values are acceptable and
 * are considered "smaller" than any non-null value.
 */
public class DefaultCellComparator extends AbstractCellComparator {

    @Override
    protected int nonNullCompare(Object o1, Object o2) {
        return o1.toString().compareTo(o2.toString());
    }

    /**
     * An instance of this class.
     */
    public static final CellComparator INSTANCE = new DefaultCellComparator();
}
