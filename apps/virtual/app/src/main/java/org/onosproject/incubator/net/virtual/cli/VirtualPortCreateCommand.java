/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.cli.net.PortNumberCompleter;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Creates a new virtual port.
 */
@Service
@Command(scope = "onos", name = "vnet-create-port",
        description = "Creates a new virtual port in a network.")
public class VirtualPortCreateCommand extends AbstractShellCommand {

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
    Integer portNum = null;

    @Argument(index = 3, name = "physDeviceId", description = "Physical Device ID",
            required = false, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String physDeviceId = null;

    @Argument(index = 4, name = "physPortNum", description = "Physical device port number",
            required = false, multiValued = false)
    @Completion(PortNumberCompleter.class)
    Integer physPortNum = null;

    @Override
    protected void doExecute() {
        VirtualNetworkAdminService service = get(VirtualNetworkAdminService.class);
        DeviceService deviceService = get(DeviceService.class);

        VirtualDevice virtualDevice = getVirtualDevice(DeviceId.deviceId(deviceId));
        checkNotNull(virtualDevice, "The virtual device does not exist.");

        ConnectPoint realizedBy = null;
        if (physDeviceId != null && physPortNum != null) {
            checkNotNull(physPortNum, "The physical port does not specified.");
            realizedBy = new ConnectPoint(DeviceId.deviceId(physDeviceId),
                                               PortNumber.portNumber(physPortNum));
            checkNotNull(realizedBy, "The physical port does not exist.");
        }

        service.createVirtualPort(NetworkId.networkId(networkId), DeviceId.deviceId(deviceId),
                                  PortNumber.portNumber(portNum), realizedBy);
        print("Virtual port successfully created.");
    }

    /**
     * Returns the virtual device matching the device identifier.
     *
     * @param aDeviceId device identifier
     * @return matching virtual device, or null.
     */
    private VirtualDevice getVirtualDevice(DeviceId aDeviceId) {
        VirtualNetworkService service = get(VirtualNetworkService.class);

        Set<VirtualDevice> virtualDevices = service.getVirtualDevices(NetworkId.networkId(networkId));

        for (VirtualDevice virtualDevice : virtualDevices) {
            if (virtualDevice.id().equals(aDeviceId)) {
                return virtualDevice;
            }
        }
        return null;
    }
}
