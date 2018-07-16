/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openstacknetworkingui;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.host.HostService;
import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.topo.ButtonId;
import org.onosproject.ui.topo.PropertyPanel;

import static org.onosproject.ui.topo.TopoConstants.Properties.INTENTS;
import static org.onosproject.ui.topo.TopoConstants.Properties.TOPOLOGY_SSCS;
import static org.onosproject.ui.topo.TopoConstants.Properties.TUNNELS;
import static org.onosproject.ui.topo.TopoConstants.Properties.VERSION;
import static org.onosproject.ui.topo.TopoConstants.Properties.VLAN;

/**
 * Topology overlay for OpenStack Networking UI.
 */
public class OpenstackNetworkingUiOverlay extends UiTopoOverlay {
    private static final String OVERLAY_ID = "sona-overlay";
    private static final String SONA = "SONA";
    private static final String SUMMARY_TITLE = "OpenStack Networking UI";
    private static final String SUMMARY_VERSION = "0.9";
    private static final String VNI = "VNI";
    private static final String ANNOTATION_SEGMENT_ID = "segId";
    private static final String DEVICE_ID = "DeviceId";

    private static final String NOT_AVAILABLE = "N/A";

    private static final ButtonId FLOW_TRACE_BUTTON = new ButtonId("flowtrace");
    private static final ButtonId RESET_BUTTON = new ButtonId("reset");
    private static final ButtonId TO_GATEWAY_BUTTON = new ButtonId("toGateway");
    private static final ButtonId TO_EXTERNAL_BUTTON = new ButtonId("toExternal");

    private final HostService hostService = DefaultServiceDirectory.getService(HostService.class);

    public OpenstackNetworkingUiOverlay() {
        super(OVERLAY_ID);
    }


    @Override
    public void modifySummary(PropertyPanel pp) {
        pp.title(SUMMARY_TITLE)
                .removeProps(
                        TOPOLOGY_SSCS,
                        INTENTS,
                        TUNNELS,
                        VERSION
                )
                .addProp(SONA, VERSION, SUMMARY_VERSION);
    }

    @Override
    public void modifyHostDetails(PropertyPanel pp, HostId hostId) {
        String vni = hostService.getHost(hostId).annotations().value(ANNOTATION_SEGMENT_ID);
        DeviceId deviceId = hostService.getHost(hostId).location().deviceId();

        pp.removeProps(VLAN);
        pp.addProp(VNI, VNI, vni == null ? NOT_AVAILABLE : vni)
                .addProp(DEVICE_ID, DEVICE_ID, deviceId.toString())
                .addButton(FLOW_TRACE_BUTTON)
                .addButton(RESET_BUTTON)
                .addButton(TO_GATEWAY_BUTTON)
                .addButton(TO_EXTERNAL_BUTTON);
    }
}
