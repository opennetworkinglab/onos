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
package org.onosproject.vtnrsc.cli.subnet;

import java.util.Set;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpAddress.Version;
import org.onlab.packet.IpPrefix;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.vtnrsc.AllocationPool;
import org.onosproject.vtnrsc.DefaultSubnet;
import org.onosproject.vtnrsc.HostRoute;
import org.onosproject.vtnrsc.Subnet;
import org.onosproject.vtnrsc.Subnet.Mode;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.subnet.SubnetService;

import com.google.common.collect.Sets;

/**
 * Supports for updating a subnet.
 */
@Command(scope = "onos", name = "subnet-update", description = "Supports for updating a subnet")
public class SubnetUpdateCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "id", description = "Subnet Id", required = true,
            multiValued = false)
    String id = null;

    @Argument(index = 1, name = "subnetName", description = "Subnet String name", required = true,
            multiValued = false)
    String subnetName = null;

    @Argument(index = 2, name = "networkId", description = "Subnet Network Id", required = true,
            multiValued = false)
    String networkId = null;

    @Argument(index = 3, name = "tenantId", description = "Subnet Tenant Id", required = true,
            multiValued = false)
    String tenantId = null;

    @Option(name = "-i", aliases = "--ipVersion", description = "Subnet Version ipVersion",
            required = false, multiValued = false)
    Version ipVersion = null;

    @Option(name = "-c", aliases = "--cidr", description = "Subnet IpPrefix cidr", required = false,
            multiValued = false)
    String cidr = "0.0.0.0/0";

    @Option(name = "-g", aliases = "--gatewayIp", description = "Subnet IpAddress gatewayIp",
            required = false, multiValued = false)
    String gatewayIp = "0.0.0.0";

    @Option(name = "-d", aliases = "--dhcpEnabled", description = "Subnet boolean dhcpEnabled",
            required = false, multiValued = false)
    boolean dhcpEnabled = false;

    @Option(name = "-s", aliases = "--shared", description = "Subnet boolean shared", required = false,
            multiValued = false)
    boolean shared = false;

    @Option(name = "-m", aliases = "--ipV6AddressMode", description = "Subnet Mode ipV6AddressMode",
            required = false, multiValued = false)
    String ipV6AddressMode = null;

    @Option(name = "-r", aliases = "--ipV6RaMode", description = "Subnet Mode ipV6RaMode",
            required = false, multiValued = false)
    String ipV6RaMode = null;

    @Option(name = "-h", aliases = "--hostRoutes", description = "Subnet jsonnode hostRoutes",
            required = false, multiValued = false)
    Set<HostRoute> hostRoutes = Sets.newHashSet();

    @Option(name = "-a", aliases = "--allocationPools",
            description = "Subnet jsonnode allocationPools", required = false, multiValued = false)
    Set<AllocationPool> allocationPools = Sets.newHashSet();

    @Override
    protected void execute() {
        SubnetService service = get(SubnetService.class);
        if (id == null || networkId == null || tenantId == null) {
            print("id,networkId,tenantId can not be null");
            return;
        }
        Subnet subnet = new DefaultSubnet(SubnetId.subnetId(id), subnetName,
                                          TenantNetworkId.networkId(networkId),
                                          TenantId.tenantId(tenantId), ipVersion,
                                          cidr == null ? null : IpPrefix.valueOf(cidr),
                                          gatewayIp == null ? null : IpAddress.valueOf(gatewayIp),
                                          dhcpEnabled, shared, hostRoutes,
                                          ipV6AddressMode == null ? null : Mode.valueOf(ipV6AddressMode),
                                          ipV6RaMode == null ? null : Mode.valueOf(ipV6RaMode),
                                          allocationPools);
        Set<Subnet> subnetsSet = Sets.newHashSet();
        subnetsSet.add(subnet);
        service.updateSubnets(subnetsSet);
    }

}
