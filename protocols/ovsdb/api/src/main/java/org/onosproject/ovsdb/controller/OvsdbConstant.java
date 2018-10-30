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
package org.onosproject.ovsdb.controller;

/**
 * Ovsdb related constants.
 */
public final class OvsdbConstant {

    /**
     * Default constructor.
     *
     * The constructor is private to prevent creating an instance of this
     * utility class.
     */
    private OvsdbConstant() {
    }

    /** Common column names. */
    public static final String UUID = "_uuid";

    /** Ovsdb database Open_vSwitch. */
    public static final String DATABASENAME = "Open_vSwitch";

    /** Open_vSwitch table. */
    public static final String BRIDGES = "bridges";

    /** Bridge table. */
    public static final String BRIDGE = "Bridge";
    public static final String PORTS = "ports";
    public static final String MIRRORS = "mirrors";
    // other configs
    public static final String DATAPATH_ID = "datapath-id";
    public static final String DISABLE_INBAND = "disable-in-band";
    public static final String PROTOCOLS = "protocols";

    /** Port table. */
    public static final String PORT = "Port";
    public static final String INTERFACES = "interfaces";
    public static final String PORT_QOS = "qos";

    /** Interface table. */
    public static final String INTERFACE = "Interface";
    // type
    public static final String TYPEVXLAN = "vxlan";
    // virtual machine identifiers
    public static final String EXTERNAL_ID_INTERFACE_ID = "iface-id";
    public static final String EXTERNAL_ID_VM_MAC = "attached-mac";
    // tunnel interface options
    public static final String TUNNEL_LOCAL_IP = "local_ip";
    public static final String TUNNEL_REMOTE_IP = "remote_ip";
    public static final String TUNNEL_KEY = "key";
    // patch interface options
    public static final String PATCH_PEER = "peer";

    /** Controller table. */
    public static final String CONTROLLER = "Controller";

    /** Mirror table. */
    public static final String MIRROR = "Mirror";

    /* Qos table */
    public static final String QOS = "QoS";
    public static final String QUEUES = "queues";
    public static final String CIR = "cir";
    public static final String CBS = "cbs";
    public static final String QOS_EXTERNAL_ID_KEY = "onos-qos-id";
    public static final String QOS_TYPE_PREFIX = "linux-";
    public static final String QOS_EGRESS_POLICER = "egress-policer";

    /* Queue table */
    public static final String QUEUE = "Queue";
    public static final String MIN_RATE = "min-rate";
    public static final String MAX_RATE = "max-rate";
    public static final String BURST = "burst";
    public static final String PRIORITY = "priority";
    public static final String QUEUE_EXTERNAL_ID_KEY = "onos-queue-id";

    /* external id */
    public static final String EXTERNAL_ID = "external_ids";

    /** Ovsdb bridge name. */
    // TODO remove this particular bridge name from OVSDB provider
    public static final String INTEGRATION_BRIDGE = "br-int";

    /** Openflow version. */
    public static final String OPENFLOW13 = "OpenFlow13";

    /** Openflow port. */
    public static final int OFPORT = 6653;

    /** Ovsdb port. */
    public static final int OVSDBPORT = 6640;

    /** Ovsdb Bridge table, Controller column name. */
    public static final String BRIDGE_CONTROLLER = "controller";

    /** Openflow port Error. */
    public static final int OFPORT_ERROR = -1;

    public static final boolean SERVER_MODE = true;

    /** Ovsdb database Switch_Inventory. */
    public static final String SWINVENTORY_DBNAME = "Switch_Inventory";

    /** Cpu_Memory_Data table. */
    public static final String CPU_MEMORY_DATA = "Cpu_Memory_Data";

    /** Cpu column of Cpu_Memory_Data table. */
    public static final String DEVICE_CPU = "cpu";

    /** Memory column of Cpu_Memory_Data table. */
    public static final String DEVICE_MEMORY = "memory";

    public static final boolean OVSDB_TLS_FLAG = false;

}
