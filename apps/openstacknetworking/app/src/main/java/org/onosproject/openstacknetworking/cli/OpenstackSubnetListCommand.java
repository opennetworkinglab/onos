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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.openstack.networking.domain.NeutronSubnet;

import java.util.Comparator;
import java.util.List;

import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.deriveResourceName;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.modelEntityToJson;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.prettyJson;

/**
 * Lists OpenStack subnets.
 */
@Service
@Command(scope = "onos", name = "openstack-subnets",
        description = "Lists all OpenStack subnets")
public class OpenstackSubnetListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-40s%-20s%-20s%-20s%-40s%-20s%-8s";

    @Override
    protected void doExecute() {
        OpenstackNetworkService service = get(OpenstackNetworkService.class);
        List<Subnet> subnets = Lists.newArrayList(service.subnets());
        subnets.sort(Comparator.comparing(Subnet::getId));

        if (outputJson()) {
            print("%s", json(subnets));
        } else {
            print(FORMAT, "ID", "Name", "CIDR", "GatewayIp", "NetworkId",
                                                "NetworkName", "HostRoutes");

            for (Subnet subnet: subnets) {
                Network osNet = service.network(subnet.getNetworkId());
                String netName = osNet == null ? "N/A" : deriveResourceName(osNet);
                print(FORMAT,
                        subnet.getId(),
                        deriveResourceName(subnet),
                        subnet.getCidr(),
                        subnet.getGateway(),
                        subnet.getNetworkId(),
                        netName,
                        subnet.getHostRoutes());
            }
        }
    }

    private String json(List<Subnet> subnets) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Subnet net: subnets) {
            result.add(modelEntityToJson(net, NeutronSubnet.class));
        }
        return prettyJson(mapper, result.toString());
    }
}
