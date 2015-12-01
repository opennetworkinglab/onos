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
package org.onosproject.ovsdb.rfc.table;

import org.onosproject.ovsdb.rfc.notation.Column;
import org.onosproject.ovsdb.rfc.notation.OvsdbSet;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.notation.UUID;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.tableservice.AbstractOvsdbTableService;
import org.onosproject.ovsdb.rfc.tableservice.ColumnDescription;

import java.util.Map;
import java.util.Set;

/**
 * This class provides operations of Bridge Table.
 */
public class Bridge extends AbstractOvsdbTableService {

    /**
     * Bridge table column name.
     */
    public enum BridgeColumn {
        NAME("name"), DATAPATHTYPE("datapath_type"), DATAPATHID("datapath_id"),
        STPENABLE("stpenable"), PORTS("ports"), MIRRORS("mirrors"),
        NETFLOW("netflow"), SFLOW("sflow"), IPFIX("ipfix"),
        CONTROLLER("controller"), PROTOCOLS("protocols"),
        FAILMODE("fail_mode"), STATUS("status"), OTHERCONFIG("other_config"),
        EXTERNALIDS("external_ids"), FLOODVLANS("flood_vlans"),
        FLOWTABLES("flow_tables");

        private final String columnName;

        private BridgeColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for BridgeColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a Bridge object. Generate Bridge Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public Bridge(DatabaseSchema dbSchema, Row row) {
        super(dbSchema, row, OvsdbTable.BRIDGE, VersionNum.VERSION100);
    }

    /**
     * Get the Column entity which column name is "name" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "name"
     */
    public Column getNameColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.NAME
                                                                     .columnName(),
                                                             "getNameColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "name" to the Row entity of
     * attributes.
     * @param name the column data which column name is "name"
     */
    public void setName(String name) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.NAME
                                                                     .columnName(),
                                                             "setName",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, name);
    }

    /**
     * Get the column data which column name is "name" from the Row entity of
     * attributes.
     * @return the column data which column name is "name"
     */
    public String getName() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.NAME
                                                                     .columnName(),
                                                             "getName",
                                                             VersionNum.VERSION100);
        return (String) super.getDataHandler(columndesc);
    }

    /**
     * Get the Column entity which column name is "datapath_type" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "datapath_type"
     */
    public Column getDatapathTypeColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.DATAPATHTYPE
                                                                     .columnName(),
                                                             "getDatapathTypeColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "datapath_type" to the Row
     * entity of attributes.
     * @param datapathType the column data which column name is "datapath_type"
     */
    public void setDatapathType(String datapathType) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.DATAPATHTYPE
                                                                     .columnName(),
                                                             "setDatapathType",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, datapathType);
    }

    /**
     * Get the Column entity which column name is "datapath_id" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "datapath_id"
     */
    public Column getDatapathIdColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.DATAPATHID
                                                                     .columnName(),
                                                             "getDatapathIdColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "datapath_id" to the Row entity
     * of attributes.
     * @param datapathId the column data which column name is "datapath_id"
     */
    public void setDatapathId(Set<String> datapathId) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.DATAPATHID
                                                                     .columnName(),
                                                             "setDatapathId",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, datapathId);
    }

    /**
     * Get the Column entity which column name is "stpenable" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "stpenable"
     */
    public Column getStpEnableColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.STPENABLE
                                                                     .columnName(),
                                                             "getStpEnableColumn",
                                                             VersionNum.VERSION620);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "stpenable" to the Row entity of
     * attributes.
     * @param stpenable the column data which column name is "stpenable"
     */
    public void setStpEnable(Boolean stpenable) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.STPENABLE
                                                                     .columnName(),
                                                             "setStpEnable",
                                                             VersionNum.VERSION620);
        super.setDataHandler(columndesc, stpenable);
    }

    /**
     * Get the Column entity which column name is "ports" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "ports"
     */
    public Column getPortsColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.PORTS
                                                                     .columnName(),
                                                             "getPortsColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "ports" to the Row entity of
     * attributes.
     * @param ports the column data which column name is "ports"
     */
    public void setPorts(Set<UUID> ports) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.PORTS
                                                                     .columnName(),
                                                             "setPorts",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, ports);
    }

    /**
     * Get the Column entity which column name is "mirrors" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "mirrors"
     */
    public Column getMirrorsColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.MIRRORS
                                                                     .columnName(),
                                                             "getMirrorsColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "mirrors" to the Row entity of
     * attributes.
     * @param mirrors the column data which column name is "mirrors"
     */
    public void setMirrors(Set<UUID> mirrors) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.MIRRORS
                                                                     .columnName(),
                                                             "setMirrors",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, mirrors);
    }

    /**
     * Get the Column entity which column name is "netflow" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "netflow"
     */
    public Column getNetflowColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.NETFLOW
                                                                     .columnName(),
                                                             "getNetflowColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "netflow" to the Row entity of
     * attributes.
     * @param netflow the column data which column name is "netflow"
     */
    public void setNetflow(Set<UUID> netflow) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.NETFLOW
                                                                     .columnName(),
                                                             "setNetflow",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, netflow);
    }

    /**
     * Get the Column entity which column name is "sflow" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "sflow"
     */
    public Column getSflowColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.SFLOW
                                                                     .columnName(),
                                                             "getSflowColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "sflow" to the Row entity of
     * attributes.
     * @param sflow the column data which column name is "sflow"
     */
    public void setSflow(Set<UUID> sflow) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.SFLOW
                                                                     .columnName(),
                                                             "setSflow",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, sflow);
    }

    /**
     * Get the Column entity which column name is "ipfix" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "ipfix"
     */
    public Column getIpfixColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.IPFIX
                                                                     .columnName(),
                                                             "getIpfixColumn",
                                                             VersionNum.VERSION710);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "ipfix" to the Row entity of
     * attributes.
     * @param ipfix the column data which column name is "ipfix"
     */
    public void setIpfix(Set<UUID> ipfix) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.IPFIX
                                                                     .columnName(),
                                                             "setIpfix",
                                                             VersionNum.VERSION710);
        super.setDataHandler(columndesc, ipfix);
    }

    /**
     * Get the Column entity which column name is "controller" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "controller"
     */
    public Column getControllerColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.CONTROLLER
                                                                     .columnName(),
                                                             "getControllerColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "controller" to the Row entity
     * of attributes.
     * @param controller the column data which column name is "controller"
     */
    public void setController(OvsdbSet controller) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.CONTROLLER
                                                                     .columnName(),
                                                             "setController",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, controller);
    }

    /**
     * Get the Column entity which column name is "protocols" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "protocols"
     */
    public Column getProtocolsColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.PROTOCOLS
                                                                     .columnName(),
                                                             "getProtocolsColumn",
                                                             VersionNum.VERSION6111);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "protocols" to the Row entity of
     * attributes.
     * @param protocols the column data which column name is "protocols"
     */
    public void setProtocols(Set<String> protocols) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.PROTOCOLS
                                                                     .columnName(),
                                                             "setProtocols",
                                                             VersionNum.VERSION6111);
        super.setDataHandler(columndesc, protocols);
    }

    /**
     * Get the Column entity which column name is "fail_mode" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "fail_mode"
     */
    public Column getFailModeColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.FAILMODE
                                                                     .columnName(),
                                                             "getFailModeColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "fail_mode" to the Row entity of
     * attributes.
     * @param failMode the column data which column name is "fail_mode"
     */
    public void setFailMode(Set<String> failMode) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.FAILMODE
                                                                     .columnName(),
                                                             "setFailMode",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, failMode);
    }

    /**
     * Get the Column entity which column name is "status" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "status"
     */
    public Column getStatusColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.STATUS
                                                                     .columnName(),
                                                             "getStatusColumn",
                                                             VersionNum.VERSION620);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "status" to the Row entity of
     * attributes.
     * @param status the column data which column name is "status"
     */
    public void setStatus(Map<String, String> status) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.STATUS
                                                                     .columnName(),
                                                             "setStatus",
                                                             VersionNum.VERSION620);
        super.setDataHandler(columndesc, status);
    }

    /**
     * Get the Column entity which column name is "other_config" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "other_config"
     */
    public Column getOtherConfigColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.OTHERCONFIG
                                                                     .columnName(),
                                                             "getOtherConfigColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "other_config" to the Row entity
     * of attributes.
     * @param otherConfig the column data which column name is "other_config"
     */
    public void setOtherConfig(Map<String, String> otherConfig) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.OTHERCONFIG
                                                                     .columnName(),
                                                             "setOtherConfig",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, otherConfig);
    }

    /**
     * Get the Column entity which column name is "external_ids" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "external_ids"
     */
    public Column getExternalIdsColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.EXTERNALIDS
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
                                                             BridgeColumn.EXTERNALIDS
                                                                     .columnName(),
                                                             "setExternalIds",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, externalIds);
    }

    /**
     * Get the Column entity which column name is "flood_vlans" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "flood_vlans"
     */
    public Column getFloodVlansColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.FLOODVLANS
                                                                     .columnName(),
                                                             "getFloodVlansColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "flood_vlans" to the Row entity
     * of attributes.
     * @param vlans the column data which column name is "flood_vlans"
     */
    public void setFloodVlans(Set<Long> vlans) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.FLOODVLANS
                                                                     .columnName(),
                                                             "setFloodVlans",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, vlans);
    }

    /**
     * Get the Column entity which column name is "flow_tables" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "flow_tables"
     */
    public Column getFlowTablesColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.FLOWTABLES
                                                                     .columnName(),
                                                             "getFlowTablesColumn",
                                                             VersionNum.VERSION650);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "flow_tables" to the Row entity
     * of attributes.
     * @param flowTables the column data which column name is "flow_tables"
     */
    public void setFlowTables(Map<Long, UUID> flowTables) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             BridgeColumn.FLOWTABLES
                                                                     .columnName(),
                                                             "setFlowTables",
                                                             VersionNum.VERSION650);
        super.setDataHandler(columndesc, flowTables);
    }

}
