package org.onlab.onos.cli.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.Comparators;
import org.onlab.onos.net.topology.TopologyCluster;

import java.util.Collections;
import java.util.List;

/**
 * Lists all clusters in the current topology.
 */
@Command(scope = "onos", name = "clusters",
         description = "Lists all clusters in the current topology")
public class ClustersListCommand extends TopologyCommand {

    private static final String FMT =
            "id=%d, devices=%d, links=%d";

    @Override
    protected void execute() {
        init();
        List<TopologyCluster> clusters = Lists.newArrayList(service.getClusters(topology));
        Collections.sort(clusters, Comparators.CLUSTER_COMPARATOR);

        if (outputJson()) {
            print("%s", json(clusters));
        } else {
            for (TopologyCluster cluster : clusters) {
                print(FMT, cluster.id().index(), cluster.deviceCount(), cluster.linkCount());
            }
        }
    }

    // Produces a JSON result.
    private JsonNode json(Iterable<TopologyCluster> clusters) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (TopologyCluster cluster : clusters) {
            result.add(mapper.createObjectNode()
                               .put("id", cluster.id().index())
                               .put("deviceCount", cluster.deviceCount())
                               .put("linkCount", cluster.linkCount()));
        }
        return result;
    }

}
