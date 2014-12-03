/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Lists all controller cluster nodes.
 */
@Command(scope = "onos", name = "nodes",
         description = "Lists all controller cluster nodes")
public class NodesListCommand extends AbstractShellCommand {

    private static final String FMT =
            "id=%s, address=%s:%s, state=%s %s";

    @Override
    protected void execute() {
        ClusterService service = get(ClusterService.class);
        List<ControllerNode> nodes = newArrayList(service.getNodes());
        Collections.sort(nodes, Comparators.NODE_COMPARATOR);
        if (outputJson()) {
            print("%s", json(service, nodes));
        } else {
            ControllerNode self = service.getLocalNode();
            for (ControllerNode node : nodes) {
                print(FMT, node.id(), node.ip(), node.tcpPort(),
                      service.getState(node.id()),
                      node.equals(self) ? "*" : "");
            }
        }
    }

    // Produces JSON structure.
    private JsonNode json(ClusterService service, List<ControllerNode> nodes) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        ControllerNode self = service.getLocalNode();
        for (ControllerNode node : nodes) {
            result.add(mapper.createObjectNode()
                               .put("id", node.id().toString())
                               .put("ip", node.ip().toString())
                               .put("tcpPort", node.tcpPort())
                               .put("state", service.getState(node.id()).toString())
                               .put("self", node.equals(self)));
        }
        return result;
    }

}
