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
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.net.DeviceId;
import org.onosproject.utils.Comparators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lists all virtual ports for the network ID.
 */
@Command(scope = "onos", name = "vnet-ports",
        description = "Lists all virtual ports in a virtual network.")
public class VirtualPortListCommand extends AbstractShellCommand {

    private static final String FMT_VIRTUAL_PORT =
            "virtual portNumber=%s, physical deviceId=%s, portNumber=%s";

    @Argument(index = 0, name = "networkId", description = "Network ID",
            required = true, multiValued = false)
    Long networkId = null;

    @Argument(index = 1, name = "deviceId", description = "Virtual Device ID",
            required = true, multiValued = false)
    String deviceId = null;

    @Override
    protected void execute() {

        getSortedVirtualPorts().forEach(this::printVirtualPort);
    }

    /**
     * Returns the list of virtual ports sorted using the network identifier.
     *
     * @return sorted virtual port list
     */
    private List<VirtualPort> getSortedVirtualPorts() {
        VirtualNetworkService service = get(VirtualNetworkService.class);

        List<VirtualPort> virtualPorts = new ArrayList<>();
        virtualPorts.addAll(service.getVirtualPorts(NetworkId.networkId(networkId),
                                                    DeviceId.deviceId(deviceId)));
        Collections.sort(virtualPorts, Comparators.VIRTUAL_PORT_COMPARATOR);
        return virtualPorts;
    }

    /**
     * Prints out each virtual port.
     *
     * @param virtualPort virtual port
     */
    private void printVirtualPort(VirtualPort virtualPort) {
        if (virtualPort.realizedBy() == null) {
            print(FMT_VIRTUAL_PORT, virtualPort.number(), "None", "None");
        } else {
            print(FMT_VIRTUAL_PORT, virtualPort.number(),
                  virtualPort.realizedBy().deviceId(),
                  virtualPort.realizedBy().port());
        }
    }
}
