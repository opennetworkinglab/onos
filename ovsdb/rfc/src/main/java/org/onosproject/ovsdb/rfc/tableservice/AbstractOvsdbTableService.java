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
package org.onosproject.ovsdb.rfc.tableservice;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onosproject.ovsdb.rfc.error.ColumnSchemaNotFoundException;
import org.onosproject.ovsdb.rfc.error.TableSchemaNotFoundException;
import org.onosproject.ovsdb.rfc.error.TypedSchemaException;
import org.onosproject.ovsdb.rfc.error.VersionMismatchException;
import org.onosproject.ovsdb.rfc.notation.Column;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.notation.UUID;
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
    public AbstractOvsdbTableService(DatabaseSchema dbSchema, Row row,
                                     OvsdbTable table, VersionNum formVersion) {
        checkNotNull(dbSchema, "database schema cannot be null");
        checkNotNull(row, "row cannot be null");
        checkNotNull(table, "table cannot be null");
        checkNotNull(formVersion, "the initial version cannot be null");
        this.dbSchema = dbSchema;
        this.row = row;
        TableDescription tableDesc = new TableDescription(table, formVersion);
        this.tableDesc = tableDesc;
        row.setTableSchema(dbSchema.getTableSchema(table.tableName()));
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
    private void checkVersion(String schemaVersion, String fromVersion,
                              String untilVersion) {
        VersionUtil.versionMatch(fromVersion);
        VersionUtil.versionMatch(untilVersion);
        if (!fromVersion.equals(VersionUtil.DEFAULT_VERSION_STRING)) {
            if (VersionUtil.versionCompare(schemaVersion, fromVersion) < 0) {
                String message = VersionMismatchException
                        .createFromMessage(schemaVersion, fromVersion);
                throw new VersionMismatchException(message);
            }
        }
        if (!untilVersion.equals(VersionUtil.DEFAULT_VERSION_STRING)) {
            if (VersionUtil.versionCompare(untilVersion, schemaVersion) < 0) {
                String message = VersionMismatchException
                        .createToMessage(schemaVersion, untilVersion);
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
     * @param tableSchema TableSchema entity
     * @param columnName column name
     * @return ColumnSchema
     */
    private ColumnSchema getColumnSchema(TableSchema tableSchema,
                                         String columnName) {
        return tableSchema.getColumnSchema(columnName);
    }

    @Override
    public Column getColumnHandler(ColumnDescription columnDesc) {
        if (!isValid()) {
            return null;
        }
        String columnName = columnDesc.name();
        checkColumnSchemaVersion(columnDesc);
        if (columnName == null) {
            throw new TypedSchemaException("Error processing GetColumn : "
                    + tableDesc.name() + "." + columnDesc.method());
        }
        TableSchema tableSchema = getTableSchema();
        if (tableSchema == null) {
            String message = TableSchemaNotFoundException
                    .createMessage(tableDesc.name(), dbSchema.name());
            throw new TableSchemaNotFoundException(message);
        }
        ColumnSchema columnSchema = getColumnSchema(tableSchema, columnName);
        if (columnSchema == null) {
            String message = ColumnSchemaNotFoundException
                    .createMessage(columnName, tableSchema.name());
            throw new ColumnSchemaNotFoundException(message);
        }
        if (row == null) {
            return new Column(columnSchema, null);
        }
        return row.getColumn(columnSchema);
    }

    @Override
    public Object getDataHandler(ColumnDescription columnDesc) {
        if (!isValid()) {
            return null;
        }
        String columnName = columnDesc.name();
        checkColumnSchemaVersion(columnDesc);
        if (columnName == null) {
            throw new TypedSchemaException("Error processing GetColumn : "
                    + tableDesc.name() + "." + columnDesc.method());
        }
        TableSchema tableSchema = getTableSchema();
        if (tableSchema == null) {
            String message = TableSchemaNotFoundException
                    .createMessage(tableDesc.name(), dbSchema.name());
            throw new TableSchemaNotFoundException(message);
        }
        ColumnSchema columnSchema = getColumnSchema(tableSchema, columnName);
        if (columnSchema == null) {
            String message = ColumnSchemaNotFoundException
                    .createMessage(columnName, tableSchema.name());
            throw new ColumnSchemaNotFoundException(message);
        }
        if (row == null || row.getColumn(columnSchema) == null) {
            return null;
        }
        return row.getColumn(columnSchema).data();
    }

    @Override
    public void setDataHandler(ColumnDescription columnDesc, Object obj) {
        if (!isValid()) {
            return;
        }
        String columnName = columnDesc.name();
        checkColumnSchemaVersion(columnDesc);
        if (columnName == null) {
            throw new TypedSchemaException("Unable to locate Column Name for "
                    + tableDesc.name() + "." + columnDesc.method());
        }
        TableSchema tableSchema = getTableSchema();
        ColumnSchema columnSchema = getColumnSchema(tableSchema, columnName);
        Column column = new Column(columnSchema, obj);
        row.addColumn(columnName, column);
    }

    @Override
    public Object getTbSchema() {
        if (!isValid()) {
            return null;
        }
        if (dbSchema == null) {
            return null;
        }
        return getTableSchema();
    }

    @Override
    public UUID getUuid() {
        if (!isValid()) {
            return null;
        }
        ColumnDescription columnDesc = new ColumnDescription("_uuid",
                                                             "getTbUuid");
        return (UUID) getDataHandler(columnDesc);
    }

    @Override
    public Column getUuidColumn() {
        if (!isValid()) {
            return null;
        }
        ColumnDescription columnDesc = new ColumnDescription("_uuid",
                                                             "getTbUuidColumn");
        return (Column) getColumnHandler(columnDesc);
    }

    @Override
    public UUID getVersion() {
        if (!isValid()) {
            return null;
        }
        ColumnDescription columnDesc = new ColumnDescription("_version",
                                                             "getTbVersion");
        return (UUID) getDataHandler(columnDesc);
    }

    @Override
    public Column getVersionColumn() {
        if (!isValid()) {
            return null;
        }
        ColumnDescription columnDesc = new ColumnDescription("_version",
                                                             "getTbVersionColumn");
        return (Column) getColumnHandler(columnDesc);
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
        return Objects.hash(row);
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
        TableSchema schema = (TableSchema) getTbSchema();
        String tableName = schema.name();
        return toStringHelper(this).add("tableName", tableName).add("row", row)
                .toString();
    }
}
