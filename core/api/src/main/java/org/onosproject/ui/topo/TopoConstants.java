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
 * <p>
 * See also:
 * <ul>
 *     <li> https://wiki.onosproject.org/display/ONOS/UI+Service+-+GlyphService </li>
 * </ul>
 */
public final class TopoConstants {

    /**
     * Defines constants for standard glyph identifiers.
     */
    public static final class Glyphs {
        public static final String BIRD = "bird";

        public static final String UNKNOWN = "m_unknown";
        public static final String QUERY = "query";
        public static final String NODE = "node";

        public static final String SWITCH = "m_switch";
        public static final String ROUTER = "router";
        public static final String ROADM = "m_roadm";
        public static final String OTN = "otn";
        public static final String ROADM_OTN = "roadm_otn";
        public static final String FIBER_SWITCH = "fiber_switch";
        public static final String MICROWAVE = "microwave";

        public static final String ENDSTATION = "m_endstation";
        public static final String BGP_SPEAKER = "bgpSpeaker";

        public static final String CHAIN = "chain";
        public static final String CROWN = "crown";
        public static final String LOCK = "lock";
        public static final String TOPO = "topo";
        public static final String REFRESH = "refresh";
        public static final String GARBAGE = "garbage";

        public static final String FLOW_TABLE = "flowTable";
        public static final String PORT_TABLE = "portTable";
        public static final String GROUP_TABLE = "groupTable";
        public static final String METER_TABLE = "meterTable";

        public static final String SUMMARY = "m_summary";
        public static final String DETAILS = "m_details";
        public static final String PORTS = "m_ports";
        public static final String MAP = "m_map";
        public static final String CYCLE_LABELS = "m_cycleLabels";
        public static final String OBLIQUE = "m_oblique";
        public static final String FILTERS = "m_filters";
        public static final String RESET_ZOOM = "m_resetZoom";
        public static final String RELATED_INTENTS = "m_relatedIntents";
        public static final String NEXT = "m_next";
        public static final String PREV = "m_prev";
        public static final String INTENT_TRAFFIC = "m_intentTraffic";
        public static final String ALL_TRAFFIC = "m_allTraffic";
        public static final String FLOWS = "m_flows";
        public static final String EQ_MASTER = "m_eqMaster";

        public static final String UI_ATTACHED = "m_uiAttached";
        public static final String CHECK_MARK = "checkMark";
        public static final String X_MARK = "m_xMark";
        public static final String TRIANGLE_UP = "triangleUp";
        public static final String TRIANGLE_DOWN = "triangleDown";
        public static final String PLUS = "plus";
        public static final String MINUS = "minus";
        public static final String PLAY = "play";
        public static final String STOP = "stop";

        public static final String CLOUD = "m_cloud";
    }

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
        public static final String SERIAL_NUMBER = "Serial Number";
        public static final String PROTOCOL = "Protocol";
        public static final String LATITUDE = "Latitude";
        public static final String LONGITUDE = "Longitude";
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
