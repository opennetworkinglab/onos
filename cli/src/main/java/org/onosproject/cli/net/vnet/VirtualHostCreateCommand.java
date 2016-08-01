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
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Creates a new virtual host.
 */
@Command(scope = "onos", name = "vnet-create-host",
        description = "Creates a new virtual host in a network.")
public class VirtualHostCreateCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "networkId", description = "Network ID",
            required = true, multiValued = false)
    Long networkId = null;

    @Argument(index = 1, name = "mac", description = "Mac address",
            required = true, multiValued = false)
    String mac = null;

    @Argument(index = 2, name = "vlan", description = "Vlan",
            required = true, multiValued = false)
    short vlan;

    @Argument(index = 3, name = "hostLocationDeviceId", description = "Host location device ID",
            required = true, multiValued = false)
    String hostLocationDeviceId;

    @Argument(index = 4, name = "hostLocationPortNumber", description = "Host location port number",
            required = true, multiValued = false)
    long hostLocationPortNumber;

    // ip addresses
    @Option(name = "--hostIp", description = "Host IP addresses.  Can be specified multiple times.",
            required = false, multiValued = true)
    protected String[] hostIpStrings;

    @Override
    protected void execute() {
        VirtualNetworkAdminService service = get(VirtualNetworkAdminService.class);

        Set<IpAddress> hostIps = new HashSet<>();
        if (hostIpStrings != null) {
            Arrays.stream(hostIpStrings).forEach(s -> hostIps.add(IpAddress.valueOf(s)));
        }
        HostLocation hostLocation = new HostLocation(DeviceId.deviceId(hostLocationDeviceId),
                                                     PortNumber.portNumber(hostLocationPortNumber),
                                                     System.currentTimeMillis());
        MacAddress macAddress = MacAddress.valueOf(mac);
        VlanId vlanId = VlanId.vlanId(vlan);
        service.createVirtualHost(NetworkId.networkId(networkId),
                                  HostId.hostId(macAddress, vlanId), macAddress, vlanId,
                                  hostLocation, hostIps);
        print("Virtual host successfully created.");
    }
}
