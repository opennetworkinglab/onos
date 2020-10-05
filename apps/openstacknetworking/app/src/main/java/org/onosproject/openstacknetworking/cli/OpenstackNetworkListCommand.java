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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.openstack.networking.domain.NeutronNetwork;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.deriveResourceName;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.modelEntityToJson;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.prettyJson;

/**
 * Lists OpenStack networks.
 */
@Service
@Command(scope = "onos", name = "openstack-networks",
        description = "Lists all OpenStack networks")
public class OpenstackNetworkListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-40s%-20s%-20s%-20s%-16s%-8s";

    @Override
    protected void doExecute() {
        OpenstackNetworkService service = get(OpenstackNetworkService.class);
        List<Network> networks = Lists.newArrayList(service.networks());
        networks.sort(Comparator.comparing(Network::getId));

        if (outputJson()) {
            print("%s", json(networks));
        } else {
            print(FORMAT, "ID", "Name", "Type", "SegId", "Subnets", "HostRoutes");
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
                        deriveResourceName(net),
                        service.networkType(net.getId()).toString(),
                        net.getProviderSegID(),
                        subnets.isEmpty() ? "" : subnetsString,
                        hostRoutes.isEmpty() ? "" : hostRoutes);
            }
        }
    }

    private String json(List<Network> networks) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Network net: networks) {
            result.add(modelEntityToJson(net, NeutronNetwork.class));
        }
        return prettyJson(mapper, result.toString());
    }
}
