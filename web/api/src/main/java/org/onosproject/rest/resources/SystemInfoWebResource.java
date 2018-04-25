/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.rest.resources;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.Set;

/**
 * Provides high-level system information, version, basic sumaries, memory usage, etc.
 */
@Path("system")
public class SystemInfoWebResource extends AbstractWebResource {

    private long activeNodes(Set<ControllerNode> nodes) {
        ClusterService clusterService = get(ClusterService.class);
        return nodes.stream()
                .map(node -> clusterService.getState(node.id()))
                .filter(ControllerNode.State::isActive)
                .count();
    }

    /**
     * Get high-level system information, version, basic sumaries, memory usage, etc.
     *
     * @return 200 OK with a system summary
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSystemInfo() {
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

        ObjectNode root = mapper().createObjectNode()
                .put("node", nodeIp.toString())
                .put("version", version.toString())
                .put("clusterId", clusterId)
                .put("nodes", numNodes)
                .put("devices", numDevices)
                .put("links", numLinks)
                .put("hosts", numHosts)
                .put("sccs", numScc)
                .put("flows", numFlows)
                .put("intents", numIntents);

        MemoryUsage mem = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        newObject(root, "mem")
                .put("current", mem.getUsed())
                .put("max", mem.getMax())
                .put("committed", mem.getCommitted());

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        newObject(root, "threads")
                .put("live", threads.getThreadCount())
                .put("daemon", threads.getDaemonThreadCount())
                .put("peak", threads.getPeakThreadCount());

        return ok(root).build();
    }

}
