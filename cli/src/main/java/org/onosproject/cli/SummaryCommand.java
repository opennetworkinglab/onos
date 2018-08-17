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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterMetadataService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.core.CoreService;
import org.onosproject.core.Version;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.TopologyService;

import java.util.Set;

/**
 * Provides summary of ONOS model.
 */
@Service
@Command(scope = "onos", name = "summary",
         description = "Provides summary of ONOS model")
public class SummaryCommand extends AbstractShellCommand {

    /**
     * Count the active ONOS controller nodes.
     *
     * @param nodes set of all of the controller nodes in the cluster
     * @return count of active nodes
     */
    private long activeNodes(Set<ControllerNode> nodes) {
        ClusterService clusterService = get(ClusterService.class);

        return nodes.stream()
                .map(node -> clusterService.getState(node.id()))
                .filter(nodeState -> nodeState.isActive())
                .count();
    }

    @Override
    protected void doExecute() {
        IpAddress nodeIp = get(ClusterService.class).getLocalNode().ip();
        Version version = get(CoreService.class).version();
        long numNodes = activeNodes(get(ClusterService.class).getNodes());
        int numDevices = get(DeviceService.class).getDeviceCount();
        int numLinks = get(LinkService.class).getLinkCount();
        int numHosts = get(HostService.class).getHostCount();
        int numScc = get(TopologyService.class).currentTopology().clusterCount();
        int numFlows = get(FlowRuleService.class).getFlowRuleCount();
        long numIntents = get(IntentService.class).getIntentCount();
        String clusterId = get(ClusterMetadataService.class).getClusterMetadata().getName();

        if (outputJson()) {
            print("%s", new ObjectMapper().createObjectNode()
                    .put("node", nodeIp.toString())
                    .put("version", version.toString())
                    .put("clusterId", clusterId)
                    .put("nodes", numNodes)
                    .put("devices", numDevices)
                    .put("links", numLinks)
                    .put("hosts", numHosts)
                    .put("SCC(s)", numScc)
                    .put("flows", numFlows)
                    .put("intents", numIntents));
        } else {
            print("node=%s, version=%s clusterId=%s", nodeIp, version, clusterId);
            print("nodes=%d, devices=%d, links=%d, hosts=%d, SCC(s)=%s, flows=%d, intents=%d",
                  numNodes, numDevices, numLinks, numHosts, numScc, numFlows, numIntents);
        }
    }

}
