/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.cordvtn.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cordvtn.CordVtnNodeManager;
import org.onosproject.cordvtn.CordVtnNode;

import java.util.Collections;
import java.util.List;

/**
 * Lists all nodes registered to the service.
 */
@Command(scope = "onos", name = "cordvtn-nodes",
        description = "Lists all nodes registered in CORD VTN service")
public class CordVtnNodeListCommand extends AbstractShellCommand {

    private static final String COMPLETE = "COMPLETE";
    private static final String INCOMPLETE = "INCOMPLETE";

    @Override
    protected void execute() {
        CordVtnNodeManager nodeManager = AbstractShellCommand.get(CordVtnNodeManager.class);
        List<CordVtnNode> nodes = nodeManager.getNodes();
        Collections.sort(nodes, CordVtnNode.CORDVTN_NODE_COMPARATOR);

        if (outputJson()) {
            print("%s", json(nodeManager, nodes));
        } else {
            for (CordVtnNode node : nodes) {
                print("hostname=%s, hostMgmtIp=%s, dpIp=%s, br-int=%s, dpIntf=%s, init=%s",
                      node.hostname(),
                      node.hostMgmtIp().cidr(),
                      node.dpIp().cidr(),
                      node.intBrId().toString(),
                      node.dpIntf(),
                      getState(nodeManager, node));
            }
            print("Total %s nodes", nodeManager.getNodeCount());
        }
    }

    private JsonNode json(CordVtnNodeManager nodeManager, List<CordVtnNode> nodes) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (CordVtnNode node : nodes) {
            result.add(mapper.createObjectNode()
                               .put("hostname", node.hostname())
                               .put("hostManagementIp", node.hostMgmtIp().cidr())
                               .put("dataPlaneIp", node.dpIp().cidr())
                               .put("bridgeId", node.intBrId().toString())
                               .put("dataPlaneInterface", node.dpIntf())
                               .put("init", getState(nodeManager, node)));
        }
        return result;
    }

    private String getState(CordVtnNodeManager nodeManager, CordVtnNode node) {
        return nodeManager.isNodeInitComplete(node) ? COMPLETE : INCOMPLETE;
    }
}
