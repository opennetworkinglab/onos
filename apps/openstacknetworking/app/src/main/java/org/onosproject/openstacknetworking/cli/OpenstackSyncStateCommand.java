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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupAdminService;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.client.IOSClientBuilder;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.api.types.Facing;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.networking.domain.NeutronRouterInterface;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

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

    @Argument(index = 4, name = "perspective", description = "OpenStack endpoint perspective",
            required = false, multiValued = false)
    private String perspective = null;

    private static final String DOMAIN_DEFUALT = "default";

    private static final String SECURITY_GROUP_FORMAT = "%-40s%-20s";
    private static final String NETWORK_FORMAT = "%-40s%-20s%-20s%-8s";
    private static final String SUBNET_FORMAT = "%-40s%-20s%-20s";
    private static final String PORT_FORMAT = "%-40s%-20s%-20s%-8s";
    private static final String ROUTER_FORMAT = "%-40s%-20s%-20s%-8s";
    private static final String FLOATING_IP_FORMAT = "%-40s%-20s%-20s";

    private static final String DEVICE_OWNER_GW = "network:router_gateway";
    private static final String DEVICE_OWNER_IFACE = "network:router_interface";

    private static final String KEYSTONE_V2 = "v2.0";
    private static final String KEYSTONE_V3 = "v3";

    @Override
    protected void execute() {
        OpenstackSecurityGroupAdminService osSgAdminService = get(OpenstackSecurityGroupAdminService.class);
        OpenstackNetworkAdminService osNetAdminService = get(OpenstackNetworkAdminService.class);
        OpenstackRouterAdminService osRouterAdminService = get(OpenstackRouterAdminService.class);

        OSClient osClient;

        Config config = getSslConfig();

        try {
            if (endpoint != null) {
                if (endpoint.contains(KEYSTONE_V2)) {
                    IOSClientBuilder.V2 builder = OSFactory.builderV2()
                            .endpoint(this.endpoint)
                            .tenantName(this.tenant)
                            .credentials(this.user, this.password)
                            .withConfig(config);

                    if (perspective != null) {
                        builder.perspective(getFacing(perspective));
                    }

                    osClient = builder.authenticate();
                } else if (endpoint.contains(KEYSTONE_V3)) {

                    Identifier project = Identifier.byName(this.tenant);
                    Identifier domain = Identifier.byName(DOMAIN_DEFUALT);

                    IOSClientBuilder.V3 builder = OSFactory.builderV3()
                            .endpoint(this.endpoint)
                            .credentials(this.user, this.password, domain)
                            .scopeToProject(project, domain)
                            .withConfig(config);

                    if (perspective != null) {
                        builder.perspective(getFacing(perspective));
                    }

                    osClient = builder.authenticate();
                } else {
                    print("Unrecognized keystone version type");
                    return;
                }
            } else {
                print("Need to specify a valid endpoint");
                return;
            }
        } catch (AuthenticationException e) {
            print("Authentication failed");
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

    // TODO fix the logic to add router interface to router
    private void addRouterIface(Port osPort, OpenstackRouterAdminService adminService) {
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
                if (adminService.routerInterface(rIface.getPortId()) != null) {
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

    private Config getSslConfig() {
        // we bypass the SSL certification verification for now
        // TODO: verify server side SSL using a given certification
        Config config = Config.newConfig().withSSLVerificationDisabled();

        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        HostnameVerifier allHostsValid = (hostname, session) -> true;

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            config.withSSLContext(sc);
        } catch (Exception e) {
            print("Failed to access OpenStack service");
            return null;
        }

        return config;
    }

    private Facing getFacing(String strFacing) {

        if (strFacing == null) {
            return null;
        }

        switch (strFacing) {
            case "public":
                return Facing.PUBLIC;
            case "admin":
                return Facing.ADMIN;
            case "internal":
                return Facing.INTERNAL;
            default:
                return null;
        }
    }
}
