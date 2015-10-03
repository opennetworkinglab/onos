/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.vtnrsc.cli.virtualport;

import java.util.Map;
import java.util.Set;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.MacAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.vtnrsc.AllowedAddressPair;
import org.onosproject.vtnrsc.BindingHostId;
import org.onosproject.vtnrsc.DefaultVirtualPort;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.SecurityGroup;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Supports for creating a virtualPort.
 */
@Command(scope = "onos", name = "virtualport-create",
        description = "Supports for creating a virtualPort.")
public class VirtualPortCreateCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "id", description = "virtualPort id.", required = true,
            multiValued = false)
    String id = null;

    @Argument(index = 1, name = "networkId", description = "network id.", required = true,
            multiValued = false)
    String networkId = null;

    @Argument(index = 2, name = "name", description = "virtualPort name.", required = true,
            multiValued = false)
    String name = null;

    @Argument(index = 3, name = "tenantId", description = "tenant id.", required = true,
            multiValued = false)
    String tenantId = null;

    @Argument(index = 4, name = "deviceId", description = "device id.", required = true,
            multiValued = false)
    String deviceId = null;

    @Option(name = "-a", aliases = "--adminStateUp",
            description = "administrative status of the virtualPort which is true or false.",
            required = false, multiValued = false)
    Boolean adminStateUp = false;

    @Option(name = "-s", aliases = "--state", description = "virtualPort state.", required = false,
            multiValued = false)
    String state = null;

    @Option(name = "-m", aliases = "--macAddress", description = "MAC address.", required = false,
            multiValued = false)
    String macAddress = "";

    @Option(name = "-d", aliases = "--deviceOwner", description = "ID of the entity that uses this "
            + "virtualPort.", required = false, multiValued = false)
    String deviceOwner = null;

    @Option(name = "-f", aliases = "--fixedIp",
            description = "The IP address for the port,include the IP address "
                    + "and subnet identity.", required = false, multiValued = false)
    FixedIp fixedIp = null;

    @Option(name = "-i", aliases = "--bindingHostId", description = "virtualPort bindingHostId.",
            required = false, multiValued = false)
    String bindingHostId = null;

    @Option(name = "-t", aliases = "--bindingvnicType", description = "virtualPort bindingvnicType.",
            required = false, multiValued = false)
    String bindingvnicType = null;

    @Option(name = "-v", aliases = "--bindingvifType", description = "virtualPort bindingvifType.",
            required = false, multiValued = false)
    String bindingvifType = null;

    @Option(name = "-b", aliases = "--bindingvnicDetails",
            description = "virtualPort bindingvnicDetails.", required = false, multiValued = false)
    String bindingvnicDetails = null;

    @Option(name = "-l", aliases = "--allowedAddress", description = "virtual allowedAddressPair.",
            required = false, multiValued = false)
    Set<AllowedAddressPair> allowedAddressPairs = Sets.newHashSet();

    @Option(name = "-e", aliases = "--securityGroups", description = "virtualPort securityGroups.",
            required = false, multiValued = false)
    Set<SecurityGroup> securityGroups = Sets.newHashSet();

    @Override
    protected void execute() {
        Map<String, String> strMap = Maps.newHashMap();
        strMap.putIfAbsent("name", name);
        strMap.putIfAbsent("deviceOwner", deviceOwner);
        strMap.putIfAbsent("bindingvnicType", bindingvnicType);
        strMap.putIfAbsent("bindingvifType", bindingvifType);
        strMap.putIfAbsent("bindingvnicDetails", bindingvnicDetails);
        VirtualPortService service = get(VirtualPortService.class);
        VirtualPort virtualPort = new DefaultVirtualPort(VirtualPortId.portId(id),
                                       TenantNetworkId.networkId(networkId),
                                       false, strMap, VirtualPort.State.ACTIVE,
                                       MacAddress.valueOf(macAddress),
                                       TenantId.tenantId(tenantId),
                                       DeviceId.deviceId(deviceId), Sets.newHashSet(fixedIp),
                                       BindingHostId.bindingHostId(bindingHostId),
                                       allowedAddressPairs, securityGroups);
        Set<VirtualPort> virtualPorts = Sets.newHashSet(virtualPort);
        service.createPorts(virtualPorts);
    }
}
