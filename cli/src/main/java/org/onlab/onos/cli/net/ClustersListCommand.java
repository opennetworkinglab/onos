package org.onlab.onos.cli.net;

import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.net.topology.TopologyCluster;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Lists all clusters in the current topology.
 */
@Command(scope = "onos", name = "clusters",
         description = "Lists all clusters in the current topology")
public class ClustersListCommand extends TopologyCommand {

    private static final String FMT =
            "id=%s, devices=%d, links=%d";

    protected static final Comparator<TopologyCluster> ID_COMPARATOR =
            new Comparator<TopologyCluster>() {
                @Override
                public int compare(TopologyCluster c1, TopologyCluster c2) {
                    return c1.id().index() - c2.id().index();
                }
            };

    @Override
    protected Object doExecute() throws Exception {
        init();
        List<TopologyCluster> clusters = Lists.newArrayList(service.getClusters(topology));
        Collections.sort(clusters, ID_COMPARATOR);

        for (TopologyCluster cluster : clusters) {
            print(FMT, cluster.id(), cluster.deviceCount(), cluster.linkCount());
        }
        return null;
    }

}
