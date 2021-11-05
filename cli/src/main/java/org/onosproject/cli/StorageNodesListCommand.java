/*
 * Copyright 2014-present Open Networking Foundation
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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterAdminService;
import org.onosproject.cluster.Node;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Lists all storage nodes.
 */
@Service
@Command(scope = "onos", name = "storage-nodes", description = "Lists all storage nodes")
public class StorageNodesListCommand extends AbstractShellCommand {

    private static final String FMT = "id=%s, address=%s:%s";

    @Override
    protected void doExecute() {
        ClusterAdminService service = get(ClusterAdminService.class);
        List<Node> nodes = newArrayList(service.getConsensusNodes());
        Collections.sort(nodes, Comparator.comparing(Node::id));
        if (outputJson()) {
            print("%s", json(nodes));
        } else {
            for (Node node : nodes) {
                print(FMT, node.id(), node.host(), node.tcpPort());
            }
        }
    }

    // Produces JSON structure.
    private JsonNode json(List<Node> nodes) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Node node : nodes) {
            IpAddress nodeIp = node.ip();
            ObjectNode newNode = mapper.createObjectNode()
                    .put("id", node.id().toString())
                    .put("ip", nodeIp != null ? nodeIp.toString() : node.host())
                    .put("host", node.host())
                    .put("tcpPort", node.tcpPort());
            result.add(newNode);
        }
        return result;
    }

}
