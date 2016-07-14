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
 * This class provides operations of Controller Table.
 */
public class Controller extends AbstractOvsdbTableService {

    /**
     * Controller table column name.
     */
    public enum ControllerColumn {
        TARGET("target"), BURSTLIMIT("controller_burst_limit"),
        RATELIMIT("controller_rate_limit"), CONNECTIONMODE("connection_mode"),
        ENABLEASYNCMESSAGES("enable_async_messages"),
        EXTERNALIDS("external_ids"), LOCALNETMASK("local_netmask"),
        LOCALGATEWAY("local_gateway"), STATUS("status"), ROLE("role"),
        INACTIVITYPROBE("inactivity_probe"), ISCONNECTED("is_connected"),
        OTHERCONFIG("other_config"), MAXBACKOFF("max_backoff"),
        LOCALIP("local_ip"),
        DISCOVERUPDATERESOLVCONF("discover_update_resolv_conf"),
        DISCOVERACCEPTREGEX("discover_accept_regex");

        private final String columnName;

        private ControllerColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for ControllerColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a Controller object. Generate Controller Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public Controller(DatabaseSchema dbSchema, Row row) {
        super(dbSchema, row, OvsdbTable.CONTROLLER, VersionNum.VERSION100);
    }

    /**
     * Get the Column entity which column name is "target" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "target"
     */
    public Column getTargetColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.TARGET
                                                                     .columnName(),
                                                             "getTargetColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "target" to the Row entity of
     * attributes.
     * @param target the column data which column name is "target"
     */
    public void setTarget(String target) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.TARGET
                                                                     .columnName(),
                                                             "setTarget",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, target);
    }

    /**
     * Get the Column entity which column name is "controller_burst_limit" from
     * the Row entity of attributes.
     * @return the Column entity which column name is "controller_burst_limit"
     */
    public Column getBurstLimitColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.BURSTLIMIT
                                                                     .columnName(),
                                                             "getBurstLimitColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "controller_burst_limit" to the
     * Row entity of attributes.
     * @param burstLimit the column data which column name is
     *            "controller_burst_limit"
     */
    public void setBurstLimit(Long burstLimit) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.BURSTLIMIT
                                                                     .columnName(),
                                                             "setBurstLimit",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, burstLimit);
    }

    /**
     * Get the Column entity which column name is "controller_rate_limit" from
     * the Row entity of attributes.
     * @return the Column entity which column name is "controller_rate_limit"
     */
    public Column getRateLimitColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.RATELIMIT
                                                                     .columnName(),
                                                             "getRateLimitColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "controller_rate_limit" to the
     * Row entity of attributes.
     * @param rateLimit the column data which column name is
     *            "controller_rate_limit"
     */
    public void setRateLimit(Long rateLimit) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             "controller_rate_limit",
                                                             "setRateLimit",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, rateLimit);
    }

    /**
     * Get the Column entity which column name is "connection_mode" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "connection_mode"
     */
    public Column getConnectionModeColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             "connection_mode",
                                                             "getConnectionModeColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "connection_mode" to the Row
     * entity of attributes.
     * @param connectionMode the column data which column name is
     *            "connection_mode"
     */
    public void setConnectionMode(Set<String> connectionMode) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.RATELIMIT
                                                                     .columnName(),
                                                             "setConnectionMode",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, connectionMode);
    }

    /**
     * Get the Column entity which column name is "enable_async_messages" from
     * the Row entity of attributes.
     * @return the Column entity which column name is "enable_async_messages"
     */
    public Column getEnableAsyncMessagesColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.ENABLEASYNCMESSAGES
                                                                     .columnName(),
                                                             "getEnableAsyncMessagesColumn",
                                                             VersionNum.VERSION670);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "enable_async_messages" to the
     * Row entity of attributes.
     * @param enableAsyncMessages the column data which column name is
     *            "enable_async_messages"
     */
    public void setEnableAsyncMessages(Set<Boolean> enableAsyncMessages) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.ENABLEASYNCMESSAGES
                                                                     .columnName(),
                                                             "setEnableAsyncMessages",
                                                             VersionNum.VERSION670);
        super.setDataHandler(columndesc, enableAsyncMessages);
    }

    /**
     * Get the Column entity which column name is "external_ids" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "external_ids"
     */
    public Column getExternalIdsColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.EXTERNALIDS
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
                                                             ControllerColumn.EXTERNALIDS
                                                                     .columnName(),
                                                             "setExternalIds",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, externalIds);
    }

    /**
     * Get the Column entity which column name is "local_netmask" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "local_netmask"
     */
    public Column getLocalNetmaskColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.LOCALNETMASK
                                                                     .columnName(),
                                                             "getLocalNetmaskColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "local_netmask" to the Row
     * entity of attributes.
     * @param localNetmask the column data which column name is "local_netmask"
     */
    public void setLocalNetmask(Set<String> localNetmask) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.LOCALNETMASK
                                                                     .columnName(),
                                                             "setLocalNetmask",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, localNetmask);
    }

    /**
     * Get the Column entity which column name is "local_gateway" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "local_gateway"
     */
    public Column getLocalGatewayColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.LOCALGATEWAY
                                                                     .columnName(),
                                                             "getLocalGatewayColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "local_gateway" to the Row
     * entity of attributes.
     * @param localGateway the column data which column name is "local_gateway"
     */
    public void setLocalGateway(Set<String> localGateway) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.LOCALGATEWAY
                                                                     .columnName(),
                                                             "setLocalGateway",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, localGateway);
    }

    /**
     * Get the Column entity which column name is "status" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "status"
     */
    public Column getStatusColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.STATUS
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
                                                             ControllerColumn.STATUS
                                                                     .columnName(),
                                                             "setStatus",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, status);
    }

    /**
     * Get the Column entity which column name is "role" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "role"
     */
    public Column getRoleColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.ROLE
                                                                     .columnName(),
                                                             "getRoleColumn",
                                                             VersionNum.VERSION110);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "role" to the Row entity of
     * attributes.
     * @param role the column data which column name is "role"
     */
    public void setRole(Set<String> role) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.ROLE
                                                                     .columnName(),
                                                             "setRole",
                                                             VersionNum.VERSION110);
        super.setDataHandler(columndesc, role);
    }

    /**
     * Get the Column entity which column name is "inactivity_probe" from the
     * Row entity of attributes.
     * @return the Column entity which column name is "inactivity_probe"
     */
    public Column getInactivityProbeColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.INACTIVITYPROBE
                                                                     .columnName(),
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
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.INACTIVITYPROBE
                                                                     .columnName(),
                                                             "setInactivityProbe",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, inactivityProbe);
    }

    /**
     * Get the Column entity which column name is "is_connected" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "is_connected"
     */
    public Column getIsConnectedColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.ISCONNECTED
                                                                     .columnName(),
                                                             "getIsConnectedColumn",
                                                             VersionNum.VERSION110);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "is_connected" to the Row entity
     * of attributes.
     * @param isConnected the column data which column name is "is_connected"
     */
    public void setIsConnected(Boolean isConnected) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.ISCONNECTED
                                                                     .columnName(),
                                                             "setIsConnected",
                                                             VersionNum.VERSION110);
        super.setDataHandler(columndesc, isConnected);
    }

    /**
     * Get the Column entity which column name is "other_config" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "other_config"
     */
    public Column getOtherConfigColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.OTHERCONFIG
                                                                     .columnName(),
                                                             "getOtherConfigColumn",
                                                             VersionNum.VERSION680);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "other_config" to the Row entity
     * of attributes.
     * @param otherConfig the column data which column name is "other_config"
     */
    public void setOtherConfig(Map<String, String> otherConfig) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.OTHERCONFIG
                                                                     .columnName(),
                                                             "setOtherConfig",
                                                             VersionNum.VERSION680);
        super.setDataHandler(columndesc, otherConfig);
    }

    /**
     * Get the Column entity which column name is "max_backoff" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "max_backoff"
     */
    public Column getMaxBackoffColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.MAXBACKOFF
                                                                     .columnName(),
                                                             "getMaxBackoffColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "max_backoff" to the Row entity
     * of attributes.
     * @param maxBackoff the column data which column name is "max_backoff"
     */
    public void setMaxBackoff(Long maxBackoff) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.MAXBACKOFF
                                                                     .columnName(),
                                                             "setMaxBackoff",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, maxBackoff);
    }

    /**
     * Get the Column entity which column name is "local_ip" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "local_ip"
     */
    public Column getLocalIpColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.LOCALIP
                                                                     .columnName(),
                                                             "getLocalIpColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "local_ip" to the Row entity of
     * attributes.
     * @param localIp the column data which column name is "local_ip"
     */
    public void setLocalIp(Set<String> localIp) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.LOCALIP
                                                                     .columnName(),
                                                             "setLocalIp",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, localIp);
    }

    /**
     * Get the Column entity which column name is "discover_update_resolv_conf"
     * from the Row entity of attributes.
     * @return the Column entity which column name is
     *         "discover_update_resolv_conf"
     */
    public Column getDiscoverUpdateResolvConfColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.DISCOVERUPDATERESOLVCONF
                                                                     .columnName(),
                                                             "getDiscoverUpdateResolvConfColumn",
                                                             VersionNum.VERSION100,
                                                             VersionNum.VERSION300);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "discover_update_resolv_conf" to
     * the Row entity of attributes.
     * @param discoverUpdateResolvConf the column data which column name is
     *            "discover_update_resolv_conf"
     */
    public void setDiscoverUpdateResolvConf(Set<String> discoverUpdateResolvConf) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.DISCOVERUPDATERESOLVCONF
                                                                     .columnName(),
                                                             "setDiscoverUpdateResolvConf",
                                                             VersionNum.VERSION100,
                                                             VersionNum.VERSION300);
        super.setDataHandler(columndesc, discoverUpdateResolvConf);
    }

    /**
     * Get the Column entity which column name is "discover_accept_regex" from
     * the Row entity of attributes.
     * @return the Column entity which column name is "discover_accept_regex"
     */
    public Column getDiscoverAcceptRegexColumn() {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.DISCOVERACCEPTREGEX
                                                                     .columnName(),
                                                             "getDiscoverAcceptRegexColumn",
                                                             VersionNum.VERSION100,
                                                             VersionNum.VERSION300);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "discover_accept_regex" to the
     * Row entity of attributes.
     * @param discoverAcceptRegex the column data which column name is
     *            "discover_accept_regex"
     */
    public void setDiscoverAcceptRegex(Set<String> discoverAcceptRegex) {
        ColumnDescription columndesc = new ColumnDescription(
                                                             ControllerColumn.DISCOVERACCEPTREGEX
                                                                     .columnName(),
                                                             "setDiscoverAcceptRegex",
                                                             VersionNum.VERSION100,
                                                             VersionNum.VERSION300);
        super.setDataHandler(columndesc, discoverAcceptRegex);
    }
}
