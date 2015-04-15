/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.core.CoreService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;

/**
 * Provides summary of ONOS model.
 */
@Command(scope = "onos", name = "summary",
         description = "Provides summary of ONOS model")
public class SummaryCommand extends AbstractShellCommand {

    /**
     * Count the active ONOS controller nodes.
     *
     * @param nodes set of all of the controller nodes in the cluster
     * @return count of active nodes
     */
    private int activeNodes(Set<ControllerNode> nodes) {
        int nodeCount = 0;

        for (final ControllerNode node : nodes) {
            final ControllerNode.State nodeState =
                    get(ClusterService.class).getState(node.id());
            if (nodeState == ControllerNode.State.ACTIVE) {
                nodeCount++;
            }
        }
        return nodeCount;
    }

    @Override
    protected void execute() {
        TopologyService topologyService = get(TopologyService.class);
        Topology topology = topologyService.currentTopology();
        if (outputJson()) {
            print("%s", new ObjectMapper().createObjectNode()
                    .put("node", get(ClusterService.class).getLocalNode().ip().toString())
                    .put("version", get(CoreService.class).version().toString())
                    .put("nodes", get(ClusterService.class).getNodes().size())
                    .put("devices", topology.deviceCount())
                    .put("links", topology.linkCount())
                    .put("hosts", get(HostService.class).getHostCount())
                    .put("SCC(s)", topology.clusterCount())
                    .put("flows", get(FlowRuleService.class).getFlowRuleCount())
                    .put("intents", get(IntentService.class).getIntentCount()));
        } else {
            print("node=%s, version=%s",
                  get(ClusterService.class).getLocalNode().ip(),
                  get(CoreService.class).version().toString());
            print("nodes=%d, devices=%d, links=%d, hosts=%d, SCC(s)=%s, flows=%d, intents=%d",
                  activeNodes(get(ClusterService.class).getNodes()),
                  get(DeviceService.class).getDeviceCount(),
                  get(LinkService.class).getLinkCount(),
                  get(HostService.class).getHostCount(),
                  topologyService.getClusters(topology).size(),
                  get(FlowRuleService.class).getFlowRuleCount(),
                  get(IntentService.class).getIntentCount());
        }
    }

}
