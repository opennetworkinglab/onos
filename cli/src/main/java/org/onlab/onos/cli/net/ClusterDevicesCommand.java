package org.onlab.onos.cli.net;

import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.Comparators;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.topology.TopologyCluster;

import java.util.Collections;
import java.util.List;

import static org.onlab.onos.net.topology.ClusterId.clusterId;

/**
 * Lists devices of the specified topology cluster in the current topology.
 */
@Command(scope = "onos", name = "cluster-devices",
         description = "Lists devices of the specified topology cluster in the current topology")
public class ClusterDevicesCommand extends ClustersListCommand {

    @Argument(index = 0, name = "id", description = "Cluster ID",
              required = false, multiValued = false)
    String id = null;

    @Override
    protected void execute() {
        int cid = Integer.parseInt(id);
        init();
        TopologyCluster cluster = service.getCluster(topology, clusterId(cid));
        if (cluster == null) {
            error("No such cluster %s", cid);
        } else {
            List<DeviceId> ids = Lists.newArrayList(service.getClusterDevices(topology, cluster));
            Collections.sort(ids, Comparators.ELEMENT_ID_COMPARATOR);
            for (DeviceId deviceId : ids) {
                print("%s", deviceId);
            }
        }
    }


}
