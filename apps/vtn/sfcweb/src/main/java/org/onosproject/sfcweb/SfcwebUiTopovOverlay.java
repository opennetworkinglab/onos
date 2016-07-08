/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.sfcweb;

import org.onlab.packet.MacAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.host.HostService;
import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.topo.PropertyPanel;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.portpair.PortPairService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;

/**
 * Our sfcweb topology overlay.
 */
public class SfcwebUiTopovOverlay extends UiTopoOverlay {

    // NOTE: this must match the ID defined in sfcwebTopov.js
    private static final String OVERLAY_ID = "SFC-Service-overlay";
    private static final String MY_DEVICE_TITLE = "SFF specific device details";
    private static final String MY_HOST_TITLE = "SF specific host details";

    public SfcwebUiTopovOverlay() {
        super(OVERLAY_ID);
    }

    @Override
    public void modifyDeviceDetails(PropertyPanel pp, DeviceId deviceId) {
        pp.title(MY_DEVICE_TITLE);
        pp.removeAllProps();
        pp.addProp("SFF Device Id", deviceId.toString());
    }

    @Override
    public void modifyHostDetails(PropertyPanel pp, HostId hostId) {
        pp.title(MY_HOST_TITLE);
        pp.removeAllProps();
        PortPairService portPairService = AbstractShellCommand.get(PortPairService.class);
        VirtualPortService virtualPortService = AbstractShellCommand.get(VirtualPortService.class);
        HostService hostService = AbstractShellCommand.get(HostService.class);
        Iterable<PortPair> portPairs = portPairService.getPortPairs();
        for (PortPair portPair : portPairs) {
            VirtualPort vPort = virtualPortService.getPort(VirtualPortId.portId(portPair.ingress()));
            MacAddress dstMacAddress = vPort.macAddress();
            Host host = hostService.getHost(HostId.hostId(dstMacAddress));
            if (hostId.toString().equals(host.id().toString())) {
                pp.addProp("SF Name", portPair.name());
                pp.addProp("SF Ip", vPort.fixedIps().iterator().next().ip());
            }
        }
        pp.addProp("SF host Address", hostId.toString());
    }

}
