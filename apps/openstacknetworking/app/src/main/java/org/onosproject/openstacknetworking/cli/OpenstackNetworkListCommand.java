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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.openstack.networking.domain.NeutronNetwork;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.modelEntityToJson;

/**
 * Lists OpenStack networks.
 */
@Command(scope = "onos", name = "openstack-networks",
        description = "Lists all OpenStack networks")
public class OpenstackNetworkListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-40s%-20s%-20s%-20s%-16s%-8s";

    @Override
    protected void execute() {
        OpenstackNetworkService service = AbstractShellCommand.get(OpenstackNetworkService.class);
        List<Network> networks = Lists.newArrayList(service.networks());
        networks.sort(Comparator.comparing(Network::getName));

        if (outputJson()) {
            try {
                print("%s", mapper().writeValueAsString(json(networks)));
            } catch (JsonProcessingException e) {
                print("Failed to list networks in JSON format");
            }
            return;
        }

        print(FORMAT, "ID", "Name", "Network Mode", "VNI", "Subnets", "HostRoutes");
        for (Network net: networks) {
            List<Subnet> subnets = service.subnets().stream()
                    .filter(subnet -> subnet.getNetworkId().equals(net.getId()))
                    .collect(Collectors.toList());

            List<String> subnetsString = subnets.stream()
                    .map(Subnet::getCidr)
                    .collect(Collectors.toList());

            List<String> hostRoutes = Lists.newArrayList();

            subnets.forEach(subnet -> {
                subnet.getHostRoutes().forEach(h -> hostRoutes.add(h.toString()));
            });

            print(FORMAT, net.getId(),
                    net.getName(),
                    net.getNetworkType().toString(),
                    net.getProviderSegID(),
                    subnets.isEmpty() ? "" : subnetsString,
                    hostRoutes.isEmpty() ? "" : hostRoutes);
        }
    }

    private JsonNode json(List<Network> networks) {
        ArrayNode result = mapper().enable(INDENT_OUTPUT).createArrayNode();
        for (Network net: networks) {
            result.add(modelEntityToJson(net, NeutronNetwork.class));
        }
        return result;
    }
}
