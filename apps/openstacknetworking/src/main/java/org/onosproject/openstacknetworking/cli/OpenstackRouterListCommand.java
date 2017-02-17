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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.openstack.networking.domain.NeutronRouter;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

/**
 * Lists OpenStack routers.
 */
@Command(scope = "onos", name = "openstack-routers",
        description = "Lists all OpenStack routers")
public class OpenstackRouterListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-40s%-20s%-20s%-8s";

    private final OpenstackRouterService routerService =
            AbstractShellCommand.get(OpenstackRouterService.class);
    private final OpenstackNetworkService netService =
            AbstractShellCommand.get(OpenstackNetworkService.class);

    @Override
    protected void execute() {
        List<Router> routers = Lists.newArrayList(routerService.routers());
        routers.sort(Comparator.comparing(Router::getName));

        if (outputJson()) {
            try {
                print("%s", mapper().writeValueAsString(json(routers)));
            } catch (JsonProcessingException e) {
                print("Failed to list routers in JSON format");
            }
            return;
        }
        print(FORMAT, "ID", "Name", "External", "Internal");
        for (Router router: routers) {
            String exNetId = router.getExternalGatewayInfo() != null ?
                router.getExternalGatewayInfo().getNetworkId() : null;

            List<String> externals = Lists.newArrayList();
            if (exNetId != null) {
                // FIXME fix openstack4j to provide external gateway ip info
                externals = netService.ports(exNetId).stream()
                        .filter(port -> Objects.equals(port.getDeviceId(),
                                router.getId()))
                        .flatMap(port -> port.getFixedIps().stream())
                        .map(IP::getIpAddress)
                        .collect(Collectors.toList());
            }

            List<String> internals = Lists.newArrayList();
            routerService.routerInterfaces(router.getId()).forEach(iface -> {
                    internals.add(getRouterIfaceIp(iface));
            });
            print(FORMAT, router.getId(), router.getName(), externals, internals);
        }
    }

    private String getRouterIfaceIp(RouterInterface iface) {
        Port osPort = netService.port(iface.getPortId());
        IP ipAddr = osPort.getFixedIps().stream()
                .filter(ip -> ip.getSubnetId().equals(iface.getSubnetId()))
                .findAny().orElse(null);
        return ipAddr == null ? "" : ipAddr.getIpAddress();
    }

    private JsonNode json(List<Router> routers) {
        ArrayNode result = mapper().enable(INDENT_OUTPUT).createArrayNode();
        for (Router router: routers) {
            result.add(writeRouter(router));
        }
        return result;
    }

    private ObjectNode writeRouter(Router osRouter) {
        try {
            String strNet = ObjectMapperSingleton.getContext(NeutronRouter.class)
                    .writerFor(NeutronRouter.class)
                    .writeValueAsString(osRouter);
            return (ObjectNode) mapper().readTree(strNet.getBytes());
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }
}
