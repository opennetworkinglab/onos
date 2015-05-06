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
 * An integer-based cell comparator.
 * Note that null values are acceptable and are considered "smaller" than
 * any non-null value.
 */
public final class IntComparator extends AbstractCellComparator {

    // non-instantiable
    private IntComparator() { }

    @Override
    protected int nonNullCompare(Object o1, Object o2) {
        return ((int) o1) - ((int) o2);
    }

    /**
     * An instance of this class.
     */
    public static final CellComparator INSTANCE = new IntComparator();
}
