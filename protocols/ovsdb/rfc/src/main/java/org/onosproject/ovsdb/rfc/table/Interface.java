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
package org.onosproject.ovsdb.rfc.table;

import java.util.Map;
import java.util.Set;

import org.onosproject.ovsdb.rfc.notation.Column;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.tableservice.AbstractOvsdbTableService;
import org.onosproject.ovsdb.rfc.tableservice.ColumnDescription;

/**
 * This class provides operations of Interface Table.
 */
public class Interface extends AbstractOvsdbTableService {

    /**
     * Interface table column name.
     */
    public enum InterfaceColumn {
        NAME("name"), TYPE("type"), OPTIONS("options"),
        INGRESSPOLICINGRATE("ingress_policing_rate"),
        INGRESSPOLICINGBURST("ingress_policing_burst"), MACINUSE("mac_in_use"),
        MAC("mac"), IFINDEX("ifindex"), EXTERNALIDS("external_ids"),
        OFPORT("ofport"), OFPORTREQUEST("ofport_request"), BFD("bfd"),
        BFDSTATUS("bfd_status"), MONITOR("monitor"), CFMMPID("cfm_mpid"),
        CFMREMOTEMPID("cfm_remote_mpid"), CFMREMOTEMPIDS("cfm_remote_mpids"),
        CFMFLAPCOUNT("cfm_flap_count"), CFMFAULT("cfm_fault"),
        CFMFAULTSTATUS("cfm_fault_status"),
        CFMREMOTEOPSTATE("cfm_remote_opstate"), CFMHEALTH("cfm_health"),
        LACPCURRENT("lacp_current"), OTHERCONFIG("other_config"),
        STATISTICS("statistics"), STATUS("status"), ADMINSTATE("admin_state"),
        LINKSTATE("link_state"), LINKRESETS("link_resets"),
        LINKSPEED("link_speed"), DUPLEX("duplex"), MTU("mtu"), MTU_REQUEST("mtu_request"), ERROR("error");

        private final String columnName;

        private InterfaceColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for InterfaceColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a Interface object. Generate Interface Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public Interface(DatabaseSchema dbSchema, Row row) {
        super(dbSchema, row, OvsdbTable.INTERFACE, VersionNum.VERSION100);
    }

    /**
     * Get the Column entity which column name is "name" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "name"
     */
    public Column getNameColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.NAME
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
                                                             InterfaceColumn.NAME
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
                                                             InterfaceColumn.NAME
                                                                     .columnName(),
                                                             "getName",
                                                             VersionNum.VERSION100);
        return (String) super.getDataHandler(columndesc);
    }

    /**
     * Get the Column entity which column name is "type" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "type"
     */
    public Column getTypeColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.TYPE
                                                                     .columnName(),
                                                             "getTypeColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "type" to the Row entity of
     * attributes.
     * @param type the column data which column name is "type"
     */
    public void setType(String type) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.TYPE
                                                                     .columnName(),
                                                             "setType",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, type);
    }

    /**
     * Get the Column entity which column name is "options" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "options"
     */
    public Column getOptionsColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.OPTIONS
                                                                     .columnName(),
                                                             "getOptionsColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "options" to the Row entity of
     * attributes.
     * @param options the column data which column name is "options"
     */
    public void setOptions(Map<String, String> options) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.OPTIONS
                                                                     .columnName(),
                                                             "setOptions",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, options);
    }

    /**
     * Get the Column entity which column name is "ingress_policing_rate" from
     * the Row entity of attributes.
     * @return the Column entity which column name is "ingress_policing_rate"
     */
    public Column getIngressPolicingRateColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.INGRESSPOLICINGRATE
                                                                     .columnName(),
                                                             "getIngressPolicingRateColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "ingress_policing_rate" to the
     * Row entity of attributes.
     * @param ingressPolicingRate the column data which column name is
     *            "ingress_policing_rate"
     */
    public void setIngressPolicingRate(Set<Long> ingressPolicingRate) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.INGRESSPOLICINGRATE
                                                                     .columnName(),
                                                             "setIngressPolicingRate",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, ingressPolicingRate);
    }

    /**
     * Get the Column entity which column name is "ingress_policing_burst" from
     * the Row entity of attributes.
     * @return the Column entity which column name is "ingress_policing_burst"
     */
    public Column getIngressPolicingBurstColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.INGRESSPOLICINGBURST
                                                                     .columnName(),
                                                             "getIngressPolicingBurstColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "ingress_policing_burst" to the
     * Row entity of attributes.
     * @param ingressPolicingBurst the column data which column name is
     *            "ingress_policing_burst"
     */
    public void setIngressPolicingBurst(Set<Long> ingressPolicingBurst) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.INGRESSPOLICINGBURST
                                                                     .columnName(),
                                                             "setIngressPolicingBurst",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, ingressPolicingBurst);
    }

    /**
     * Get the Column entity which column name is "mac_in_use" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "mac_in_use"
     */
    public Column getMacInUseColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.MACINUSE
                                                                     .columnName(),
                                                             "getMacInUseColumn",
                                                             VersionNum.VERSION710);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "mac_in_use" to the Row entity
     * of attributes.
     * @param macInUse the column data which column name is "mac_in_use"
     */
    public void setMacInUse(Set<String> macInUse) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.MACINUSE
                                                                     .columnName(),
                                                             "setMacInUse",
                                                             VersionNum.VERSION710);
        super.setDataHandler(columndesc, macInUse);
    }

    /**
     * Get the Column entity which column name is "mac" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "mac"
     */
    public Column getMacColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.MAC
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
                                                             InterfaceColumn.MAC
                                                                     .columnName(),
                                                             "setMac",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, mac);
    }

    /**
     * Get the Column entity which column name is "ifindex" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "ifindex"
     */
    public Column getIfIndexColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.IFINDEX
                                                                     .columnName(),
                                                             "getIfIndexColumn",
                                                             VersionNum.VERSION721);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "ifindex" to the Row entity of
     * attributes.
     * @param ifIndex the column data which column name is "ifindex"
     */
    public void setIfIndex(Long ifIndex) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.IFINDEX
                                                                     .columnName(),
                                                             "setIfIndex",
                                                             VersionNum.VERSION721);
        super.setDataHandler(columndesc, ifIndex);
    }

    /**
     * Get the Column entity which column name is "external_ids" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "external_ids"
     */
    public Column getExternalIdsColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.EXTERNALIDS
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
                                                             InterfaceColumn.EXTERNALIDS
                                                                     .columnName(),
                                                             "setExternalIds",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, externalIds);
    }

    /**
     * Get the Column entity which column name is "ofport" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "ofport"
     */
    public Column getOpenFlowPortColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.OFPORT
                                                                     .columnName(),
                                                             "getOpenFlowPortColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "ofport" to the Row entity of
     * attributes.
     * @param openFlowPort the column data which column name is "ofport"
     */
    public void setOpenFlowPort(Set<Long> openFlowPort) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.OFPORT
                                                                     .columnName(),
                                                             "setOpenFlowPort",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, openFlowPort);
    }

    /**
     * Get the Column entity which column name is "ofport_request" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "ofport_request"
     */
    public Column getOpenFlowPortRequestColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.OFPORTREQUEST
                                                                     .columnName(),
                                                             "getOpenFlowPortRequestColumn",
                                                             VersionNum.VERSION620);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "ofport_request" to the Row
     * entity of attributes.
     * @param openFlowPortRequest the column data which column name is
     *            "ofport_request"
     */
    public void setOpenFlowPortRequest(String openFlowPortRequest) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.OFPORTREQUEST
                                                                     .columnName(),
                                                             "setOpenFlowPortRequest",
                                                             VersionNum.VERSION620);
        super.setDataHandler(columndesc, openFlowPortRequest);
    }

    /**
     * Get the Column entity which column name is "bfd" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "bfd"
     */
    public Column getBfdColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.BFD
                                                                     .columnName(),
                                                             "getBfdColumn",
                                                             VersionNum.VERSION720);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "bfd" to the Row entity of
     * attributes.
     * @param bfd the column data which column name is "bfd"
     */
    public void setBfd(Map<String, String> bfd) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.BFD
                                                                     .columnName(),
                                                             "setBfd",
                                                             VersionNum.VERSION720);
        super.setDataHandler(columndesc, bfd);
    }

    /**
     * Get the Column entity which column name is "bfd_status" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "bfd_status"
     */
    public Column getBfdStatusColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.BFDSTATUS
                                                                     .columnName(),
                                                             "getBfdStatusColumn",
                                                             VersionNum.VERSION720);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "bfd_status" to the Row entity
     * of attributes.
     * @param bfdStatus the column data which column name is "bfd_status"
     */
    public void setBfdStatus(Map<String, String> bfdStatus) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.BFDSTATUS
                                                                     .columnName(),
                                                             "setBfdStatus",
                                                             VersionNum.VERSION720);
        super.setDataHandler(columndesc, bfdStatus);
    }

    /**
     * Get the Column entity which column name is "monitor" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "monitor"
     */
    public Column getMonitorColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.MONITOR
                                                                     .columnName(),
                                                             "getMonitorColumn",
                                                             VersionNum.VERSION100,
                                                             VersionNum.VERSION350);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "monitor" to the Row entity of
     * attributes.
     * @param monitor the column data which column name is "monitor"
     */
    public void setMonitor(String monitor) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.MONITOR
                                                                     .columnName(),
                                                             "setMonitor",
                                                             VersionNum.VERSION100,
                                                             VersionNum.VERSION350);
        super.setDataHandler(columndesc, monitor);
    }

    /**
     * Get the Column entity which column name is "cfm_mpid" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "cfm_mpid"
     */
    public Column getCfmMpidColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMMPID
                                                                     .columnName(),
                                                             "getCfmMpidColumn",
                                                             VersionNum.VERSION400);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "cfm_mpid" to the Row entity of
     * attributes.
     * @param cfmMpid the column data which column name is "cfm_mpid"
     */
    public void setCfmMpid(Set<Long> cfmMpid) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMMPID
                                                                     .columnName(),
                                                             "setCfmMpid",
                                                             VersionNum.VERSION400);
        super.setDataHandler(columndesc, cfmMpid);
    }

    /**
     * Get the Column entity which column name is "cfm_remote_mpid" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "cfm_remote_mpid"
     */
    public Column getCfmRemoteMpidColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMREMOTEMPID
                                                                     .columnName(),
                                                             "getCfmRemoteMpidColumn",
                                                             VersionNum.VERSION400,
                                                             VersionNum.VERSION520);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "cfm_remote_mpid" to the Row
     * entity of attributes.
     * @param cfmRemoteMpid the column data which column name is
     *            "cfm_remote_mpid"
     */
    public void setCfmRemoteMpid(Set<Long> cfmRemoteMpid) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMREMOTEMPID
                                                                     .columnName(),
                                                             "setCfmRemoteMpid",
                                                             VersionNum.VERSION400,
                                                             VersionNum.VERSION520);
        super.setDataHandler(columndesc, cfmRemoteMpid);
    }

    /**
     * Get the Column entity which column name is "cfm_remote_mpids" from the
     * Row entity of attributes.
     * @return the Column entity which column name is "cfm_remote_mpids"
     */
    public Column getCfmRemoteMpidsColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMREMOTEMPIDS
                                                                     .columnName(),
                                                             "getCfmRemoteMpidsColumn",
                                                             VersionNum.VERSION600);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "cfm_remote_mpids" to the Row
     * entity of attributes.
     * @param cfmRemoteMpids the column data which column name is
     *            "cfm_remote_mpids"
     */
    public void setCfmRemoteMpids(Set<Long> cfmRemoteMpids) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMREMOTEMPIDS
                                                                     .columnName(),
                                                             "setCfmRemoteMpids",
                                                             VersionNum.VERSION600);
        super.setDataHandler(columndesc, cfmRemoteMpids);
    }

    /**
     * Get the Column entity which column name is "cfm_flap_count" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "cfm_flap_count"
     */
    public Column getCfmFlapCountColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMFLAPCOUNT
                                                                     .columnName(),
                                                             "getCfmFlapCountColumn",
                                                             VersionNum.VERSION730);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "cfm_flap_count" to the Row
     * entity of attributes.
     * @param cfmFlapCount the column data which column name is "cfm_flap_count"
     */
    public void setCfmFlapCount(Set<Long> cfmFlapCount) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMFLAPCOUNT
                                                                     .columnName(),
                                                             "setCfmFlapCount",
                                                             VersionNum.VERSION730);
        super.setDataHandler(columndesc, cfmFlapCount);
    }

    /**
     * Get the Column entity which column name is "cfm_fault" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "cfm_fault"
     */
    public Column getCfmFaultColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMFAULT
                                                                     .columnName(),
                                                             "getCfmFaultColumn",
                                                             VersionNum.VERSION400);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "cfm_fault" to the Row entity of
     * attributes.
     * @param cfmFault the column data which column name is "cfm_fault"
     */
    public void setCfmFault(Set<Boolean> cfmFault) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMFAULT
                                                                     .columnName(),
                                                             "setCfmFault",
                                                             VersionNum.VERSION400);
        super.setDataHandler(columndesc, cfmFault);
    }

    /**
     * Get the Column entity which column name is "cfm_fault_status" from the
     * Row entity of attributes.
     * @return the Column entity which column name is "cfm_fault_status"
     */
    public Column getCfmFaultStatusColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMFAULTSTATUS
                                                                     .columnName(),
                                                             "getCfmFaultStatusColumn",
                                                             VersionNum.VERSION660);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "cfm_fault_status" to the Row
     * entity of attributes.
     * @param cfmFaultStatus the column data which column name is
     *            "cfm_fault_status"
     */
    public void setCfmFaultStatus(Set<String> cfmFaultStatus) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMFAULTSTATUS
                                                                     .columnName(),
                                                             "setCfmFaultStatus",
                                                             VersionNum.VERSION660);
        super.setDataHandler(columndesc, cfmFaultStatus);
    }

    /**
     * Get the Column entity which column name is "cfm_remote_opstate" from the
     * Row entity of attributes.
     * @return the Column entity which column name is "cfm_remote_opstate"
     */
    public Column getCfmRemoteOpStateColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMREMOTEOPSTATE
                                                                     .columnName(),
                                                             "getCfmRemoteOpStateColumn",
                                                             VersionNum.VERSION6100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "cfm_remote_opstate" to the Row
     * entity of attributes.
     * @param cfmRemoteOpState the column data which column name is
     *            "cfm_remote_opstate"
     */
    public void setCfmRemoteOpState(Set<String> cfmRemoteOpState) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMREMOTEOPSTATE
                                                                     .columnName(),
                                                             "setCfmRemoteOpState",
                                                             VersionNum.VERSION6100);
        super.setDataHandler(columndesc, cfmRemoteOpState);
    }

    /**
     * Get the Column entity which column name is "cfm_health" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "cfm_health"
     */
    public Column getCfmHealthColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMHEALTH
                                                                     .columnName(),
                                                             "getCfmHealthColumn",
                                                             VersionNum.VERSION690);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "cfm_health" to the Row entity
     * of attributes.
     * @param cfmHealth the column data which column name is "cfm_health"
     */
    public void setCfmHealth(Set<Long> cfmHealth) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.CFMHEALTH
                                                                     .columnName(),
                                                             "setCfmHealth",
                                                             VersionNum.VERSION690);
        super.setDataHandler(columndesc, cfmHealth);
    }

    /**
     * Get the Column entity which column name is "lacp_current" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "lacp_current"
     */
    public Column getLacpCurrentColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.LACPCURRENT
                                                                     .columnName(),
                                                             "getLacpCurrentColumn",
                                                             VersionNum.VERSION330);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "lacp_current" to the Row entity
     * of attributes.
     * @param lacpCurrent the column data which column name is "lacp_current"
     */
    public void setLacpCurrent(Set<Boolean> lacpCurrent) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.LACPCURRENT
                                                                     .columnName(),
                                                             "setLacpCurrent",
                                                             VersionNum.VERSION330);
        super.setDataHandler(columndesc, lacpCurrent);
    }

    /**
     * Get the Column entity which column name is "other_config" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "other_config"
     */
    public Column getOtherConfigColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.OTHERCONFIG
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
                                                             InterfaceColumn.OTHERCONFIG
                                                                     .columnName(),
                                                             "setOtherConfig",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, otherConfig);
    }

    /**
     * Get the Column entity which column name is "statistics" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "statistics"
     */
    public Column getStatisticsColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.STATISTICS
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
                                                             InterfaceColumn.STATISTICS
                                                                     .columnName(),
                                                             "setStatistics",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, statistics);
    }

    /**
     * Get the Column entity which column name is "status" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "status"
     */
    public Column getStatusColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.STATUS
                                                                     .columnName(),
                                                             "getStatusColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "status" to the Row entity of
     * attributes.
     * @param status the column data which column name is "status"
     */
    public void setStatus(Map<String, String> status) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.STATUS
                                                                     .columnName(),
                                                             "setStatus",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, status);
    }

    /**
     * Get the Column entity which column name is "admin_state" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "admin_state"
     */
    public Column getAdminStateColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.ADMINSTATE
                                                                     .columnName(),
                                                             "getAdminStateColumn",
                                                             VersionNum.VERSION106);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "admin_state" to the Row entity
     * of attributes.
     * @param adminState the column data which column name is "admin_state"
     */
    public void setAdminState(Set<String> adminState) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.ADMINSTATE
                                                                     .columnName(),
                                                             "setAdminState",
                                                             VersionNum.VERSION106);
        super.setDataHandler(columndesc, adminState);
    }

    /**
     * Get the Column entity which column name is "link_state" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "link_state"
     */
    public Column getLinkStateColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.LINKSTATE
                                                                     .columnName(),
                                                             "getLinkStateColumn",
                                                             VersionNum.VERSION106);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "link_state" to the Row entity
     * of attributes.
     * @param linkState the column data which column name is "link_state"
     */
    public void setLinkState(Map<String, String> linkState) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.LINKSTATE
                                                                     .columnName(),
                                                             "setLinkState",
                                                             VersionNum.VERSION106);
        super.setDataHandler(columndesc, linkState);
    }

    /**
     * Get the Column entity which column name is "link_resets" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "link_resets"
     */
    public Column getLinkResetsColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.LINKRESETS
                                                                     .columnName(),
                                                             "getLinkResetsColumn",
                                                             VersionNum.VERSION620);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "link_resets" to the Row entity
     * of attributes.
     * @param linkResets the column data which column name is "link_resets"
     */
    public void setLinkResets(Set<String> linkResets) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.LINKRESETS
                                                                     .columnName(),
                                                             "setLinkResets",
                                                             VersionNum.VERSION620);
        super.setDataHandler(columndesc, linkResets);
    }

    /**
     * Get the Column entity which column name is "link_speed" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "link_speed"
     */
    public Column getLinkSpeedColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.LINKSPEED
                                                                     .columnName(),
                                                             "getLinkSpeedColumn",
                                                             VersionNum.VERSION106);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "link_speed" to the Row entity
     * of attributes.
     * @param linkSpeed the column data which column name is "link_speed"
     */
    public void setLinkSpeed(Set<Long> linkSpeed) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.LINKSPEED
                                                                     .columnName(),
                                                             "setLinkSpeed",
                                                             VersionNum.VERSION106);
        super.setDataHandler(columndesc, linkSpeed);
    }

    /**
     * Get the Column entity which column name is "duplex" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "duplex"
     */
    public Column getDuplexColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.DUPLEX
                                                                     .columnName(),
                                                             "getDuplexColumn",
                                                             VersionNum.VERSION106);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "duplex" to the Row entity of
     * attributes.
     * @param duplex the column data which column name is "duplex"
     */
    public void setDuplex(Set<Long> duplex) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.DUPLEX
                                                                     .columnName(),
                                                             "setDuplex",
                                                             VersionNum.VERSION106);
        super.setDataHandler(columndesc, duplex);
    }

    /**
     * Get the Column entity which column name is "mtu" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "mtu"
     */
    public Column getMtuColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.MTU
                                                                     .columnName(),
                                                             "getMtuColumn",
                                                             VersionNum.VERSION106);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Get the Column entity which column name is "mtuRequest" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "mtuRequest"
     */
    public Column getMtuRequestColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.MTU_REQUEST
                                                             .columnName(),
                                                            "getMtuRequestColumn",
                                                             VersionNum.VERSION7140);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "mtu" to the Row entity of
     * attributes.
     * @param mtu the column data which column name is "mtu"
     */
    public void setMtu(Set<Long> mtu) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.MTU
                                                                     .columnName(),
                                                             "setMtu",
                                                             VersionNum.VERSION106);
        super.setDataHandler(columndesc, mtu);
    }

    /**
     * Add a Column entity which column name is "mtuRequest" to the Row entity of
     * attributes.
     * @param mtuRequest the column data which column name is "mtuRequest"
     */
    public void setMtuRequest(Set<Long> mtuRequest) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.MTU_REQUEST
                                                                     .columnName(),
                                                             "setMtuRequest",
                                                             VersionNum.VERSION7140);
        super.setDataHandler(columndesc, mtuRequest);
    }

    /**
     * Get the Column entity which column name is "error" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "error"
     */
    public Column getErrorColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.ERROR
                                                                     .columnName(),
                                                             "getErrorColumn",
                                                             VersionNum.VERSION770);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "error" to the Row entity of
     * attributes.
     * @param error the column data which column name is "error"
     */
    public void setError(Set<String> error) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             InterfaceColumn.ERROR
                                                                     .columnName(),
                                                             "setError",
                                                             VersionNum.VERSION770);
        super.setDataHandler(columndesc, error);
    }

}
