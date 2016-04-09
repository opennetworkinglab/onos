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

package org.onosproject.cli.net.vnet;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

/**
 * Removes a virtual port.
 */
@Command(scope = "onos", name = "vnet-remove-port",
        description = "Removes a virtual port.")
public class VirtualPortRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "networkId", description = "Network ID",
            required = true, multiValued = false)
    Long networkId = null;

    @Argument(index = 1, name = "deviceId", description = "Device ID",
            required = true, multiValued = false)
    String deviceId = null;

    @Argument(index = 2, name = "portNum", description = "Device port number",
            required = true, multiValued = false)
    Integer portNum = null;

    @Override
    protected void execute() {
        VirtualNetworkAdminService service = get(VirtualNetworkAdminService.class);
        service.removeVirtualPort(NetworkId.networkId(networkId), DeviceId.deviceId(deviceId),
                                  PortNumber.portNumber(portNum));
        print("Virtual port successfully removed.");
    }
}
