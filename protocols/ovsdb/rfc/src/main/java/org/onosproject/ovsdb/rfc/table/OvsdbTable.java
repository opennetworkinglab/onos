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

/**
 * Ovsdb table name. Refer to RFC7047's Section 9.2.
 */
public enum OvsdbTable {
    INTERFACE("Interface"), BRIDGE("Bridge"), CONTROLLER("Controller"),
    PORT("Port"), OPENVSWITCH("Open_vSwitch"), FLWTABLE("Flow_Table"),
    QOS("QoS"), QUEUE("Queue"), MIRROR("Mirror"), MANAGER("Manager"),
    NETFLOW("NetFlow"), SSL("SSL"), SFLOW("sFlow"), IPFIX("IPFIX"),
    FLOWSAMPLECOLLECTORSET("Flow_Sample_Collector_Set");

    private final String tableName;

    private OvsdbTable(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns the table name for OvsdbTable.
     * @return the table name
     */
    public String tableName() {
        return tableName;
    }
}
