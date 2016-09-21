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

package org.onosproject.ui.table;

import com.google.common.collect.Sets;
import org.onosproject.ui.table.cell.DefaultCellComparator;
import org.onosproject.ui.table.cell.DefaultCellFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A simple model of table data.
 * <p>
 * Note that this is not a full MVC type model; the expected usage pattern
 * is to create an empty table, add rows (by consulting the business model),
 * sort rows (based on client request parameters), and finally produce the
 * sorted list of rows.
 * <p>
 * The table also provides a mechanism for defining how cell values for a
 * particular column should be formatted into strings, to help facilitate
 * the encoding of the table data into a JSON structure.
 * <p>
 * Note that it is expected that all values for a particular column will
 * be the same class.
 */
public class TableModel {

    private static final String DESC = "desc";

    private static final CellComparator DEF_CMP = DefaultCellComparator.INSTANCE;
    private static final CellFormatter DEF_FMT = DefaultCellFormatter.INSTANCE;
    private static final String EMPTY = "";

    private final String[] columnIds;
    private final Set<String> idSet;
    private final Map<String, CellComparator> comparators = new HashMap<>();
    private final Map<String, CellFormatter> formatters = new HashMap<>();
    private final List<Row> rows = new ArrayList<>();
    private final Map<String, Annot> annotations = new HashMap<>();

    /**
     * Constructs a table (devoid of data) with the given column IDs.
     *
     * @param columnIds column identifiers
     */
    public TableModel(String... columnIds) {
        checkNotNull(columnIds, "columnIds cannot be null");
        checkArgument(columnIds.length > 0, "must be at least one column");

        idSet = Sets.newHashSet(columnIds);
        if (idSet.size() != columnIds.length) {
            throw new IllegalArgumentException("duplicate column ID(s) detected");
        }

        this.columnIds = Arrays.copyOf(columnIds, columnIds.length);
    }

    private void checkId(String id) {
        checkNotNull(id, "must provide a column ID");
        if (!idSet.contains(id)) {
            throw new IllegalArgumentException("unknown column id: " + id);
        }
    }

    /**
     * Returns the number of rows in this table model.
     *
     * @return number of rows
     */
    public int rowCount() {
        return rows.size();
    }

    /**
     * Returns the number of columns in this table model.
     *
     * @return number of columns
     */
    public int columnCount() {
        return columnIds.length;
    }

    /**
     * Returns the array of column IDs for this table model.
     * <p>
     * Implementation note: we are knowingly passing you a reference to
     * our internal array to avoid copying. Don't mess with it. It's your
     * table you'll break if you do!
     *
     * @return the column identifiers
     */
    public String[] getColumnIds() {
        return columnIds;
    }

    /**
     * Returns the raw {@link Row} representation of the rows in this table.
     *
     * @return raw table rows
     */
    public Row[] getRows() {
        return rows.toArray(new Row[rows.size()]);
    }

    /**
     * Inserts a new annotation.
     *
     * @param key key of annotation
     * @param value value of annotation
     */
    public void addAnnotation(String key, Object value) {
        Annot annot = new Annot(key, value);
        annotations.put(key, annot);
    }

    /**
     * Returns the annotations in this table.
     *
     * @return annotations
     */
    public Collection<Annot> getAnnotations() {
        Collection<Annot> annots = new ArrayList<>(annotations.size());
        annotations.forEach((k, v) -> annots.add(v));
        return annots;
    }

    /**
     * Sets a cell comparator for the specified column.
     *
     * @param columnId column identifier
     * @param comparator comparator to use
     */
    public void setComparator(String columnId, CellComparator comparator) {
        checkNotNull(comparator, "must provide a comparator");
        checkId(columnId);
        comparators.put(columnId, comparator);
    }

    /**
     * Returns the cell comparator to use on values in the specified column.
     *
     * @param columnId column identifier
     * @return an appropriate cell comparator
     */
    private CellComparator getComparator(String columnId) {
        checkId(columnId);
        CellComparator cmp = comparators.get(columnId);
        return cmp == null ? DEF_CMP : cmp;
    }

    /**
     * Sets a cell formatter for the specified column.
     *
     * @param columnId column identifier
     * @param formatter formatter to use
     */
    public void setFormatter(String columnId, CellFormatter formatter) {
        checkNotNull(formatter, "must provide a formatter");
        checkId(columnId);
        formatters.put(columnId, formatter);
    }

    /**
     * Returns the cell formatter to use on values in the specified column.
     *
     * @param columnId column identifier
     * @return an appropriate cell formatter
     */
    public CellFormatter getFormatter(String columnId) {
        checkId(columnId);
        CellFormatter fmt = formatters.get(columnId);
        return fmt == null ? DEF_FMT : fmt;
    }

    /**
     * Adds a row to the table model.
     *
     * @return the row, for chaining
     */
    public Row addRow() {
        Row r = new Row();
        rows.add(r);
        return r;
    }

    /**
     * Sorts the table rows based on the specified columns, in the
     * specified directions. The second column is optional, and can be
     * disregarded by passing null into id2 and dir2.
     *
     * @param id1 first column identifier
     * @param dir1 first column sort direction
     * @param id2 second column identifier (may be null)
     * @param dir2 second column sort direction (may be null)
     */
    public void sort(String id1, SortDir dir1, String id2, SortDir dir2) {
        Collections.sort(rows, new RowComparator(id1, dir1, id2, dir2));
    }

    private boolean nullOrEmpty(String s) {
        return s == null || EMPTY.equals(s.trim());
    }

    /** Designates sorting direction. */
    public enum SortDir {
        /** Designates an ascending sort. */
        ASC,
        /** Designates a descending sort. */
        DESC
    }


    /**
     * Row comparator.
     */
    private class RowComparator implements Comparator<Row> {
        private final String id1;
        private final SortDir dir1;
        private final String id2;
        private final SortDir dir2;
        private final CellComparator cc1;
        private final CellComparator cc2;

        /**
         * Constructs a row comparator based on the specified
         * column identifiers and sort directions. Note that id2 and dir2 may
         * be null.
         *
         * @param id1 first column identifier
         * @param dir1 first column sort direction
         * @param id2 second column identifier
         * @param dir2 second column sort direction
         */
        public RowComparator(String id1, SortDir dir1, String id2, SortDir dir2) {
            this.id1 = id1;
            this.dir1 = dir1;
            this.id2 = id2;
            this.dir2 = dir2;
            cc1 = getComparator(id1);
            cc2 = nullOrEmpty(id2) ? null : getComparator(id2);
        }

        @Override
        public int compare(Row a, Row b) {
            Object cellA = a.get(id1);
            Object cellB = b.get(id1);
            int result = cc1.compare(cellA, cellB);
            result = dir1 == SortDir.ASC ? result : -result;

            if (result == 0 && cc2 != null) {
                cellA = a.get(id2);
                cellB = b.get(id2);
                result = cc2.compare(cellA, cellB);
                result = dir2 == SortDir.ASC ? result : -result;
            }
            return result;
        }
    }

    /**
     * Model of an annotation.
     */
    public class Annot {
        private final String key;
        private final Object value;

        /**
         * Constructs an annotation with the given key and value.
         *
         * @param key the key
         * @param value the value
         */
        public Annot(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Returns the annotation's key.
         *
         * @return key
         */
        public String key() {
            return key;
        }

        /**
         * Returns the annotation's value.
         *
         * @return value
         */
        public Object value() {
            return value;
        }

        /**
         * Returns the value as a string.
         * This default implementation uses the value's toString() method.
         *
         * @return the value as a string
         */
        public String valueAsString() {
            return value.toString();
        }
    }

    /**
     * Model of a row.
     */
    public class Row {
        private final Map<String, Object> cells = new HashMap<>();

        /**
         * Sets the cell value for the given column of this row.
         *
         * @param columnId column identifier
         * @param value value to set
         * @return self, for chaining
         */
        public Row cell(String columnId, Object value) {
            checkId(columnId);
            cells.put(columnId, value);
            return this;
        }

        /**
         * Returns the value of the cell in the given column for this row.
         *
         * @param columnId column identifier
         * @return cell value
         */
        public Object get(String columnId) {
            return cells.get(columnId);
        }

        /**
         * Returns the value of the cell as a string, using the
         * formatter appropriate for the column.
         *
         * @param columnId column identifier
         * @return formatted cell value
         */
        String getAsString(String columnId) {
            return getFormatter(columnId).format(get(columnId));
        }

        /**
         * Returns the row as an array of formatted strings.
         *
         * @return the formatted row data
         */
        public String[] getAsFormattedStrings() {
            List<String> formatted = new ArrayList<>(columnCount());
            for (String c : columnIds) {
                formatted.add(getAsString(c));
            }
            return formatted.toArray(new String[formatted.size()]);
        }
    }

    /**
     * Returns the appropriate sort direction for the given string.
     * <p>
     * The expected strings are "asc" for {@link SortDir#ASC ascending} and
     * "desc" for {@link SortDir#DESC descending}. Any other value will
     * default to ascending.
     *
     * @param s sort direction string encoding
     * @return sort direction
     */
    public static SortDir sortDir(String s) {
        return !DESC.equals(s) ? SortDir.ASC : SortDir.DESC;
    }
}
