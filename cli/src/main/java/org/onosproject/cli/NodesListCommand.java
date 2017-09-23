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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Command;
import org.joda.time.DateTime;
import org.onlab.util.Tools;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.MembershipAdminService;
import org.onosproject.core.Version;
import org.onosproject.utils.Comparators;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;


/**
 * Lists all controller cluster nodes.
 */
@Command(scope = "onos", name = "nodes",
        description = "Lists all controller cluster nodes")
public class NodesListCommand extends AbstractShellCommand {

    private static final String FMT = "id=%s, address=%s:%s, state=%s, version=%s, updated=%s %s";

    @Override
    protected void execute() {
        MembershipAdminService service = get(MembershipAdminService.class);
        List<ControllerNode> nodes = newArrayList(service.getNodes());
        Collections.sort(nodes, Comparators.NODE_COMPARATOR);
        if (outputJson()) {
            print("%s", json(service, nodes));
        } else {
            ControllerNode self = service.getLocalNode();
            for (ControllerNode node : nodes) {
                DateTime lastUpdated = service.getLastUpdated(node.id());
                String timeAgo = "Never";
                if (lastUpdated != null) {
                    timeAgo = Tools.timeAgo(lastUpdated.getMillis());
                }
                Version version = service.getVersion(node.id());
                print(FMT, node.id(), node.ip(), node.tcpPort(),
                        service.getState(node.id()),
                        version == null ? "unknown" : version,
                        timeAgo,
                        node.equals(self) ? "*" : "");
            }
        }
    }

    // Produces JSON structure.
    private JsonNode json(MembershipAdminService service, List<ControllerNode> nodes) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        ControllerNode self = service.getLocalNode();
        for (ControllerNode node : nodes) {
            ControllerNode.State nodeState = service.getState(node.id());
            Version nodeVersion = service.getVersion(node.id());
            ObjectNode newNode = mapper.createObjectNode()
                    .put("id", node.id().toString())
                    .put("ip", node.ip().toString())
                    .put("tcpPort", node.tcpPort())
                    .put("self", node.equals(self));
            if (nodeState != null) {
                newNode.put("state", nodeState.toString());
            }
            if (nodeVersion != null) {
                newNode.put("version", nodeVersion.toString());
            }
            result.add(newNode);
        }
        return result;
    }

}
