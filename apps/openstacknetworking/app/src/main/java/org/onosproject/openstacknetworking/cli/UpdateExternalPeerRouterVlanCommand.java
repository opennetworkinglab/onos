/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.cli;

import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;

import java.util.List;

/**
 * Updates external peer router macc address.
 */
@Command(scope = "onos", name = "openstack-update-peer-router-vlan",
        description = "Updates external peer router vlan")
public class UpdateExternalPeerRouterVlanCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "ip address", description = "ip address",
            required = true, multiValued = false)
    private String ipAddress = null;

    @Argument(index = 1, name = "vlan id", description = "vlan id",
            required = true, multiValued = false)
    private String vlanId = null;

    private static final String FORMAT = "%-20s%-20s%-20s";
    private static final String NO_ELEMENT = "There's no external peer router information with given ip address";
    private static final String NONE = "None";

    @Override
    protected void execute() {
        OpenstackNetworkAdminService osNetAdminService = AbstractShellCommand.get(OpenstackNetworkAdminService.class);
        OpenstackRouterService osRouterService = AbstractShellCommand.get(OpenstackRouterService.class);

        IpAddress externalPeerIpAddress = IpAddress.valueOf(
                IpAddress.Version.INET, Ip4Address.valueOf(ipAddress).toOctets());

        if (osNetAdminService.externalPeerRouters().isEmpty()) {
            print(NO_ELEMENT);
            return;
        } else if (osNetAdminService.externalPeerRouters().stream()
                .noneMatch(router -> router.externalPeerRouterIp().toString().equals(ipAddress))) {
            print(NO_ELEMENT);
            return;
        }

        Subnet subnet = osNetAdminService.subnets().stream()
                .filter(s -> s.getGateway().equals(ipAddress))
                .findAny().orElse(null);
        if (subnet == null) {
            return;
        }

        Network network = osNetAdminService.network(subnet.getNetworkId());
        if (network == null) {
            return;
        }

        Router router = osRouterService.routers().stream()
                .filter(r -> r.getExternalGatewayInfo().getNetworkId().equals(network.getId()))
                .findAny().orElse(null);

        if (router == null) {
            return;
        }

        try {
            if (vlanId.equals(NONE)) {
                osNetAdminService.updateExternalPeerRouterVlan(externalPeerIpAddress, VlanId.NONE);
                osNetAdminService.deriveExternalPeerRouterMac(router.getExternalGatewayInfo(), router, VlanId.NONE);
            } else {
                osNetAdminService.updateExternalPeerRouterVlan(externalPeerIpAddress, VlanId.vlanId(vlanId));
                osNetAdminService.deriveExternalPeerRouterMac(
                        router.getExternalGatewayInfo(), router, VlanId.vlanId(vlanId));

            }
        } catch (IllegalArgumentException e) {
            log.error("Exception occurred because of {}", e.toString());
        }

        print(FORMAT, "Router IP", "Mac Address", "VLAN ID");
        List<ExternalPeerRouter> routers = Lists.newArrayList(osNetAdminService.externalPeerRouters());

        for (ExternalPeerRouter r: routers) {
            print(FORMAT, r.externalPeerRouterIp(),
                    r.externalPeerRouterMac().toString(),
                    r.externalPeerRouterVlanId());
        }
    }
}
