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
package org.onosproject.openstacknetworking.cli;

import com.google.common.base.Strings;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupAdminService;
import org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.addRouterIface;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.CONTROLLER;

/**
 * Synchronizes OpenStack network states.
 */
@Command(scope = "onos", name = "openstack-sync-states",
        description = "Synchronizes all OpenStack network states")
public class OpenstackSyncStateCommand extends AbstractShellCommand {

    private static final String SECURITY_GROUP_FORMAT = "%-40s%-20s";
    private static final String NETWORK_FORMAT = "%-40s%-20s%-20s%-8s";
    private static final String SUBNET_FORMAT = "%-40s%-20s%-20s";
    private static final String PORT_FORMAT = "%-40s%-20s%-20s%-8s";
    private static final String ROUTER_FORMAT = "%-40s%-20s%-20s%-8s";
    private static final String FLOATING_IP_FORMAT = "%-40s%-20s%-20s";

    private static final String DEVICE_OWNER_GW = "network:router_gateway";
    private static final String DEVICE_OWNER_IFACE = "network:router_interface";

    @Override
    protected void execute() {
        OpenstackSecurityGroupAdminService osSgAdminService = get(OpenstackSecurityGroupAdminService.class);
        OpenstackNetworkAdminService osNetAdminService = get(OpenstackNetworkAdminService.class);
        OpenstackRouterAdminService osRouterAdminService = get(OpenstackRouterAdminService.class);
        OpenstackNodeService osNodeService = get(OpenstackNodeService.class);

        Optional<OpenstackNode> node = osNodeService.nodes(CONTROLLER).stream().findFirst();
        if (!node.isPresent()) {
            error("Keystone auth info has not been configured. " +
                    "Please specify auth info via network-cfg.json.");
            return;
        }

        OSClient osClient = OpenstackNetworkingUtil.getConnectedClient(node.get());

        if (osClient == null) {
            return;
        }

        print("Synchronizing OpenStack security groups");
        print(SECURITY_GROUP_FORMAT, "ID", "Name");
        osClient.networking().securitygroup().list().forEach(osSg -> {
            if (osSgAdminService.securityGroup(osSg.getId()) != null) {
                osSgAdminService.updateSecurityGroup(osSg);
            } else {
                osSgAdminService.createSecurityGroup(osSg);
            }
            print(SECURITY_GROUP_FORMAT, osSg.getId(), osSg.getName());
        });

        print("\nSynchronizing OpenStack networks");
        print(NETWORK_FORMAT, "ID", "Name", "VNI", "Subnets");
        osClient.networking().network().list().forEach(osNet -> {
            if (osNetAdminService.network(osNet.getId()) != null) {
                osNetAdminService.updateNetwork(osNet);
            } else {
                osNetAdminService.createNetwork(osNet);
            }
            printNetwork(osNet);
        });

        print("\nSynchronizing OpenStack subnets");
        print(SUBNET_FORMAT, "ID", "Network", "CIDR");
        osClient.networking().subnet().list().forEach(osSubnet -> {
            if (osNetAdminService.subnet(osSubnet.getId()) != null) {
                osNetAdminService.updateSubnet(osSubnet);
            } else {
                osNetAdminService.createSubnet(osSubnet);
            }
            printSubnet(osSubnet, osNetAdminService);
        });

        print("\nSynchronizing OpenStack ports");
        print(PORT_FORMAT, "ID", "Network", "MAC", "Fixed IPs");
        osClient.networking().port().list().forEach(osPort -> {
            if (osNetAdminService.port(osPort.getId()) != null) {
                osNetAdminService.updatePort(osPort);
            } else {
                osNetAdminService.createPort(osPort);
            }
            printPort(osPort, osNetAdminService);
        });

        print("\nSynchronizing OpenStack routers");
        print(ROUTER_FORMAT, "ID", "Name", "External", "Internal");
        osClient.networking().router().list().forEach(osRouter -> {
            if (osRouterAdminService.router(osRouter.getId()) != null) {
                osRouterAdminService.updateRouter(osRouter);
            } else {
                osRouterAdminService.createRouter(osRouter);
            }

            // FIXME do we need to manage router interfaces separately?
            osNetAdminService.ports().stream()
                    .filter(osPort -> Objects.equals(osPort.getDeviceId(), osRouter.getId()) &&
                            Objects.equals(osPort.getDeviceOwner(), DEVICE_OWNER_IFACE))
                    .forEach(osPort -> addRouterIface(osPort, osRouterAdminService));

            printRouter(osRouter, osNetAdminService);
        });

        print("\nSynchronizing OpenStack floating IPs");
        print(FLOATING_IP_FORMAT, "ID", "Floating IP", "Fixed IP");
        osClient.networking().floatingip().list().forEach(osFloating -> {
            if (osRouterAdminService.floatingIp(osFloating.getId()) != null) {
                osRouterAdminService.updateFloatingIp(osFloating);
            } else {
                osRouterAdminService.createFloatingIp(osFloating);
            }
            printFloatingIp(osFloating);
        });
    }

    private void printNetwork(Network osNet) {
        final String strNet = String.format(NETWORK_FORMAT,
                osNet.getId(),
                osNet.getName(),
                osNet.getProviderSegID(),
                osNet.getSubnets());
        print(strNet);
    }

    private void printSubnet(Subnet osSubnet, OpenstackNetworkAdminService osNetService) {
        final String strSubnet = String.format(SUBNET_FORMAT,
                osSubnet.getId(),
                osNetService.network(osSubnet.getNetworkId()).getName(),
                osSubnet.getCidr());
        print(strSubnet);
    }

    private void printPort(Port osPort, OpenstackNetworkAdminService osNetService) {
        List<String> fixedIps = osPort.getFixedIps().stream()
                .map(IP::getIpAddress)
                .collect(Collectors.toList());
        final String strPort = String.format(PORT_FORMAT,
                osPort.getId(),
                osNetService.network(osPort.getNetworkId()).getName(),
                osPort.getMacAddress(),
                fixedIps.isEmpty() ? "" : fixedIps);
        print(strPort);
    }

    private void printRouter(Router osRouter, OpenstackNetworkAdminService osNetService) {
        List<String> externals = osNetService.ports().stream()
                .filter(osPort -> Objects.equals(osPort.getDeviceId(), osRouter.getId()) &&
                        Objects.equals(osPort.getDeviceOwner(), DEVICE_OWNER_GW))
                .flatMap(osPort -> osPort.getFixedIps().stream())
                .map(IP::getIpAddress)
                .collect(Collectors.toList());

        List<String> internals = osNetService.ports().stream()
                .filter(osPort -> Objects.equals(osPort.getDeviceId(), osRouter.getId()) &&
                        Objects.equals(osPort.getDeviceOwner(), DEVICE_OWNER_IFACE))
                .flatMap(osPort -> osPort.getFixedIps().stream())
                .map(IP::getIpAddress)
                .collect(Collectors.toList());

        final String strRouter = String.format(ROUTER_FORMAT,
                osRouter.getId(),
                osRouter.getName(),
                externals.isEmpty() ? "" : externals,
                internals.isEmpty() ? "" : internals);
        print(strRouter);
    }

    private void printFloatingIp(NetFloatingIP floatingIp) {
        final String strFloating = String.format(FLOATING_IP_FORMAT,
                floatingIp.getId(),
                floatingIp.getFloatingIpAddress(),
                Strings.isNullOrEmpty(floatingIp.getFixedIpAddress()) ?
                        "" : floatingIp.getFixedIpAddress());
        print(strFloating);
    }
}
