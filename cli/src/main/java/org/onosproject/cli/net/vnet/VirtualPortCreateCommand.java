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
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Creates a new virtual port.
 */
@Command(scope = "onos", name = "vnet-create-port",
        description = "Creates a new virtual port in a network.")
public class VirtualPortCreateCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "networkId", description = "Network ID",
            required = true, multiValued = false)
    Long networkId = null;

    @Argument(index = 1, name = "deviceId", description = "Virtual Device ID",
            required = true, multiValued = false)
    String deviceId = null;

    @Argument(index = 2, name = "portNum", description = "Virtual device port number",
            required = true, multiValued = false)
    Integer portNum = null;

    @Argument(index = 3, name = "physDeviceId", description = "Physical Device ID",
            required = true, multiValued = false)
    String physDeviceId = null;

    @Argument(index = 4, name = "physPortNum", description = "Physical device port number",
            required = true, multiValued = false)
    Integer physPortNum = null;

    @Override
    protected void execute() {
        VirtualNetworkAdminService service = get(VirtualNetworkAdminService.class);
        DeviceService deviceService = get(DeviceService.class);
        VirtualDevice virtualDevice = getVirtualDevice(DeviceId.deviceId(deviceId));
        checkNotNull(virtualDevice, "The virtual device does not exist.");

        DefaultAnnotations annotations = DefaultAnnotations.builder().build();
        Device physDevice = new DefaultDevice(null, DeviceId.deviceId(physDeviceId),
                                              null, null, null, null, null, null, annotations);
        Port realizedBy = new DefaultPort(physDevice, PortNumber.portNumber(physPortNum), true);
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
