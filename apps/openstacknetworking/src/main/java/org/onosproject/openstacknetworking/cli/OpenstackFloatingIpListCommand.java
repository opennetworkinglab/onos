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
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;

import java.util.Comparator;
import java.util.List;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

/**
 * Lists OpenStack floating IP addresses.
 */
@Command(scope = "onos", name = "openstack-floatingips",
        description = "Lists all OpenStack floating IP addresses")
public class OpenstackFloatingIpListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-40s%-20s%-20s";

    @Override
    protected void execute() {
        OpenstackRouterService service = AbstractShellCommand.get(OpenstackRouterService.class);
        List<NetFloatingIP> floatingIps = Lists.newArrayList(service.floatingIps());
        floatingIps.sort(Comparator.comparing(NetFloatingIP::getFloatingIpAddress));

        if (outputJson()) {
            try {
                print("%s", mapper().writeValueAsString(json(floatingIps)));
            } catch (JsonProcessingException e) {
                print("Failed to list networks in JSON format");
            }
            return;
        }

        print(FORMAT, "ID", "Floating IP", "Fixed IP");
        for (NetFloatingIP floatingIp: floatingIps) {
            print(FORMAT, floatingIp.getId(),
                    floatingIp.getFloatingIpAddress(),
                    Strings.isNullOrEmpty(floatingIp.getFixedIpAddress()) ?
            "" : floatingIp.getFixedIpAddress());
        }
    }

    private JsonNode json(List<NetFloatingIP> floatingIps) {
        ArrayNode result = mapper().enable(INDENT_OUTPUT).createArrayNode();
        for (NetFloatingIP floatingIp: floatingIps) {
            result.add(writeFloatingIp(floatingIp));
        }
        return result;
    }

    private ObjectNode writeFloatingIp(NetFloatingIP floatingIp) {
        try {
            String strFloatingIp = ObjectMapperSingleton.getContext(NeutronFloatingIP.class)
                    .writerFor(NeutronFloatingIP.class)
                    .writeValueAsString(floatingIp);
            log.trace(strFloatingIp);
            return (ObjectNode) mapper().readTree(strFloatingIp.getBytes());
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }
}
