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
package org.onosproject.ovsdb.rfc.schema;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A schema for the database represented by database-schema, which consists of
 * a set of tables.
 */
public final class DatabaseSchema {

    private final String name;
    private final String version;
    private final Map<String, TableSchema> tableSchemas;

    /**
     * Constructs a DatabaseSchema object.
     * @param name the name of database
     * @param version the version of database
     * @param tableSchemas a map of TableSchema
     */
    public DatabaseSchema(String name, String version,
                          Map<String, TableSchema> tableSchemas) {
        checkNotNull(name, "name cannot be null");
        checkNotNull(version, "version cannot be null");
        checkNotNull(tableSchemas, "tableSchemas cannot be null");
        this.name = name;
        this.version = version;
        this.tableSchemas = tableSchemas;
    }

    /**
     * Returns the name of database.
     * @return the name of database
     */
    public String name() {
        return name;
    }

    /**
     * Returns the version of database.
     * @return the version of database
     */
    public String version() {
        return version;
    }

    /**
     * Returns a map of TableSchema.
     * @return a map of TableSchema
     */
    public Map<String, TableSchema> tableSchemas() {
        return tableSchemas;
    }

    /**
     * Returns a set of table name.
     * @return a set of table name
     */
    public Set<String> getTableNames() {
        return this.tableSchemas.keySet();
    }

    /**
     * Determine whether contain the table.
     * @param tableName table name
     * @return boolean
     */
    public boolean hasTable(String tableName) {
        return this.getTableNames().contains(tableName);
    }

    /**
     * Returns the TableSchema whose name is the tableName.
     * @param tableName table name
     * @return TableSchema
     */
    public TableSchema getTableSchema(String tableName) {
        TableSchema table = tableSchemas.get(tableName);
        return table;
    }

    /**
     * generate initialization columns in each table namely _uuid and _version.
     */
    public void generateInitializationColumns() {
        for (TableSchema tableSchema : tableSchemas.values()) {
            tableSchema.generateInitializationColumns();
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, tableSchemas);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DatabaseSchema) {
            final DatabaseSchema other = (DatabaseSchema) obj;
            return Objects.equals(this.name, other.name)
                    && Objects.equals(this.version, other.version)
                    && Objects.equals(this.tableSchemas, other.tableSchemas);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("name", name).add("version", version)
                .add("tableSchemas", tableSchemas).toString();
    }
}
