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
package org.onosproject.ovsdb.rfc.schema;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.onosproject.ovsdb.rfc.schema.type.AtomicColumnType;
import org.onosproject.ovsdb.rfc.schema.type.UuidBaseType;

/**
 * A schema for the table represented by table-schema, which consists of a set
 * of columns.
 */
public final class TableSchema {

    private final String name;
    private final Map<String, ColumnSchema> columnSchemas;

    /**
     * Constructs a TableSchema object.
     * @param name the name of table
     * @param columnSchemas a map of ColumnSchema
     */
    public TableSchema(String name, Map<String, ColumnSchema> columnSchemas) {
        checkNotNull(name, "name cannot be null");
        checkNotNull(columnSchemas, "columnSchemas cannot be null");
        this.name = name;
        this.columnSchemas = columnSchemas;
    }

    /**
     * Returns the name of table.
     * @return the name of table
     */
    public String name() {
        return name;
    }

    /**
     * Returns a map of ColumnSchema.
     * @return a map of ColumnSchema
     */
    public Map<String, ColumnSchema> columnSchemas() {
        return this.columnSchemas;
    }

    /**
     * Returns a set of column name.
     * @return a set of column name
     */
    public Set<String> getColumnNames() {
        return this.columnSchemas.keySet();
    }

    /**
     * Determine whether contain the column.
     * @param columnName column name
     * @return boolean
     */
    public boolean hasColumn(String columnName) {
        return this.getColumnNames().contains(columnName);
    }

    /**
     * Returns the ColumnSchema whose name is the columnName.
     * @param columnName column name
     * @return ColumnSchema
     */
    public ColumnSchema getColumnSchema(String columnName) {
        return this.columnSchemas.get(columnName);
    }

    /**
     * Refer to RFC 7047 Section 3.2. generate initialization columns in each
     * table namely _uuid and _version.
     */
    public void generateInitializationColumns() {
        columnSchemas
                .put("_uuid",
                     new ColumnSchema("_uuid",
                                      new AtomicColumnType(new UuidBaseType())));
        columnSchemas
                .put("_version",
                     new ColumnSchema("_version",
                                      new AtomicColumnType(new UuidBaseType())));
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columnSchemas);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TableSchema) {
            final TableSchema other = (TableSchema) obj;
            return Objects.equals(this.name, other.name)
                    && Objects.equals(this.columnSchemas, other.columnSchemas);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("name", name)
                .add("columnSchemas", columnSchemas).toString();
    }
}
