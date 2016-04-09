/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.vtnrsc.BindingHostId;
import org.onosproject.vtnrsc.DefaultVirtualPort;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.Subnet;
import org.onosproject.vtnrsc.TenantNetwork;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.subnet.SubnetService;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Supports for updating the external gateway virtualPort.
 */
@Command(scope = "onos", name = "externalgateway-update",
        description = "Supports for updating the external gateway virtualPort.")
public class VirtualPortExGwUpdateCommand extends AbstractShellCommand {

    @Option(name = "-m", aliases = "--macAddress", description = "MAC address.", required = true,
            multiValued = false)
    String macAddress = "";

    @Override
    protected void execute() {
        VirtualPortService service = get(VirtualPortService.class);
        SubnetService subnetService = get(SubnetService.class);
        TenantNetworkService tenantNetworkService = get(TenantNetworkService.class);
        Iterable<TenantNetwork> networks = tenantNetworkService.getNetworks();
        if (networks != null) {
            for (TenantNetwork network : networks) {
                if (network.routerExternal()) {
                    Iterable<Subnet> subnets = subnetService.getSubnets();
                    if (subnets != null) {
                        for (Subnet subnet : subnets) {
                            if (network.id().networkId().equals(subnet.networkId().networkId())) {
                                IpAddress exgwip = subnet.gatewayIp();
                                FixedIp fixedGwIp = FixedIp.fixedIp(subnet.id(), exgwip);
                                VirtualPort exgwPort = service.getPort(fixedGwIp);
                                if (exgwPort == null) {
                                    createExGwPort(network, subnet, fixedGwIp);
                                } else {
                                    updateExGwPort(exgwPort);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void createExGwPort(TenantNetwork network, Subnet subnet, FixedIp fixedGwIp) {
        VirtualPortService service = get(VirtualPortService.class);
        Map<String, String> strMap = Maps.newHashMap();
        VirtualPort virtualPort = new DefaultVirtualPort(VirtualPortId.portId("externalgateway-update-id"),
                                                         network.id(),
                                                         false, strMap,
                                                         VirtualPort.State.DOWN,
                                                         MacAddress.valueOf(macAddress),
                                                         subnet.tenantId(),
                                                         DeviceId.deviceId(""),
                                                         Sets.newHashSet(fixedGwIp),
                                                         BindingHostId.bindingHostId(""),
                                                         Sets.newHashSet(),
                                                         Sets.newHashSet());
        Set<VirtualPort> virtualPorts = Sets.newHashSet(virtualPort);
        service.createPorts(virtualPorts);
    }

    private void updateExGwPort(VirtualPort exgwPort) {
        VirtualPortService service = get(VirtualPortService.class);
        Map<String, String> strMap = Maps.newHashMap();
        strMap.putIfAbsent("name", exgwPort.name());
        strMap.putIfAbsent("deviceOwner", exgwPort.deviceOwner());
        strMap.putIfAbsent("bindingvnicType", exgwPort.bindingVnicType());
        strMap.putIfAbsent("bindingvifType", exgwPort.bindingVifType());
        strMap.putIfAbsent("bindingvnicDetails", exgwPort.bindingVifDetails());
        VirtualPort virtualPort = new DefaultVirtualPort(exgwPort.portId(),
                                                         exgwPort.networkId(),
                                                         false, strMap,
                                                         VirtualPort.State.DOWN,
                                                         MacAddress.valueOf(macAddress),
                                                         exgwPort.tenantId(),
                                                         exgwPort.deviceId(),
                                                         exgwPort.fixedIps(),
                                                         exgwPort.bindingHostId(),
                                                         Sets.newHashSet(exgwPort
                                                                .allowedAddressPairs()),
                                                         Sets.newHashSet(exgwPort
                                                                .securityGroups()));
        Set<VirtualPort> virtualPorts = Sets.newHashSet(virtualPort);
        service.updatePorts(virtualPorts);
    }
}
