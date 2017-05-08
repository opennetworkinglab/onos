/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.openstacknode.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknode.OpenstackNode;
import org.onosproject.openstacknode.OpenstackNodeService;

import java.util.List;

/**
 * Lists all nodes registered to the service.
 */
@Command(scope = "onos", name = "openstack-nodes",
        description = "Lists all nodes registered in OpenStack node service")
public class OpenstackNodeListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-20s%-15s%-24s%-24s%-20s%-20s%-15s%s";

    @Override
    protected void execute() {
        OpenstackNodeService nodeService = AbstractShellCommand.get(OpenstackNodeService.class);
        List<OpenstackNode> nodes = nodeService.nodes();
        nodes.sort(OpenstackNode.OPENSTACK_NODE_COMPARATOR);

        if (outputJson()) {
            print("%s", json(nodes));
        } else {
            print(FORMAT, "Hostname", "Type", "Integration Bridge", "Router Bridge",
                    "Management IP", "Data IP", "VLAN Intf", "State");
            for (OpenstackNode node : nodes) {
                print(FORMAT,
                        node.hostname(),
                        node.type(),
                        node.intBridge(),
                        node.routerBridge().isPresent() ? node.routerBridge().get() : "",
                        node.managementIp(),
                        node.dataIp().isPresent() ? node.dataIp().get() : "",
                        node.vlanPort().isPresent() ? node.vlanPort().get() : "",
                        node.state());
            }
            print("Total %s nodes", nodeService.nodes().size());
        }
    }

    private JsonNode json(List<OpenstackNode> nodes) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (OpenstackNode node : nodes) {
            result.add(mapper.createObjectNode()
                    .put("hostname", node.hostname())
                    .put("type", node.type().name())
                    .put("intBridge", node.intBridge().toString())
                    .put("routerBridge", node.routerBridge().toString())
                    .put("managementIp", node.managementIp().toString())
                    .put("dataIp", node.dataIp().toString())
                    .put("vlanPort", node.vlanPort().toString())
                    .put("state", node.state().name()));
        }
        return result;
    }
}