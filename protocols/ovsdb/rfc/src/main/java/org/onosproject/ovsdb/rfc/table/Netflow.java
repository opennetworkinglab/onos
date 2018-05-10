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
 * This class provides operations of Netflow Table.
 */
public class Netflow extends AbstractOvsdbTableService {
    /**
     * Netflow table column name.
     */
    public enum NetflowColumn {
        TARGETS("targets"), ACTIVETIMEOUT("active_timeout"), ENGINETYPE("engine_type"),
        EXTERNALIDS("external_ids"), ADDIDTOINTERFACE("add_id_to_interface"), ENGINEID("engine_id");

        private final String columnName;

        NetflowColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for NetflowColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a NetFlow object. Generate Netflow Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public Netflow(DatabaseSchema dbSchema, Row row) {
        super(dbSchema, row, OvsdbTable.NETFLOW, VersionNum.VERSION100);
    }

    /**
     * Get the Column entity which column name is "targets" from the Row entity
     * of attributes.
     * @return the Column entity
     */
    public Column getTargetsColumn() {
        ColumnDescription columndesc = new ColumnDescription(NetflowColumn.TARGETS.columnName(),
                                                             "getTargetsColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "targets" to the Row entity of
     * attributes.
     * @param targets the column data which column name is "targets"
     */
    public void setTargets(Set<String> targets) {
        ColumnDescription columndesc = new ColumnDescription(NetflowColumn.TARGETS.columnName(),
                                                             "setTargets", VersionNum.VERSION100);
        super.setDataHandler(columndesc, targets);
    }

    /**
     * Get the Column entity which column name is "active_timeout" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getActiveTimeoutColumn() {
        ColumnDescription columndesc = new ColumnDescription(NetflowColumn.ACTIVETIMEOUT.columnName(),
                                                             "getActiveTimeoutColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "active_timeout" to the Row
     * entity of attributes.
     * @param activeTimeout the column data which column name is
     *            "active_timeout"
     */
    public void setActiveTimeout(Long activeTimeout) {
        ColumnDescription columndesc = new ColumnDescription(NetflowColumn.ACTIVETIMEOUT.columnName(),
                                                             "setActiveTimeout", VersionNum.VERSION100);
        super.setDataHandler(columndesc, activeTimeout);
    }

    /**
     * Get the Column entity which column name is "engine_type" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getEngineTypeColumn() {
        ColumnDescription columndesc = new ColumnDescription(NetflowColumn.ENGINETYPE.columnName(),
                                                             "getEngineTypeColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "engine_type" to the Row entity
     * of attributes.
     * @param engineType the column data which column name is "engine_type"
     */
    public void setEngineType(Set<Long> engineType) {
        ColumnDescription columndesc = new ColumnDescription(NetflowColumn.ENGINETYPE.columnName(),
                                                             "setEngineType", VersionNum.VERSION100);
        super.setDataHandler(columndesc, engineType);
    }

    /**
     * Get the Column entity which column name is "external_ids" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getExternalIdsColumn() {
        ColumnDescription columndesc = new ColumnDescription(NetflowColumn.EXTERNALIDS.columnName(),
                                                             "getExternalIdsColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "external_ids" to the Row entity
     * of attributes.
     * @param externalIds the column data which column name is "external_ids"
     */
    public void setExternalIds(Map<String, String> externalIds) {
        ColumnDescription columndesc = new ColumnDescription(NetflowColumn.EXTERNALIDS.columnName(),
                                                             "setExternalIds", VersionNum.VERSION100);
        super.setDataHandler(columndesc, externalIds);
    }

    /**
     * Get the Column entity which column name is "add_id_to_interface" from the
     * Row entity of attributes.
     * @return the Column entity
     */
    public Column getAddIdToInterfaceColumn() {
        ColumnDescription columndesc = new ColumnDescription(NetflowColumn.ADDIDTOINTERFACE.columnName(),
                                                             "getAddIdToInterfaceColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "add_id_to_interface" to the Row
     * entity of attributes.
     * @param addIdToInterface the column data which column name is
     *            "add_id_to_interface"
     */
    public void setAddIdToInterface(Boolean addIdToInterface) {
        ColumnDescription columndesc = new ColumnDescription(NetflowColumn.ADDIDTOINTERFACE.columnName(),
                                                             "setAddIdToInterface", VersionNum.VERSION100);
        super.setDataHandler(columndesc, addIdToInterface);
    }

    /**
     * Get the Column entity which column name is "engine_id" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getEngineIdColumn() {
        ColumnDescription columndesc = new ColumnDescription(NetflowColumn.ENGINETYPE.columnName(),
                                                             "getEngineIdColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "engine_id" to the Row entity of
     * attributes.
     * @param engineId the column data which column name is "engine_id"
     */
    public void setEngineId(Set<Long> engineId) {
        ColumnDescription columndesc = new ColumnDescription(NetflowColumn.ENGINETYPE.columnName(),
                                                             "setEngineId", VersionNum.VERSION100);
        super.setDataHandler(columndesc, engineId);
    }

}
