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

package org.onosproject.ui.table;

import java.util.Comparator;

/**
 * Comparator for {@link TableRow}.
 */
@Deprecated
public class RowComparator implements Comparator<TableRow> {
    /** Designates the sort direction. */
    public enum Direction {
        /** Sort Ascending. */
        ASC,
        /** Sort Descending. */
        DESC
    }

    public static final String DESC_STR = "desc";

    private final String colId;
    private final Direction dir;

    /**
     * Constructs a comparator for table rows that uses the given
     * column ID and direction.
     *
     * @param colId the column to sort on
     * @param dir the direction to sort in
     */
    public RowComparator(String colId, Direction dir) {
        if (colId == null || dir == null) {
            throw new NullPointerException("Null parameters not allowed");
        }
        this.colId = colId;
        this.dir = dir;
    }

    @Override
    public int compare(TableRow a, TableRow b) {
        String cellA = a.get(colId);
        String cellB = b.get(colId);

        if (dir.equals(Direction.ASC)) {
            return cellA.compareTo(cellB);
        }
        return cellB.compareTo(cellA);
    }

    /**
     * Returns the sort direction constant for the given string.
     * The expected strings are "asc" and "desc"; defaults to "asc".
     *
     * @param s the direction as a string
     * @return the constant
     */
    public static Direction direction(String s) {
        return DESC_STR.equals(s) ? Direction.DESC : Direction.ASC;
    }
}
