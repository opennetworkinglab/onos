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
package org.onosproject.ovsdb.rfc.notation;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.Maps;

/**
 * Row is the basic element of the OpenVswitch's table.
 */
public final class Row {
    private String tableName;
    private Map<String, Column> columns;

    /**
     * Row constructor.
     */
    public Row() {
        this.columns = Maps.newHashMap();
    }

    /**
     * Row constructor.
     * @param tableName table name
     */
    public Row(String tableName) {
        checkNotNull(tableName, "tableName cannot be null");
        this.tableName = tableName;
        this.columns = Maps.newHashMap();
    }

    /**
     * Row constructor.
     * @param tableName table name
     * @param columns Map of Column entity
     */
    public Row(String tableName, Map<String, Column> columns) {
        checkNotNull(tableName, "table name cannot be null");
        checkNotNull(columns, "columns cannot be null");
        this.tableName = tableName;
        this.columns = columns;
    }

    /**
     * Returns tableName.
     * @return tableName
     */
    public String tableName() {
        return tableName;
    }

    /**
     * Set tableName value.
     * @param tableName table name
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns Column by ColumnSchema.
     * @param columnName column name
     * @return Column
     */
    public Column getColumn(String columnName) {
        return columns.get(columnName);
    }

    /**
     * Returns Collection of Column.
     * @return Collection of Column
     */
    public Collection<Column> getColumns() {
        return columns.values();
    }

    /**
     * add Column.
     * @param columnName column name
     * @param data Column entity
     */
    public void addColumn(String columnName, Column data) {
        this.columns.put(columnName, data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, columns);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Row) {
            final Row other = (Row) obj;
            return Objects.equals(this.tableName, other.tableName)
                    && Objects.equals(this.columns, other.columns);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("tableName", tableName)
                .add("columns", columns).toString();
    }
}
