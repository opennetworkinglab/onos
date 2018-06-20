/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.segmentrouting.cli;

import com.google.common.collect.Sets;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.segmentrouting.xconnect.api.XconnectService;

import java.util.Set;

/**
 * Creates Xconnect.
 */
@Command(scope = "onos", name = "sr-xconnect-add", description = "Create Xconnect")
public class XconnectAddCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Device ID",
            required = true, multiValued = false)
    private String deviceIdStr;

    @Argument(index = 1, name = "vlanId",
            description = "VLAN ID",
            required = true, multiValued = false)
    private String vlanIdStr;

    @Argument(index = 2, name = "port1",
            description = "Port 1",
            required = true, multiValued = false)
    private String port1Str;

    @Argument(index = 3, name = "port2",
            description = "Port 2",
            required = true, multiValued = false)
    private String port2Str;


    @Override
    protected void execute() {
        DeviceId deviceId = DeviceId.deviceId(deviceIdStr);
        VlanId vlanId = VlanId.vlanId(vlanIdStr);
        PortNumber port1 = PortNumber.portNumber(port1Str);
        PortNumber port2 = PortNumber.portNumber(port2Str);
        Set<PortNumber> ports = Sets.newHashSet(port1, port2);

        XconnectService xconnectService = get(XconnectService.class);
        xconnectService.addOrUpdateXconnect(deviceId, vlanId, ports);
    }
}
