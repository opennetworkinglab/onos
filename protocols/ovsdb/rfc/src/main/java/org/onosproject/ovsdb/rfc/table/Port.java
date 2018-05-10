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
 * This class provides operations of Port Table.
 */
public class Port extends AbstractOvsdbTableService {

    /**
     * Port table column name.
     */
    public enum PortColumn {
        NAME("name"), INTERFACES("interfaces"), TRUNKS("trunks"), TAG("tag"),
        VLANMODE("vlan_mode"), QOS("qos"), MAC("mac"), BONDTYPE("bond_type"),
        BONDMODE("bond_mode"), LACP("lacp"), BONDUPDELAY("bond_updelay"),
        BONDDOWNDELAY("bond_downdelay"), BONDFAKEIFACE("bond_fake_iface"),
        FAKEBRIDGE("fake_bridge"), STATUS("status"), STATISTICS("statistics"),
        OTHERCONFIG("other_config"), EXTERNALIDS("external_ids");

        private final String columnName;

        PortColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for PortColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a Port object. Generate Port Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public Port(DatabaseSchema dbSchema, Row row) {
        super(dbSchema, row, OvsdbTable.PORT, VersionNum.VERSION100);
    }

    /**
     * Get the Column entity which column name is "name" from the Row entity of
     * attributes.
     * @return the Column entity
     */
    public Column getNameColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.NAME
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
                                                             PortColumn.NAME
                                                                     .columnName(),
                                                             "setName",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, name);
    }

    /**
     * Get the Column entity which column name is "name" from the Row entity of
     * attributes.
     * @return the Column entity
     */
    public String getName() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.NAME
                                                                     .columnName(),
                                                             "getName",
                                                             VersionNum.VERSION100);
        return (String) super.getDataHandler(columndesc);
    }

    /**
     * Get the Column entity which column name is "interfaces" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getInterfacesColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.INTERFACES
                                                                     .columnName(),
                                                             "getInterfacesColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "interfaces" to the Row entity
     * of attributes.
     * @param interfaces the column data which column name is "interfaces"
     */
    public void setInterfaces(Set<Uuid> interfaces) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.INTERFACES
                                                                     .columnName(),
                                                             "setInterfaces",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, interfaces);
    }

    /**
     * Get the Column entity which column name is "trunks" from the Row entity
     * of attributes.
     * @return the Column entity
     */
    public Column getTrunksColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.TRUNKS
                                                                     .columnName(),
                                                             "getTrunksColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "trunks" to the Row entity of
     * attributes.
     * @param trunks the column data which column name is "trunks"
     */
    public void setTrunks(Set<Long> trunks) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.TRUNKS
                                                                     .columnName(),
                                                             "setTrunks",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, trunks);
    }

    /**
     * Get the Column entity which column name is "tag" from the Row entity of
     * attributes.
     * @return the Column entity
     */
    public Column getTagColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.TAG
                                                                     .columnName(),
                                                             "getTagColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "tag" to the Row entity of
     * attributes.
     * @param tag the column data which column name is "tag"
     */
    public void setTag(Set<Long> tag) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.TAG
                                                                     .columnName(),
                                                             "setTag",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, tag);
    }

    /**
     * Get the Column entity which column name is "vlan_mode" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getVlanModeColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.VLANMODE
                                                                     .columnName(),
                                                             "getVlanModeColumn",
                                                             VersionNum.VERSION610);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "vlan_mode" to the Row entity of
     * attributes.
     * @param vlanMode the column data which column name is "vlan_mode"
     */
    public void setVlanMode(Set<String> vlanMode) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.VLANMODE
                                                                     .columnName(),
                                                             "setVlanMode",
                                                             VersionNum.VERSION610);
        super.setDataHandler(columndesc, vlanMode);
    }

    /**
     * Get the Column entity which column name is "qos" from the Row entity of
     * attributes.
     * @return the Column entity
     */
    public Column getQosColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.QOS
                                                                     .columnName(),
                                                             "getQosColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "qos" to the Row entity of
     * attributes.
     * @param qos the column data which column name is "qos"
     */
    public void setQos(Uuid qos) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.QOS
                                                                     .columnName(),
                                                             "setQos",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, qos);
    }

    /**
     * Get the Column entity which column name is "mac" from the Row entity of
     * attributes.
     * @return the Column entity
     */
    public Column getMacColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.MAC
                                                                     .columnName(),
                                                             "getMacColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "mac" to the Row entity of
     * attributes.
     * @param mac the column data which column name is "mac"
     */
    public void setMac(Set<String> mac) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.MAC
                                                                     .columnName(),
                                                             "setMac",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, mac);
    }

    /**
     * Get the Column entity which column name is "bond_type" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getBondTypeColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.BONDTYPE
                                                                     .columnName(),
                                                             "getBondTypeColumn",
                                                             VersionNum.VERSION102,
                                                             VersionNum.VERSION103);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "bond_type" to the Row entity of
     * attributes.
     * @param bondtype the column data which column name is "bond_type"
     */
    public void setBondType(Set<String> bondtype) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.BONDTYPE
                                                                     .columnName(),
                                                             "setBondType",
                                                             VersionNum.VERSION102,
                                                             VersionNum.VERSION103);
        super.setDataHandler(columndesc, bondtype);
    }

    /**
     * Get the Column entity which column name is "bond_mode" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getBondModeColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.BONDMODE
                                                                     .columnName(),
                                                             "getBondModeColumn",
                                                             VersionNum.VERSION104);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "bond_mode" to the Row entity of
     * attributes.
     * @param bondmode the column data which column name is "bond_mode"
     */
    public void setBondMode(Set<String> bondmode) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.BONDMODE
                                                                     .columnName(),
                                                             "setBondMode",
                                                             VersionNum.VERSION104);
        super.setDataHandler(columndesc, bondmode);
    }

    /**
     * Get the Column entity which column name is "lacp" from the Row entity of
     * attributes.
     * @return the Column entity
     */
    public Column getLacpColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.LACP
                                                                     .columnName(),
                                                             "getLacpColumn",
                                                             VersionNum.VERSION130);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "lacp" to the Row entity of
     * attributes.
     * @param lacp the column data which column name is "lacp"
     */
    public void setLacp(Set<String> lacp) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.LACP
                                                                     .columnName(),
                                                             "setLacp",
                                                             VersionNum.VERSION130);
        super.setDataHandler(columndesc, lacp);
    }

    /**
     * Get the Column entity which column name is "bond_updelay" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getBondUpDelayColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.BONDUPDELAY
                                                                     .columnName(),
                                                             "getBondUpDelayColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "bond_updelay" to the Row entity
     * of attributes.
     * @param bondUpDelay the column data which column name is "bond_updelay"
     */
    public void setBondUpDelay(Set<Long> bondUpDelay) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.BONDUPDELAY
                                                                     .columnName(),
                                                             "setBondUpDelay",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, bondUpDelay);
    }

    /**
     * Get the Column entity which column name is "bond_downdelay" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getBondDownDelayColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.BONDDOWNDELAY
                                                                     .columnName(),
                                                             "getBondDownDelayColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "bond_downdelay" to the Row
     * entity of attributes.
     * @param bondDownDelay the column data which column name is
     *            "bond_downdelay"
     */
    public void setBondDownDelay(Set<Long> bondDownDelay) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.BONDDOWNDELAY
                                                                     .columnName(),
                                                             "setBondDownDelay",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, bondDownDelay);
    }

    /**
     * Get the Column entity which column name is "bond_fake_iface" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getBondFakeInterfaceColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.BONDFAKEIFACE
                                                                     .columnName(),
                                                             "getBondFakeInterfaceColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "bond_fake_iface" to the Row
     * entity of attributes.
     * @param bondFakeInterface the column data which column name is
     *            "bond_fake_iface"
     */
    public void setBondFakeInterface(Set<Boolean> bondFakeInterface) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.BONDFAKEIFACE
                                                                     .columnName(),
                                                             "setBondFakeInterface",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, bondFakeInterface);
    }

    /**
     * Get the Column entity which column name is "fake_bridge" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getFakeBridgeColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.FAKEBRIDGE
                                                                     .columnName(),
                                                             "getFakeBridgeColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "fake_bridge" to the Row entity
     * of attributes.
     * @param fakeBridge the column data which column name is "fake_bridge"
     */
    public void setFakeBridge(Set<Boolean> fakeBridge) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.FAKEBRIDGE
                                                                     .columnName(),
                                                             "setFakeBridge",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, fakeBridge);
    }

    /**
     * Get the Column entity which column name is "status" from the Row entity
     * of attributes.
     * @return the Column entity
     */
    public Column getStatusColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.STATUS
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
                                                             PortColumn.STATUS
                                                                     .columnName(),
                                                             "setStatus",
                                                             VersionNum.VERSION620);
        super.setDataHandler(columndesc, status);
    }

    /**
     * Get the Column entity which column name is "statistics" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getStatisticsColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.STATISTICS
                                                                     .columnName(),
                                                             "getStatisticsColumn",
                                                             VersionNum.VERSION630);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "statistics" to the Row entity
     * of attributes.
     * @param statistics the column data which column name is "statistics"
     */
    public void setStatistics(Map<String, Long> statistics) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.STATISTICS
                                                                     .columnName(),
                                                             "setStatistics",
                                                             VersionNum.VERSION630);
        super.setDataHandler(columndesc, statistics);
    }

    /**
     * Get the Column entity which column name is "other_config" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getOtherConfigColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.OTHERCONFIG
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
                                                             PortColumn.OTHERCONFIG
                                                                     .columnName(),
                                                             "setOtherConfig",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, otherConfig);
    }

    /**
     * Get the Column entity which column name is "external_ids" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getExternalIdsColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             PortColumn.EXTERNALIDS
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
                                                             PortColumn.EXTERNALIDS
                                                                     .columnName(),
                                                             "setExternalIds",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, externalIds);
    }

}
