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

package org.onosproject.ui.topo;

/**
 * Defines string constants used in the Topology View of the ONOS GUI.
 */
public final class TopoConstants {

    /**
     * Defines constants for property names on the default summary and
     * details panels.
     */
    public static final class Properties {
        public static final String SEPARATOR = "-";

        // summary panel
        public static final String DEVICES = "Devices";
        public static final String LINKS = "Links";
        public static final String HOSTS = "Hosts";
        public static final String TOPOLOGY_SSCS = "Topology SCCs";
        public static final String INTENTS = "Intents";
        public static final String TUNNELS = "Tunnels";
        public static final String FLOWS = "Flows";
        public static final String VERSION = "Version";

        // device details
        public static final String URI = "URI";
        public static final String VENDOR = "Vendor";
        public static final String HW_VERSION = "H/W Version";
        public static final String SW_VERSION = "S/W Version";
        public static final String SERIAL_NUMBER = "Serial #";
        public static final String PROTOCOL = "Protocol";
        public static final String LATITUDE = "Latitude";
        public static final String LONGITUDE = "Longitude";
        public static final String GRID_Y = "Grid Y";
        public static final String GRID_X = "Grid X";
        public static final String PORTS = "Ports";

        // host details
        public static final String MAC = "MAC";
        public static final String IP = "IP";
        public static final String VLAN = "VLAN";
    }

    /**
     * Defines identities of core buttons that appear on the topology
     * details panel.
     */
    public static final class CoreButtons {
        public static final ButtonId SHOW_DEVICE_VIEW =
                new ButtonId("showDeviceView");

        public static final ButtonId SHOW_FLOW_VIEW =
                new ButtonId("showFlowView");

        public static final ButtonId SHOW_PORT_VIEW =
                new ButtonId("showPortView");

        public static final ButtonId SHOW_GROUP_VIEW =
                new ButtonId("showGroupView");

        public static final ButtonId SHOW_METER_VIEW =
                new ButtonId("showMeterView");
    }
}
