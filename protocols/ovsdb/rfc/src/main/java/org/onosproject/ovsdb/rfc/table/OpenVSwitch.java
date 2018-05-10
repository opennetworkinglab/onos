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
import org.onosproject.ovsdb.rfc.notation.Uuid;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.tableservice.AbstractOvsdbTableService;
import org.onosproject.ovsdb.rfc.tableservice.ColumnDescription;

/**
 * This class provides operations of Open_vSwitch Table.
 */
public class OpenVSwitch extends AbstractOvsdbTableService {

    /**
     * OpenVSwitch table column name.
     */
    public enum OpenVSwitchColumn {
        BRIDGES("bridges"), MANAGERS("managers"),
        MANAGEROPTIONS("manager_options"), SSL("ssl"),
        OTHERCONFIG("other_config"), EXTERNALIDS("external_ids"),
        NEXTCFG("next_cfg"), CURCFG("cur_cfg"), CAPABILITIES("capabilities"),
        STATISTICS("statistics"), OVSVERSION("ovs_version"),
        DBVERSION("db_version"), SYSTEMTYPE("system_type"),
        SYSTEMVERSION("system_version");

        private final String columnName;

        OpenVSwitchColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for OpenVSwitchColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a OpenVSwitch object. Generate Open_vSwitch Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public OpenVSwitch(DatabaseSchema dbSchema, Row row) {
        super(dbSchema, row, OvsdbTable.OPENVSWITCH, VersionNum.VERSION100);
    }

    /**
     * Get the Column entity which column name is "bridges" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "bridges"
     */
    public Column getBridgesColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.BRIDGES
                                                                     .columnName(),
                                                             "getBridgesColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "bridges" to the Row entity of
     * attributes.
     * @param bridges the column data which column name is "bridges"
     */
    public void setBridges(Set<Uuid> bridges) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.BRIDGES
                                                                     .columnName(),
                                                             "setBridges",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, bridges);
    }

    /**
     * Get the Column entity which column name is "managers" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "managers"
     */
    public Column getManagersColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.MANAGERS
                                                                     .columnName(),
                                                             "getManagersColumn",
                                                             VersionNum.VERSION100,
                                                             VersionNum.VERSION200);
        return (Column) super.getDataHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "managers" to the Row entity of
     * attributes.
     * @param managers the column data which column name is "managers"
     */
    public void setManagers(Set<Uuid> managers) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.MANAGERS
                                                                     .columnName(),
                                                             "setManagers",
                                                             VersionNum.VERSION100,
                                                             VersionNum.VERSION200);
        super.setDataHandler(columndesc, managers);
    }

    /**
     * Get the Column entity which column name is "manager_options" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "manager_options"
     */
    public Column getManagerOptionsColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.MANAGEROPTIONS
                                                                     .columnName(),
                                                             "getManagerOptionsColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getDataHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "manager_options" to the Row
     * entity of attributes.
     * @param managerOptions the column data which column name is
     *            "manager_options"
     */
    public void setManagerOptions(Set<Uuid> managerOptions) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.MANAGEROPTIONS
                                                                     .columnName(),
                                                             "setManagerOptions",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, managerOptions);
    }

    /**
     * Get the Column entity which column name is "ssl" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "ssl"
     */
    public Column getSslColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.SSL
                                                                     .columnName(),
                                                             "getSslColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getDataHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "ssl" to the Row entity of
     * attributes.
     * @param ssl the column data which column name is "ssl"
     */
    public void setSsl(Set<Uuid> ssl) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.SSL
                                                                     .columnName(),
                                                             "setSsl",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, ssl);
    }

    /**
     * Get the Column entity which column name is "other_config" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "other_config"
     */
    public Column getOtherConfigColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.OTHERCONFIG
                                                                     .columnName(),
                                                             "getOtherConfigColumn",
                                                             VersionNum.VERSION510);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "other_config" to the Row entity
     * of attributes.
     * @param otherConfig the column data which column name is "other_config"
     */
    public void setOtherConfig(Map<String, String> otherConfig) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.OTHERCONFIG
                                                                     .columnName(),
                                                             "setOtherConfig",
                                                             VersionNum.VERSION510);
        super.setDataHandler(columndesc, otherConfig);
    }

    /**
     * Get the Column entity which column name is "external_ids" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "external_ids"
     */
    public Column getExternalIdsColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.EXTERNALIDS
                                                                     .columnName(),
                                                             "getExternalIdsColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "external_ids" to the Row entity
     * of attributes.
     * @param externalIds the column data which column name is "external_ids"
     */
    public void setExternalIds(Map<String, String> externalIds) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.EXTERNALIDS
                                                                     .columnName(),
                                                             "setExternalIds",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, externalIds);
    }

    /**
     * Get the Column entity which column name is "next_cfg" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "next_cfg"
     */
    public Column getNextConfigColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.NEXTCFG
                                                                     .columnName(),
                                                             "getNextConfigColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "next_cfg" to the Row entity of
     * attributes.
     * @param nextConfig the column data which column name is "next_cfg"
     */
    public void setNextConfig(Long nextConfig) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.NEXTCFG
                                                                     .columnName(),
                                                             "setNextConfig",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, nextConfig);
    }

    /**
     * Get the Column entity which column name is "cur_cfg" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "cur_cfg"
     */
    public Column getCurrentConfigColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.CURCFG
                                                                     .columnName(),
                                                             "getCurrentConfigColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "cur_cfg" to the Row entity of
     * attributes.
     * @param currentConfig the column data which column name is "cur_cfg"
     */
    public void setCurrentConfig(Long currentConfig) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.CURCFG
                                                                     .columnName(),
                                                             "setCurrentConfig",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, currentConfig);
    }

    /**
     * Get the Column entity which column name is "capabilities" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "capabilities"
     */
    public Column getCapabilitiesColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.CAPABILITIES
                                                                     .columnName(),
                                                             "getCapabilitiesColumn",
                                                             VersionNum.VERSION100,
                                                             VersionNum.VERSION670);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "capabilities" to the Row entity
     * of attributes.
     * @param capabilities the column data which column name is "capabilities"
     */
    public void setCapabilities(Map<String, Uuid> capabilities) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.CAPABILITIES
                                                                     .columnName(),
                                                             "setCapabilities",
                                                             VersionNum.VERSION100,
                                                             VersionNum.VERSION670);
        super.setDataHandler(columndesc, capabilities);
    }

    /**
     * Get the Column entity which column name is "statistics" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "statistics"
     */
    public Column getStatisticsColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.STATISTICS
                                                                     .columnName(),
                                                             "getStatisticsColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "statistics" to the Row entity
     * of attributes.
     * @param statistics the column data which column name is "statistics"
     */
    public void setStatistics(Map<String, Long> statistics) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.STATISTICS
                                                                     .columnName(),
                                                             "setStatistics",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, statistics);
    }

    /**
     * Get the Column entity which column name is "ovs_version" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "ovs_version"
     */
    public Column getOvsVersionColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.OVSVERSION
                                                                     .columnName(),
                                                             "getOvsVersionColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "ovs_version" to the Row entity
     * of attributes.
     * @param ovsVersion the column data which column name is "ovs_version"
     */
    public void setOvsVersion(Set<String> ovsVersion) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.OVSVERSION
                                                                     .columnName(),
                                                             "setOvsVersion",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, ovsVersion);
    }

    /**
     * Get the Column entity which column name is "db_version" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "db_version"
     */
    public Column getDbVersionColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.DBVERSION
                                                                     .columnName(),
                                                             "getDbVersionColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "db_version" to the Row entity
     * of attributes.
     * @param dbVersion the column data which column name is "db_version"
     */
    public void setDbVersion(Set<String> dbVersion) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.DBVERSION
                                                                     .columnName(),
                                                             "setDbVersion",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, dbVersion);
    }

    /**
     * Get the Column entity which column name is "system_type" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "system_type"
     */
    public Column getSystemTypeColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.SYSTEMTYPE
                                                                     .columnName(),
                                                             "getSystemTypeColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "system_type" to the Row entity
     * of attributes.
     * @param systemType the column data which column name is "system_type"
     */
    public void setSystemType(Set<String> systemType) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.SYSTEMTYPE
                                                                     .columnName(),
                                                             "setSystemType",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, systemType);
    }

    /**
     * Get the Column entity which column name is "system_version" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "system_version"
     */
    public Column getSystemVersionColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.SYSTEMVERSION
                                                                     .columnName(),
                                                             "getSystemVersionColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "system_version" to the Row
     * entity of attributes.
     * @param systemVersion the column data which column name is
     *            "system_version"
     */
    public void setSystemVersion(Set<String> systemVersion) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             OpenVSwitchColumn.SYSTEMVERSION
                                                                     .columnName(),
                                                             "setSystemVersion",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, systemVersion);
    }
}
