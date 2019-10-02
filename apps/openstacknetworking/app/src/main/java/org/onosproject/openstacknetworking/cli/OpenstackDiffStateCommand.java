/*
 * Copyright 2019-present Open Networking Foundation
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
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupAdminService;
import org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.IdEntity;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.RouterInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.openstacknetworking.api.Constants.FLOATING_IP_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.NETWORK_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.PORT_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.ROUTER_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.ROUTER_INTF_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.SECURITY_GROUP_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.SUBNET_FORMAT;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.printFloatingIp;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.printNetwork;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.printPort;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.printRouter;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.printRouterIntf;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.printSecurityGroup;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.printSubnet;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.CONTROLLER;

/**
 * Compares cached network state diff against neutron database.
 */
@Service
@Command(scope = "onos", name = "openstack-diff-state",
        description = "Compares cached network state with neutron database.")
public class OpenstackDiffStateCommand extends AbstractShellCommand {

    private static final String HTTP_HEADER_ACCEPT = "accept";
    private static final String HTTP_HEADER_VALUE_JSON = "application/json";

    @Option(name = "-s", aliases = "--show",
            description = "Shows the differences between cached network state with neutron database.",
            required = false, multiValued = false)
    private boolean show = false;

    @Option(name = "-c", aliases = "--clear",
            description = "Clears the differences between cached network state with neutron database.",
            required = false, multiValued = false)
    private boolean clear = false;

    @Override
    protected void doExecute() {
        OpenstackSecurityGroupAdminService osSgService =
                get(OpenstackSecurityGroupAdminService.class);
        OpenstackNetworkAdminService osNetService =
                get(OpenstackNetworkAdminService.class);
        OpenstackRouterAdminService osRouterService =
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

        if (!clear) {
            show = true;
        }

        if (show && clear) {
            error("Either --show (-s) or --clear (-c) option should be specified.");
        }

        if (show) {
            print("\nComparing OpenStack floating IPs");
        } else if (clear) {
            print("\nClearing OpenStack floating IPs");
        }
        Set<String> cachedFips = osRouterService.floatingIps().stream()
                .map(NetFloatingIP::getId).collect(Collectors.toSet());
        Set<String> osFips = osClient.headers(headerMap).networking()
                .floatingip().list().stream().map(NetFloatingIP::getId)
                .collect(Collectors.toSet());

        print(FLOATING_IP_FORMAT, "ID", "Floating IP", "Fixed IP");
        getDiff(cachedFips, osFips).forEach(fipId -> {
            printFloatingIp(osRouterService.floatingIp(fipId));
            if (clear) {
                osRouterService.removeFloatingIp(fipId);
            }
        });

        Set<String> cachedPorts = osNetService.ports().stream()
                .map(IdEntity::getId).collect(Collectors.toSet());
        Set<String> osPorts = osClient.headers(headerMap).networking()
                .port().list().stream().map(IdEntity::getId)
                .collect(Collectors.toSet());

        if (show) {
            print("\nComparing OpenStack router interfaces");
        } else if (clear) {
            print("\nClearing OpenStack router interfaces");
        }
        print(ROUTER_INTF_FORMAT, "ID", "Tenant ID", "Subnet ID");
        getDiff(cachedPorts, osPorts).forEach(portId -> {
            RouterInterface ri = osRouterService.routerInterface(portId);
            if (ri != null) {
                printRouterIntf(ri);
                if (clear) {
                    osRouterService.removeRouterInterface(portId);
                }
            }
        });

        if (show) {
            print("\nComparing OpenStack ports");
        } else if (clear) {
            print("\nClearing OpenStack ports");
        }
        print(PORT_FORMAT, "ID", "Network", "MAC", "Fixed IPs");
        getDiff(cachedPorts, osPorts).forEach(portId -> {
            printPort(osNetService.port(portId), osNetService);
            if (clear) {
                osNetService.removePort(portId);
            }
        });

        if (show) {
            print("\nComparing OpenStack routers");
        } else if (clear) {
            print("\nClearing OpenStack routers");
        }
        Set<String> cachedRouters = osRouterService.routers().stream()
                .map(IdEntity::getId).collect(Collectors.toSet());
        Set<String> osRouters = osClient.headers(headerMap).networking()
                .router().list().stream().map(IdEntity::getId)
                .collect(Collectors.toSet());

        print(ROUTER_FORMAT, "ID", "Name", "External", "Internal");
        getDiff(cachedRouters, osRouters).forEach(routerId -> {
            printRouter(osRouterService.router(routerId), osNetService);
            if (clear) {
                osRouterService.removeRouter(routerId);
            }
        });

        if (show) {
            print("\nComparing OpenStack subnets");
        } else if (clear) {
            print("\nClearing OpenStack subnets");
        }
        Set<String> cachedSubnets = osNetService.subnets().stream()
                .map(IdEntity::getId).collect(Collectors.toSet());
        Set<String> osSubnets = osClient.headers(headerMap).networking()
                .subnet().list().stream().map(IdEntity::getId)
                .collect(Collectors.toSet());

        print(SUBNET_FORMAT, "ID", "Network", "CIDR");
        getDiff(cachedSubnets, osSubnets).forEach(subnetId -> {
            printSubnet(osNetService.subnet(subnetId), osNetService);
            if (clear) {
                osNetService.removeSubnet(subnetId);
            }
        });

        if (show) {
            print("\nComparing OpenStack networks");
        } else if (clear) {
            print("\nClearing OpenStack networks");
        }
        Set<String> cachedNets = osNetService.networks().stream()
                .map(IdEntity::getId).collect(Collectors.toSet());
        Set<String> osNets = osClient.headers(headerMap).networking()
                .network().list().stream().map(IdEntity::getId)
                .collect(Collectors.toSet());

        print(NETWORK_FORMAT, "ID", "Name", "VNI", "Subnets");
        getDiff(cachedNets, osNets).forEach(netId -> {
            printNetwork(osNetService.network(netId));
            if (clear) {
                osNetService.removeNetwork(netId);
            }
        });

        if (show) {
            print("\nComparing OpenStack security groups");
        } else if (clear) {
            print("\nClearing OpenStack security groups");
        }
        Set<String> cachedSgs = osSgService.securityGroups().stream()
                .map(IdEntity::getId).collect(Collectors.toSet());
        Set<String> osSgs = osClient.headers(headerMap).networking()
                .securitygroup().list().stream().map(IdEntity::getId)
                .collect(Collectors.toSet());

        print(SECURITY_GROUP_FORMAT, "ID", "Name");
        getDiff(cachedSgs, osSgs).forEach(sgId -> {
            printSecurityGroup(osSgService.securityGroup(sgId));
            if (clear) {
                osSgService.removeSecurityGroup(sgId);
            }
        });
    }

    private Set<String> getDiff(Set<String> orig, Set<String> comp) {
        return orig.stream().filter(id -> !comp.contains(id))
                .collect(Collectors.toSet());
    }
}
