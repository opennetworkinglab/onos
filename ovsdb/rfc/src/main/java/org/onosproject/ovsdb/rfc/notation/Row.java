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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.onosproject.ovsdb.rfc.schema.ColumnSchema;
import org.onosproject.ovsdb.rfc.schema.TableSchema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;

/**
 * Row is the basic element of the OpenVswitch's table.
 */
public final class Row {
    @JsonIgnore
    private TableSchema tableSchema;
    private Map<String, Column> columns;

    /**
     * Row constructor.
     */
    public Row() {
        this.columns = Maps.newHashMap();
    }

    /**
     * Row constructor.
     * @param tableSchema TableSchema entity
     */
    public Row(TableSchema tableSchema) {
        checkNotNull(tableSchema, "tableSchema cannot be null");
        this.tableSchema = tableSchema;
        this.columns = Maps.newHashMap();
    }

    /**
     * Row constructor.
     * @param tableSchema TableSchema entity
     * @param columns List of Column entity
     */
    public Row(TableSchema tableSchema, List<Column> columns) {
        checkNotNull(tableSchema, "tableSchema cannot be null");
        checkNotNull(columns, "columns cannot be null");
        this.tableSchema = tableSchema;
        this.columns = Maps.newHashMap();
        for (Column column : columns) {
            this.columns.put(column.schema().name(), column);
        }
    }

    /**
     * Returns tableSchema.
     * @return tableSchema
     */
    public TableSchema getTableSchema() {
        return tableSchema;
    }

    /**
     * Set tableSchema value.
     * @param tableSchema TableSchema entity
     */
    public void setTableSchema(TableSchema tableSchema) {
        this.tableSchema = tableSchema;
    }

    /**
     * Returns Column by ColumnSchema.
     * @param schema ColumnSchema entity
     * @return Column
     */
    public Column getColumn(ColumnSchema schema) {
        return (Column) columns.get(schema.name());
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
        return Objects.hash(tableSchema, columns);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Row) {
            final Row other = (Row) obj;
            return Objects.equals(this.tableSchema, other.tableSchema)
                    && Objects.equals(this.columns, other.columns);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("tableSchema", tableSchema).add("columns", columns).toString();
    }
}
