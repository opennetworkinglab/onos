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
package org.onosproject.ovsdb.rfc.tableservice;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onosproject.ovsdb.rfc.exception.ColumnSchemaNotFoundException;
import org.onosproject.ovsdb.rfc.exception.TableSchemaNotFoundException;
import org.onosproject.ovsdb.rfc.exception.VersionMismatchException;
import org.onosproject.ovsdb.rfc.notation.Column;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.notation.Uuid;
import org.onosproject.ovsdb.rfc.schema.ColumnSchema;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.schema.TableSchema;
import org.onosproject.ovsdb.rfc.table.OvsdbTable;
import org.onosproject.ovsdb.rfc.table.VersionNum;
import org.onosproject.ovsdb.rfc.utils.VersionUtil;

/**
 * Representation of conversion between Ovsdb table and Row.
 */
public abstract class AbstractOvsdbTableService implements OvsdbTableService {

    private final DatabaseSchema dbSchema;
    private final Row row;
    private final TableDescription tableDesc;

    /**
     * Constructs a AbstractOvsdbTableService object.
     * @param dbSchema DatabaseSchema entity
     * @param row Row entity
     * @param table table name
     * @param formVersion the initial version
     */
    public AbstractOvsdbTableService(DatabaseSchema dbSchema, Row row, OvsdbTable table,
                                     VersionNum formVersion) {
        checkNotNull(dbSchema, "database schema cannot be null");
        checkNotNull(row, "row cannot be null");
        checkNotNull(table, "table cannot be null");
        checkNotNull(formVersion, "the initial version cannot be null");
        this.dbSchema = dbSchema;
        row.setTableName(table.tableName());
        this.row = row;
        TableDescription tableDesc = new TableDescription(table, formVersion);
        this.tableDesc = tableDesc;
    }

    /**
     * Check whether the parameter of dbSchema is valid and check whether the
     * table is existent in Database Schema.
     */
    private boolean isValid() {
        if (dbSchema == null) {
            return false;
        }
        if (!dbSchema.name().equalsIgnoreCase(tableDesc.database())) {
            return false;
        }
        checkTableSchemaVersion();
        return true;
    }

    /**
     * Check the table version.
     */
    private void checkTableSchemaVersion() {
        String fromVersion = tableDesc.fromVersion();
        String untilVersion = tableDesc.untilVersion();
        String schemaVersion = dbSchema.version();
        checkVersion(schemaVersion, fromVersion, untilVersion);
    }

    /**
     * Check the column version.
     * @param columnDesc ColumnDescription entity
     */
    private void checkColumnSchemaVersion(ColumnDescription columnDesc) {
        String fromVersion = columnDesc.fromVersion();
        String untilVersion = columnDesc.untilVersion();
        String schemaVersion = dbSchema.version();
        checkVersion(schemaVersion, fromVersion, untilVersion);
    }

    /**
     * Check whether the DatabaseSchema version between the initial version and
     * the end of the version.
     * @param schemaVersion DatabaseSchema version
     * @param fromVersion The initial version
     * @param untilVersion The end of the version
     * @throws VersionMismatchException this is a version mismatch exception
     */
    private void checkVersion(String schemaVersion, String fromVersion, String untilVersion) {
        VersionUtil.versionMatch(fromVersion);
        VersionUtil.versionMatch(untilVersion);
        if (!fromVersion.equals(VersionUtil.DEFAULT_VERSION_STRING)) {
            if (VersionUtil.versionCompare(schemaVersion, fromVersion) < 0) {
                String message = VersionMismatchException.createFromMessage(schemaVersion,
                                                                            fromVersion);
                throw new VersionMismatchException(message);
            }
        }
        if (!untilVersion.equals(VersionUtil.DEFAULT_VERSION_STRING)) {
            if (VersionUtil.versionCompare(untilVersion, schemaVersion) < 0) {
                String message = VersionMismatchException.createToMessage(schemaVersion,
                                                                          untilVersion);
                throw new VersionMismatchException(message);
            }
        }
    }

    /**
     * Returns TableSchema from dbSchema by table name.
     * @return TableSchema
     */
    private TableSchema getTableSchema() {
        String tableName = tableDesc.name();
        return dbSchema.getTableSchema(tableName);
    }

    /**
     * Returns ColumnSchema from TableSchema by column name.
     * @param columnName column name
     * @return ColumnSchema
     */
    private ColumnSchema getColumnSchema(String columnName) {
        TableSchema tableSchema = getTableSchema();
        if (tableSchema == null) {
            String message = TableSchemaNotFoundException.createMessage(tableDesc.name(),
                                                                        dbSchema.name());
            throw new TableSchemaNotFoundException(message);
        }
        ColumnSchema columnSchema = tableSchema.getColumnSchema(columnName);
        if (columnSchema == null) {
            String message = ColumnSchemaNotFoundException.createMessage(columnName,
                                                                         tableSchema.name());
            throw new ColumnSchemaNotFoundException(message);
        }
        return columnSchema;
    }

    @Override
    public Column getColumnHandler(ColumnDescription columnDesc) {
        if (!isValid()) {
            return null;
        }
        String columnName = columnDesc.name();
        checkColumnSchemaVersion(columnDesc);
        ColumnSchema columnSchema = getColumnSchema(columnName);
        if (row == null) {
            return null;
        }
        return row.getColumn(columnSchema.name());
    }

    @Override
    public Object getDataHandler(ColumnDescription columnDesc) {
        if (!isValid()) {
            return null;
        }
        String columnName = columnDesc.name();
        checkColumnSchemaVersion(columnDesc);
        ColumnSchema columnSchema = getColumnSchema(columnName);
        if (row == null || row.getColumn(columnSchema.name()) == null) {
            return null;
        }
        return row.getColumn(columnSchema.name()).data();
    }

    @Override
    public void setDataHandler(ColumnDescription columnDesc, Object obj) {
        if (!isValid()) {
            return;
        }
        String columnName = columnDesc.name();
        checkColumnSchemaVersion(columnDesc);
        ColumnSchema columnSchema = getColumnSchema(columnName);
        Column column = new Column(columnSchema.name(), obj);
        row.addColumn(columnName, column);
    }

    @Override
    public Uuid getTableUuid() {
        if (!isValid()) {
            return null;
        }
        ColumnDescription columnDesc = new ColumnDescription("_uuid", "getTableUuid");
        return (Uuid) getDataHandler(columnDesc);
    }

    @Override
    public Column getTableUuidColumn() {
        if (!isValid()) {
            return null;
        }
        ColumnDescription columnDesc = new ColumnDescription("_uuid", "getTableUuidColumn");
        return getColumnHandler(columnDesc);
    }

    @Override
    public Uuid getTableVersion() {
        if (!isValid()) {
            return null;
        }
        ColumnDescription columnDesc = new ColumnDescription("_version", "getTableVersion");
        return (Uuid) getDataHandler(columnDesc);
    }

    @Override
    public Column getTableVersionColumn() {
        if (!isValid()) {
            return null;
        }
        ColumnDescription columnDesc = new ColumnDescription("_version", "getTableVersionColumn");
        return getColumnHandler(columnDesc);
    }

    /**
     * Get DatabaseSchema entity.
     * @return DatabaseSchema entity
     */
    public DatabaseSchema dbSchema() {
        return dbSchema;
    }

    /**
     * Get Row entity.
     * @return Row entity
     */
    public Row getRow() {
        if (!isValid()) {
            return null;
        }
        return this.row;
    }

    /**
     * Get TableDescription entity.
     * @return TableDescription entity
     */
    public TableDescription tableDesc() {
        return tableDesc;
    }

    @Override
    public int hashCode() {
        return row.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AbstractOvsdbTableService) {
            final AbstractOvsdbTableService other = (AbstractOvsdbTableService) obj;
            return Objects.equals(this.row, other.row);
        }
        return false;
    }

    @Override
    public String toString() {
        TableSchema schema = getTableSchema();
        String tableName = schema.name();
        return toStringHelper(this).add("tableName", tableName).add("row", row).toString();
    }
}
