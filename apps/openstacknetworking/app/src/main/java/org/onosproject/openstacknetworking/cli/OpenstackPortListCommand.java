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
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.openstack.networking.domain.NeutronPort;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.deriveResourceName;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.modelEntityToJson;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.prettyJson;

/**
 * Lists OpenStack ports.
 */
@Service
@Command(scope = "onos", name = "openstack-ports",
        description = "Lists all OpenStack ports")
public class OpenstackPortListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-40s%-20s%-20s%-8s";

    @Argument(name = "networkId", description = "Network ID")
    private String networkId = null;

    @Override
    protected void doExecute() {
        OpenstackNetworkService service = get(OpenstackNetworkService.class);

        List<Port> ports = Lists.newArrayList(service.ports());
        ports.sort(Comparator.comparing(Port::getNetworkId));

        if (!Strings.isNullOrEmpty(networkId)) {
            ports.removeIf(port -> !port.getNetworkId().equals(networkId));
        }

        if (outputJson()) {
            print("%s", json(ports));

        } else {
            print(FORMAT, "ID", "Network", "MAC", "Fixed IPs");
            for (Port port: ports) {
                List<String> fixedIps = port.getFixedIps().stream()
                        .map(IP::getIpAddress)
                        .collect(Collectors.toList());
                Network osNet = service.network(port.getNetworkId());
                String netName = osNet == null ? "N/A" : deriveResourceName(osNet);
                print(FORMAT, port.getId(),
                        netName,
                        port.getMacAddress(),
                        fixedIps.isEmpty() ? "" : fixedIps);
            }
        }
    }

    private String json(List<Port> ports) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Port port: ports) {
            result.add(modelEntityToJson(port, NeutronPort.class));
        }
        return prettyJson(mapper, result.toString());
    }
}
