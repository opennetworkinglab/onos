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

package org.onosproject.ui.topo;

/**
 * Defines string constants used in the Topology View of the ONOS GUI.
 */
public final class TopoConstants {

    /**
     * Defines constants for property/localization keys on the default summary
     * and details panels. Note that display labels should be looked up using
     * the "core.view.Topo" localization bundle (LionBundle).
     */
    public static final class Properties {
        public static final String SEPARATOR = "-";

        // summary panel
        public static final String DEVICES = "devices";
        public static final String LINKS = "links";
        public static final String HOSTS = "hosts";
        public static final String TOPOLOGY_SSCS = "topology_sccs";
        public static final String INTENTS = "intents";
        public static final String TUNNELS = "tunnels";
        public static final String FLOWS = "flows";
        public static final String VERSION = "version";

        // device details
        public static final String URI = "uri";
        public static final String VENDOR = "vendor";
        public static final String HW_VERSION = "hw_version";
        public static final String SW_VERSION = "sw_version";
        public static final String SERIAL_NUMBER = "serial_number";
        public static final String PROTOCOL = "protocol";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String GRID_Y = "grid_y";
        public static final String GRID_X = "grid_x";
        public static final String PORTS = "ports";

        // host details
        public static final String MAC = "mac";
        public static final String IP = "ip";
        public static final String VLAN = "vlan";
        public static final String VLAN_NONE = "vlan_none";
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
