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
package org.onosproject.ovsdb.rfc.table;

import java.util.Map;
import java.util.Set;

import org.onosproject.ovsdb.rfc.notation.Column;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.tableservice.AbstractOvsdbTableService;
import org.onosproject.ovsdb.rfc.tableservice.ColumnDescription;

/**
 * This class provides operations of Manager Table.
 */
public class Manager extends AbstractOvsdbTableService {
    /**
     * Manager table column name.
     */
    public enum ManagerColumn {
        TARGET("target"), ISCONNECTED("is_connected"), CONNECTIONMODE("connection_mode"),
        EXTERNALIDS("external_ids"), STATUS("status"), INACTIVITYPROBE("inactivity_probe"),
        OTHERCONFIG("other_config"), MAXBACKOFF("max_backoff");

        private final String columnName;

        ManagerColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for ManagerColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a Manager object. Generate Manager Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public Manager(DatabaseSchema dbSchema, Row row) {
        super(dbSchema, row, OvsdbTable.MANAGER, VersionNum.VERSION100);
    }

    /**
     * Get the Column entity which column name is "target" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "target"
     */
    public Column getTargetColumn() {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.TARGET.columnName(),
                                                             "getTargetColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "target" to the Row entity of
     * attributes.
     * @param target the column data which column name is "target"
     */
    public void setTarget(Set<String> target) {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.TARGET.columnName(),
                                                             "setTarget",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, target);
    }

    /**
     * Get the Column entity which column name is "is_connected" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "is_connected"
     */
    public Column getIsConnectedColumn() {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.ISCONNECTED.columnName(),
                                                             "getIsConnectedColumn", VersionNum.VERSION110);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "is_connected" to the Row entity
     * of attributes.
     * @param isConnected the column data which column name is "is_connected"
     */
    public void setIsConnected(Boolean isConnected) {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.ISCONNECTED.columnName(),
                                                             "setIsConnected", VersionNum.VERSION110);
        super.setDataHandler(columndesc, isConnected);
    }

    /**
     * Get the Column entity which column name is "other_config" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "other_config"
     */
    public Column getOtherConfigColumn() {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.OTHERCONFIG.columnName(),
                                                             "getOtherConfigColumn", VersionNum.VERSION680);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "other_config" to the Row entity
     * of attributes.
     * @param otherConfig the column data which column name is "other_config"
     */
    public void setOtherConfig(Map<String, String> otherConfig) {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.OTHERCONFIG.columnName(),
                                                             "setOtherConfig", VersionNum.VERSION680);
        super.setDataHandler(columndesc, otherConfig);
    }

    /**
     * Get the Column entity which column name is "external_ids" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "external_ids"
     */
    public Column getExternalIdsColumn() {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.EXTERNALIDS.columnName(),
                                                             "getExternalIdsColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "external_ids" to the Row entity
     * of attributes.
     * @param externalIds the column data which column name is "external_ids"
     */
    public void setExternalIds(Map<String, String> externalIds) {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.EXTERNALIDS.columnName(),
                                                             "setExternalIds", VersionNum.VERSION100);
        super.setDataHandler(columndesc, externalIds);
    }

    /**
     * Get the Column entity which column name is "max_backoff" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "max_backoff"
     */
    public Column getMaxBackoffColumn() {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.MAXBACKOFF.columnName(),
                                                             "getMaxBackoffColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "max_backoff" to the Row entity
     * of attributes.
     * @param maxBackoff the column data which column name is "max_backoff"
     */
    public void setMaxBackoff(Set<Long> maxBackoff) {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.MAXBACKOFF.columnName(),
                                                             "setMaxBackoff", VersionNum.VERSION100);
        super.setDataHandler(columndesc, maxBackoff);
    }

    /**
     * Get the Column entity which column name is "status" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "status"
     */
    public Column getStatusColumn() {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.STATUS.columnName(),
                                                             "getStatusColumn", VersionNum.VERSION110);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "status" to the Row entity of
     * attributes.
     * @param status the column data which column name is "status"
     */
    public void setStatus(Map<String, String> status) {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.STATUS.columnName(),
                                                             "setStatus",
                                                             VersionNum.VERSION110);
        super.setDataHandler(columndesc, status);
    }

    /**
     * Get the Column entity which column name is "inactivity_probe" from the
     * Row entity of attributes.
     * @return the Column entity which column name is "inactivity_probe"
     */
    public Column getInactivityProbeColumn() {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.INACTIVITYPROBE.columnName(),
                                                             "getInactivityProbeColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "inactivity_probe" to the Row
     * entity of attributes.
     * @param inactivityProbe the column data which column name is
     *            "inactivity_probe"
     */
    public void setInactivityProbe(Set<Long> inactivityProbe) {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.INACTIVITYPROBE.columnName(),
                                                             "setInactivityProbe", VersionNum.VERSION100);
        super.setDataHandler(columndesc, inactivityProbe);
    }

    /**
     * Get the Column entity which column name is "connection_mode" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "connection_mode"
     */
    public Column getConnectionModeColumn() {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.CONNECTIONMODE.columnName(),
                                                             "getConnectionModeColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "connection_mode" to the Row
     * entity of attributes.
     * @param connectionMode the column data which column name is
     *            "connection_mode"
     */
    public void setConnectionMode(Set<String> connectionMode) {
        ColumnDescription columndesc = new ColumnDescription(ManagerColumn.CONNECTIONMODE.columnName(),
                                                             "setConnectionMode", VersionNum.VERSION100);
        super.setDataHandler(columndesc, connectionMode);
    }
}
