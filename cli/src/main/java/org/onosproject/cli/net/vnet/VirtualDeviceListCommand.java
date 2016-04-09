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
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.utils.Comparators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lists all virtual devices for the network ID.
 */
@Command(scope = "onos", name = "vnet-devices",
        description = "Lists all virtual devices in a virtual network.")
public class VirtualDeviceListCommand extends AbstractShellCommand {

    private static final String FMT_VIRTUAL_DEVICE =
            "deviceId=%s";

    @Argument(index = 0, name = "networkId", description = "Network ID",
            required = true, multiValued = false)
    Long networkId = null;

    @Override
    protected void execute() {

        getSortedVirtualDevices().forEach(this::printVirtualDevice);
    }

    /**
     * Returns the list of virtual devices sorted using the device identifier.
     *
     * @return sorted virtual device list
     */
    private List<VirtualDevice> getSortedVirtualDevices() {
        VirtualNetworkService service = get(VirtualNetworkService.class);

        List<VirtualDevice> virtualDevices = new ArrayList<>();
        virtualDevices.addAll(service.getVirtualDevices(NetworkId.networkId(networkId)));
        Collections.sort(virtualDevices, Comparators.VIRTUAL_DEVICE_COMPARATOR);
        return virtualDevices;
    }

    /**
     * Prints out each virtual device.
     *
     * @param virtualDevice virtual device
     */
    private void printVirtualDevice(VirtualDevice virtualDevice) {
        print(FMT_VIRTUAL_DEVICE, virtualDevice.id());
    }
}
