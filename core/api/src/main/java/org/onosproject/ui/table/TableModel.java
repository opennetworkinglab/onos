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

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A model of table data.
 */
public class TableModel {

    private static final CellFormatter DEF_FMT = new DefaultCellFormatter();

    private final String[] columnIds;
    private final Set<String> idSet;
    private final Map<String, CellFormatter> formatters = new HashMap<>();
    private final List<Row> rows = new ArrayList<>();


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
     * Returns the {@link TableRow} representation of the rows in this table.
     *
     * @return formatted table rows
     */
    public TableRow[] getTableRows() {
        return new TableRow[0];
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
            checkNotNull(value, "Must supply some value");
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
    }
}
