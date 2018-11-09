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

package org.onosproject.incubator.net.virtual.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Administratively enables or disables state of an existing virtual port.
 */
@Service
@Command(scope = "onos", name = "vnet-port-state",
        description = "Administratively enables or disables state of an existing virtual port.")
public class VirtualPortStateCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "networkId", description = "Network ID",
            required = true, multiValued = false)
    @Completion(VirtualNetworkCompleter.class)
    Long networkId = null;

    @Argument(index = 1, name = "deviceId", description = "Virtual Device ID",
            required = true, multiValued = false)
    @Completion(VirtualDeviceCompleter.class)
    String deviceId = null;

    @Argument(index = 2, name = "portNum", description = "Virtual device port number",
            required = true, multiValued = false)
    @Completion(VirtualPortCompleter.class)
    Integer portNum = null;

    @Argument(index = 3, name = "portState",
            description = "Desired State. Either \"enable\" or \"disable\".",
            required = true, multiValued = false)
    String portState = null;

    @Override
    protected void doExecute() {
        VirtualNetworkAdminService service = get(VirtualNetworkAdminService.class);

        VirtualPort vPort = getVirtualPort(PortNumber.portNumber(portNum));
        checkNotNull(vPort, "The virtual Port does not exist");

        boolean isEnabled;
        if ("enable".equals(portState)) {
            isEnabled = true;
        } else if ("disable".equals(portState)) {
            isEnabled = false;
        } else {
            print("State must be enable or disable");
            return;
        }

        service.updatePortState(NetworkId.networkId(networkId),
                                DeviceId.deviceId(deviceId), vPort.number(), isEnabled);
        print("Virtual port state updated.");
    }

    /**
     * Returns the virtual port matching the device and port identifier.
     *
     * @param aPortNumber port identifier
     * @return matching virtual port, or null.
     */
    private VirtualPort getVirtualPort(PortNumber aPortNumber) {
        VirtualNetworkService service = get(VirtualNetworkService.class);
        Set<VirtualPort> ports = service.getVirtualPorts(NetworkId.networkId(networkId),
                                                    DeviceId.deviceId(deviceId));
        return ports.stream().filter(p -> p.number().equals(aPortNumber))
                .findFirst().get();
    }
}
