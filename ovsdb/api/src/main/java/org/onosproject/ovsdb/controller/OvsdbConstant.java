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

    /** Ovsdb database Open_vSwitch. */
    public static final String DATABASENAME = "Open_vSwitch";

    /** Ovsdb table Bridge. */
    public static final String BRIDGE = "Bridge";

    /** Ovsdb table Interface. */
    public static final String INTERFACE = "Interface";

    /** Ovsdb table Controller. */
    public static final String CONTROLLER = "Controller";

    /** Ovsdb table Port. */
    public static final String PORT = "Port";

    /** Ovsdb bridge name. */
    public static final String INTEGRATION_BRIDGE = "br-int";

    /** Ovsdb vxlan tunnel type. */
    public static final String TYPEVXLAN = "vxlan";

    /** Openflow version. */
    public static final String OPENFLOW13 = "OpenFlow13";

    /** Ovsdb external_id_interface_id.. */
    public static final String EXTERNAL_ID_INTERFACE_ID = "iface-id";

    /** Ovsdb external_id_vm_mac. */
    public static final String EXTERNAL_ID_VM_MAC = "attached-mac";

    /** Openflow port. */
    public static final int OFPORT = 6653;

    /** Ovsdb port. */
    public static final int OVSDBPORT = 6640;

}
