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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupAdminService;
import org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.api.OSClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.onosproject.openstacknetworking.api.Constants.FLOATING_IP_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.NETWORK_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.PORT_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.ROUTER_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.SECURITY_GROUP_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.SUBNET_FORMAT;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.addRouterIface;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.printFloatingIp;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.printNetwork;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.printPort;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.printRouter;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.printSecurityGroup;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.printSubnet;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.CONTROLLER;

/**
 * Synchronizes OpenStack network states.
 */
@Service
@Command(scope = "onos", name = "openstack-sync-states",
        description = "Synchronizes all OpenStack network states")
public class OpenstackSyncStateCommand extends AbstractShellCommand {

    private static final String DEVICE_OWNER_GW = "network:router_gateway";
    private static final String DEVICE_OWNER_IFACE = "network:router_interface";

    private static final String HTTP_HEADER_ACCEPT = "accept";
    private static final String HTTP_HEADER_VALUE_JSON = "application/json";

    @Override
    protected void doExecute() {
        OpenstackSecurityGroupAdminService osSgAdminService =
                                    get(OpenstackSecurityGroupAdminService.class);
        OpenstackNetworkAdminService osNetAdminService =
                                    get(OpenstackNetworkAdminService.class);
        OpenstackRouterAdminService osRouterAdminService =
                                    get(OpenstackRouterAdminService.class);
        OpenstackNodeService osNodeService = get(OpenstackNodeService.class);

        Map<String, String> headerMap = new HashMap<>();
        headerMap.put(HTTP_HEADER_ACCEPT, HTTP_HEADER_VALUE_JSON);

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
        osClient.headers(headerMap).networking().securitygroup().list().forEach(osSg -> {
            if (osSgAdminService.securityGroup(osSg.getId()) != null) {
                osSgAdminService.updateSecurityGroup(osSg);
            } else {
                osSgAdminService.createSecurityGroup(osSg);
            }
            printSecurityGroup(osSg);
        });


        print("\nSynchronizing OpenStack networks");
        print(NETWORK_FORMAT, "ID", "Name", "VNI", "Subnets");
        osClient.headers(headerMap).networking().network().list().forEach(osNet -> {
            if (osNetAdminService.network(osNet.getId()) != null) {
                osNetAdminService.updateNetwork(osNet);
            } else {
                osNetAdminService.createNetwork(osNet);
            }
            printNetwork(osNet);
        });


        print("\nSynchronizing OpenStack subnets");
        print(SUBNET_FORMAT, "ID", "Network", "CIDR");
        osClient.headers(headerMap).networking().subnet().list().forEach(osSubnet -> {
            if (osNetAdminService.subnet(osSubnet.getId()) != null) {
                osNetAdminService.updateSubnet(osSubnet);
            } else {
                osNetAdminService.createSubnet(osSubnet);
            }
            printSubnet(osSubnet, osNetAdminService);
        });

        print("\nSynchronizing OpenStack ports");
        print(PORT_FORMAT, "ID", "Network", "MAC", "Fixed IPs");
        osClient.headers(headerMap).networking().port().list().forEach(osPort -> {
            if (osNetAdminService.port(osPort.getId()) != null) {
                osNetAdminService.updatePort(osPort);
            } else {
                osNetAdminService.createPort(osPort);
            }
            printPort(osPort, osNetAdminService);
        });

        print("\nSynchronizing OpenStack routers");
        print(ROUTER_FORMAT, "ID", "Name", "External", "Internal");
        osClient.headers(headerMap).networking().router().list().forEach(osRouter -> {
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
        osClient.headers(headerMap).networking().floatingip().list().forEach(osFloating -> {
            if (osRouterAdminService.floatingIp(osFloating.getId()) != null) {
                osRouterAdminService.updateFloatingIp(osFloating);
            } else {
                osRouterAdminService.createFloatingIp(osFloating);
            }
            printFloatingIp(osFloating);
        });
    }
}
