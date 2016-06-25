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
    // other configs
    public static final String DATAPATH_ID = "datapath-id";
    public static final String DISABLE_INBAND = "disable-in-band";

    /** Interface table. */
    public static final String INTERFACE = "Interface";
    // type
    public static final String TYPEVXLAN = "vxlan";
    // virtual machine identifiers
    public static final String EXTERNAL_ID_INTERFACE_ID = "iface-id";
    public static final String EXTERNAL_ID_VM_MAC = "attached-mac";

    /** Controller table. */
    public static final String CONTROLLER = "Controller";

    /** Port table. */
    public static final String PORT = "Port";

    /** Ovsdb bridge name. */
    // TODO remove this particular bridge name from OVSDB provider
    public static final String INTEGRATION_BRIDGE = "br-int";

    /** Openflow version. */
    public static final String OPENFLOW13 = "OpenFlow13";

    /** Openflow port. */
    public static final int OFPORT = 6653;

    /** Ovsdb port. */
    public static final int OVSDBPORT = 6640;
}
