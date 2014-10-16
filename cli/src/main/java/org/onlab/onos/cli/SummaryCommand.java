package org.onlab.onos.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.CoreService;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.link.LinkService;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyService;

/**
 * Provides summary of ONOS model.
 */
@Command(scope = "onos", name = "summary",
         description = "Provides summary of ONOS model")
public class SummaryCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        TopologyService topologyService = get(TopologyService.class);
        Topology topology = topologyService.currentTopology();
        if (outputJson()) {
            print("%s", new ObjectMapper().createObjectNode()
                    .put("node", get(ClusterService.class).getLocalNode().ip().toString())
                    .put("version", get(CoreService.class).version().toString())
                    .put("nodes", get(ClusterService.class).getNodes().size())
                    .put("devices", get(DeviceService.class).getDeviceCount())
                    .put("links", get(LinkService.class).getLinkCount())
                    .put("hosts", get(HostService.class).getHostCount())
                    .put("clusters", topologyService.getClusters(topology).size())
                    .put("paths", topology.pathCount())
                    .put("flows", get(FlowRuleService.class).getFlowRuleCount())
                    .put("intents", get(IntentService.class).getIntentCount()));
        } else {
            print("node=%s, version=%s",
                  get(ClusterService.class).getLocalNode().ip(),
                  get(CoreService.class).version().toString());
            print("nodes=%d, devices=%d, links=%d, hosts=%d, clusters=%s, paths=%d, flows=%d, intents=%d",
                  get(ClusterService.class).getNodes().size(),
                  get(DeviceService.class).getDeviceCount(),
                  get(LinkService.class).getLinkCount(),
                  get(HostService.class).getHostCount(),
                  topologyService.getClusters(topology).size(),
                  topology.pathCount(),
                  get(FlowRuleService.class).getFlowRuleCount(),
                  get(IntentService.class).getIntentCount());
        }
    }

}
