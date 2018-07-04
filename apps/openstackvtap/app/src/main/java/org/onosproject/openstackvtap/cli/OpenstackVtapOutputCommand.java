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
package org.onosproject.openstackvtap.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapAdminService;

import static org.onlab.packet.VlanId.UNTAGGED;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getVtapTypeFromString;

/**
 * Command line interface for set openstack vTap output.
 */
@Command(scope = "onos", name = "openstack-vtap-output",
        description = "OpenstackVtap output setup")
public class OpenstackVtapOutputCommand extends AbstractShellCommand {

    private final DeviceService deviceService = get(DeviceService.class);
    private final OpenstackVtapAdminService vTapAdminService =
                                            get(OpenstackVtapAdminService.class);

    @Argument(index = 0, name = "deviceId", description = "device id",
            required = true, multiValued = false)
    String id = "";

    @Argument(index = 1, name = "port", description = "output port number",
            required = true, multiValued = false)
    int port = 0;

    @Argument(index = 2, name = "vlan", description = "vlan id",
            required = false, multiValued = false)
    int vlan = UNTAGGED;

    @Argument(index = 3, name = "type", description = "vTap type [all|tx|rx]",
            required = false, multiValued = false)
    String vTapTypeStr = "all";

    @Override
    protected void execute() {
        try {
            Device device = deviceService.getDevice(DeviceId.deviceId(id));
            if (device != null) {
                OpenstackVtap.Type type = getVtapTypeFromString(vTapTypeStr);

                vTapAdminService.setVtapOutput(device.id(), type,
                        PortNumber.portNumber(port), VlanId.vlanId((short) vlan));
                print("Set OpenstackVtap output deviceId { %s }, port=%s, vlan=%s",
                        device.id().toString(),
                        PortNumber.portNumber(port).toString(),
                        VlanId.vlanId((short) vlan).toString());
            } else {
                print("Invalid device id");
            }
        } catch (Exception e) {
            print("Invalid parameter: %s", e.toString());
        }
    }
}
