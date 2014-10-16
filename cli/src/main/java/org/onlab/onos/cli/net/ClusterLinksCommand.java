package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.topology.TopologyCluster;

import static org.onlab.onos.cli.net.LinksListCommand.json;
import static org.onlab.onos.cli.net.LinksListCommand.linkString;
import static org.onlab.onos.net.topology.ClusterId.clusterId;

/**
 * Lists links of the specified topology cluster in the current topology.
 */
@Command(scope = "onos", name = "cluster-links",
         description = "Lists links of the specified topology cluster in the current topology")
public class ClusterLinksCommand extends ClustersListCommand {

    @Argument(index = 0, name = "id", description = "Cluster ID",
              required = true, multiValued = false)
    String id = null;

    @Override
    protected void execute() {
        int cid = Integer.parseInt(id);
        init();
        TopologyCluster cluster = service.getCluster(topology, clusterId(cid));
        if (cluster == null) {
            error("No such cluster %s", cid);
        } else if (outputJson()) {
            print("%s", json(service.getClusterLinks(topology, cluster)));
        } else {
            for (Link link : service.getClusterLinks(topology, cluster)) {
                print(linkString(link));
            }
        }
    }

}
