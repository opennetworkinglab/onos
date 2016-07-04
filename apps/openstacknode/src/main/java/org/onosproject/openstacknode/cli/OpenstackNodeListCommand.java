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

import java.util.Collections;
import java.util.List;

/**
 * Lists all nodes registered to the service.
 */
@Command(scope = "onos", name = "openstack-nodes",
        description = "Lists all nodes registered in OpenStack node service")
public class OpenstackNodeListCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        OpenstackNodeService nodeService = AbstractShellCommand.get(OpenstackNodeService.class);
        List<OpenstackNode> nodes = nodeService.nodes();
        Collections.sort(nodes, OpenstackNode.OPENSTACK_NODE_COMPARATOR);

        if (outputJson()) {
            print("%s", json(nodes));
        } else {
            for (OpenstackNode node : nodes) {
                print("hostname=%s, type=%s, managementIp=%s, dataIp=%s, intBridge=%s, routerBridge=%s init=%s",
                        node.hostname(),
                        node.type(),
                        node.managementIp(),
                        node.dataIp(),
                        node.intBridge(),
                        node.routerBridge(),
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
                    .put("managementIp", node.managementIp().toString())
                    .put("dataIp", node.dataIp().toString())
                    .put("intBridge", node.intBridge().toString())
                    .put("routerBridge", node.routerBridge().toString())
                    .put("state", node.state().name()));
        }
        return result;
    }
}