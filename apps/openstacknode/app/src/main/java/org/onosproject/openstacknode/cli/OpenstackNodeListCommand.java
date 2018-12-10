/*
 * Copyright 2016-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;

import java.util.Comparator;
import java.util.List;

import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;
import static org.onosproject.openstacknode.util.OpenstackNodeUtil.getGwByComputeNode;
import static org.onosproject.openstacknode.util.OpenstackNodeUtil.prettyJson;

/**
 * Lists all nodes registered to the service.
 */
@Service
@Command(scope = "onos", name = "openstack-nodes",
        description = "Lists all nodes registered in OpenStack node service")
public class OpenstackNodeListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-20s%-15s%-24s%-24s%-20s%-20s%-15s%-15s%-15s";

    @Override
    protected void doExecute() {
        OpenstackNodeService osNodeService = get(OpenstackNodeService.class);
        List<OpenstackNode> osNodes = Lists.newArrayList(osNodeService.nodes());
        osNodes.sort(Comparator.comparing(OpenstackNode::hostname));

        if (outputJson()) {
            print("%s", json(osNodes));
        } else {
            print(FORMAT, "Hostname", "Type", "Integration Bridge", "Management IP",
                    "Data IP", "VLAN Intf", "Uplink Port", "State", "SelectedGw");
            for (OpenstackNode osNode : osNodes) {
                print(FORMAT,
                        osNode.hostname(),
                        osNode.type(),
                        osNode.intgBridge(),
                        osNode.managementIp(),
                        osNode.dataIp() != null ? osNode.dataIp() : "",
                        osNode.vlanIntf() != null ? osNode.vlanIntf() : "",
                        osNode.uplinkPort() != null ? osNode.uplinkPort() : "",
                        osNode.state(),
                        getGwByComputeNode(osNodeService.completeNodes(GATEWAY), osNode));
            }
            print("Total %s nodes", osNodeService.nodes().size());
        }
    }

    private String json(List<OpenstackNode> osNodes) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (OpenstackNode osNode : osNodes) {
            result.add(jsonForEntity(osNode, OpenstackNode.class));
        }
        return prettyJson(mapper, result.toString());
    }
}