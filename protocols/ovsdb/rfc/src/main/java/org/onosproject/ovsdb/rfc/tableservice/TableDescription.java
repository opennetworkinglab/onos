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
package org.onosproject.ovsdb.rfc.tableservice;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.ovsdb.rfc.table.OvsdbTable;
import org.onosproject.ovsdb.rfc.table.VersionNum;
import org.onosproject.ovsdb.rfc.utils.VersionUtil;

/**
 * Table description.
 */
public class TableDescription {

    // The table name
    private final String name;
    // The database name
    private final String database;
    // The initial version
    private final String fromVersion;
    // The end of the version
    private final String untilVersion;

    /**
     * Constructs a MonitorRequest object.
     * @param table OvsdbTable entity
     */
    public TableDescription(OvsdbTable table) {
        checkNotNull(table, "table cannot be null");
        this.name = table.tableName();
        this.fromVersion = VersionUtil.DEFAULT_VERSION_STRING;
        this.untilVersion = VersionUtil.DEFAULT_VERSION_STRING;
        this.database = "Open_vSwitch";
    }

    /**
     * Constructs a MonitorRequest object.
     * @param table OvsdbTable entity
     * @param fromVersion the initial version
     */
    public TableDescription(OvsdbTable table, VersionNum fromVersion) {
        checkNotNull(table, "table cannot be null");
        checkNotNull(fromVersion, "the initial version cannot be null");
        this.name = table.tableName();
        this.fromVersion = fromVersion.versionNum();
        this.untilVersion = VersionUtil.DEFAULT_VERSION_STRING;
        this.database = "Open_vSwitch";
    }

    /**
     * Constructs a MonitorRequest object.
     * @param table OvsdbTable entity
     * @param database database name
     * @param fromVersion the initial version
     * @param untilVersion the untill version
     */
    public TableDescription(OvsdbTable table, String database, VersionNum fromVersion, VersionNum untilVersion) {
        checkNotNull(table, "table cannot be null");
        checkNotNull(database, "database cannot be null");
        checkNotNull(fromVersion, "the initial version cannot be null");
        checkNotNull(untilVersion, "the end of the version cannot be null");
        this.name = table.tableName();
        this.fromVersion = fromVersion.versionNum();
        this.untilVersion = untilVersion.versionNum();
        this.database = database;
    }

    /**
     * Constructs a MonitorRequest object.
     * @param table OvsdbTable entity
     * @param fromVersion the initial version
     * @param untilVersion the end of the version
     */
    public TableDescription(OvsdbTable table, VersionNum fromVersion, VersionNum untilVersion) {
        checkNotNull(table, "table cannot be null");
        checkNotNull(fromVersion, "the initial version cannot be null");
        checkNotNull(untilVersion, "the end of the version cannot be null");
        this.name = table.tableName();
        this.fromVersion = fromVersion.versionNum();
        this.untilVersion = untilVersion.versionNum();
        this.database = "Open_vSwitch";
    }

    /**
     * Returns the column name.
     * @return the column name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the database name.
     * @return the database name
     */
    public String database() {
        return database;
    }

    /**
     * Returns the initial version.
     * @return the initial version
     */
    public String fromVersion() {
        return fromVersion;
    }

    /**
     * Returns the end of the version.
     * @return the end of the version
     */
    public String untilVersion() {
        return untilVersion;
    }
}
