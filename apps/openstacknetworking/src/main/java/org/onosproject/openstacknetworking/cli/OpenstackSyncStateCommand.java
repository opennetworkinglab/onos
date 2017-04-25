/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupAdminService;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupService;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.model.identity.Access;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.networking.domain.NeutronRouterInterface;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.openstack4j.core.transport.ObjectMapperSingleton.getContext;

/**
 * Synchronizes OpenStack network states.
 */
@Command(scope = "onos", name = "openstack-sync-states",
        description = "Synchronizes all OpenStack network states")
public class OpenstackSyncStateCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "endpoint", description = "OpenStack service endpoint",
            required = true, multiValued = false)
    private String endpoint = null;

    @Argument(index = 1, name = "tenant", description = "OpenStack admin tenant name",
            required = true, multiValued = false)
    private String tenant = null;

    @Argument(index = 2, name = "user", description = "OpenStack admin user name",
            required = true, multiValued = false)
    private String user = null;

    @Argument(index = 3, name = "password", description = "OpenStack admin user password",
            required = true, multiValued = false)
    private String password = null;

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
        OpenstackSecurityGroupService osSgService = get(OpenstackSecurityGroupService.class);
        OpenstackNetworkAdminService osNetAdminService = get(OpenstackNetworkAdminService.class);
        OpenstackNetworkService osNetService = get(OpenstackNetworkService.class);
        OpenstackRouterAdminService osRouterAdminService = get(OpenstackRouterAdminService.class);
        OpenstackRouterService osRouterService = get(OpenstackRouterService.class);

        Access osAccess;
        try {
            osAccess = OSFactory.builder()
                    .endpoint(this.endpoint)
                    .tenantName(this.tenant)
                    .credentials(this.user, this.password)
                    .authenticate()
                    .getAccess();
        } catch (AuthenticationException e) {
            print("Authentication failed");
            return;
        } catch (Exception e) {
            print("Failed to access OpenStack service");
            return;
        }

        OSClient osClient = OSFactory.clientFromAccess(osAccess);

        print("Synchronizing OpenStack security groups");
        print(SECURITY_GROUP_FORMAT, "ID", "Name");
        osClient.networking().securitygroup().list().forEach(osSg -> {
            if (osSgService.securityGroup(osSg.getId()) != null) {
                osSgAdminService.updateSecurityGroup(osSg);
            } else {
                osSgAdminService.createSecurityGroup(osSg);
            }
            print(SECURITY_GROUP_FORMAT, osSg.getId(), osSg.getName());
        });

        print("\nSynchronizing OpenStack networks");
        print(NETWORK_FORMAT, "ID", "Name", "VNI", "Subnets");
        osClient.networking().network().list().forEach(osNet -> {
            if (osNetService.network(osNet.getId()) != null) {
                osNetAdminService.updateNetwork(osNet);
            } else {
                osNetAdminService.createNetwork(osNet);
            }
            printNetwork(osNet);
        });

        print("\nSynchronizing OpenStack subnets");
        print(SUBNET_FORMAT, "ID", "Network", "CIDR");
        osClient.networking().subnet().list().forEach(osSubnet -> {
            if (osNetService.subnet(osSubnet.getId()) != null) {
                osNetAdminService.updateSubnet(osSubnet);
            } else {
                osNetAdminService.createSubnet(osSubnet);
            }
            printSubnet(osSubnet, osNetService);
        });

        print("\nSynchronizing OpenStack ports");
        print(PORT_FORMAT, "ID", "Network", "MAC", "Fixed IPs");
        osClient.networking().port().list().forEach(osPort -> {
            if (osNetService.port(osPort.getId()) != null) {
                osNetAdminService.updatePort(osPort);
            } else {
                osNetAdminService.createPort(osPort);
            }
            printPort(osPort, osNetService);
        });

        print("\nSynchronizing OpenStack routers");
        print(ROUTER_FORMAT, "ID", "Name", "External", "Internal");
        osClient.networking().router().list().forEach(osRouter -> {
            if (osRouterService.router(osRouter.getId()) != null) {
                osRouterAdminService.updateRouter(osRouter);
            } else {
                osRouterAdminService.createRouter(osRouter);
            }

            // FIXME do we need to manage router interfaces separately?
            osNetService.ports().stream()
                    .filter(osPort -> Objects.equals(osPort.getDeviceId(), osRouter.getId()) &&
                            Objects.equals(osPort.getDeviceOwner(), DEVICE_OWNER_IFACE))
                    .forEach(osPort -> addRouterIface(osPort, osRouterService,
                            osRouterAdminService));

            printRouter(osRouter, osNetService);
        });

        print("\nSynchronizing OpenStack floating IPs");
        print(FLOATING_IP_FORMAT, "ID", "Floating IP", "Fixed IP");
        osClient.networking().floatingip().list().forEach(osFloating -> {
            if (osRouterService.floatingIp(osFloating.getId()) != null) {
                osRouterAdminService.updateFloatingIp(osFloating);
            } else {
                osRouterAdminService.createFloatingIp(osFloating);
            }
            printFloatingIp(osFloating);
        });
    }

    // TODO fix the logic to add router interface to router
    private void addRouterIface(Port osPort, OpenstackRouterService service,
                                OpenstackRouterAdminService adminService) {
        osPort.getFixedIps().forEach(p -> {
            JsonNode jsonTree = mapper().createObjectNode()
                    .put("id", osPort.getDeviceId())
                    .put("tenant_id", osPort.getTenantId())
                    .put("subnet_id", p.getSubnetId())
                    .put("port_id", osPort.getId());
            try {
                RouterInterface rIface = getContext(NeutronRouterInterface.class)
                        .readerFor(NeutronRouterInterface.class)
                        .readValue(jsonTree);
                if (service.routerInterface(rIface.getPortId()) != null) {
                    adminService.updateRouterInterface(rIface);
                } else {
                    adminService.addRouterInterface(rIface);
                }
            } catch (IOException ignore) {
            }
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

    private void printSubnet(Subnet osSubnet, OpenstackNetworkService osNetService) {
        final String strSubnet = String.format(SUBNET_FORMAT,
                osSubnet.getId(),
                osNetService.network(osSubnet.getNetworkId()).getName(),
                osSubnet.getCidr());
        print(strSubnet);
    }

    private void printPort(Port osPort, OpenstackNetworkService osNetService) {
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

    private void printRouter(Router osRouter, OpenstackNetworkService osNetService) {
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
                floatingIp.getFixedIpAddress(),
                Strings.isNullOrEmpty(floatingIp.getFixedIpAddress()) ?
                        "" : floatingIp.getFixedIpAddress());
        print(strFloating);
    }
}
