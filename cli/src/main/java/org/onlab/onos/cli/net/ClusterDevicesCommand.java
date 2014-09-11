package org.onlab.onos.cli.net;

import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.topology.TopologyCluster;

import java.util.Collections;
import java.util.Comparator;
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

    protected static final Comparator<DeviceId> ID_COMPARATOR = new Comparator<DeviceId>() {
        @Override
        public int compare(DeviceId id1, DeviceId id2) {
            return id1.uri().toString().compareTo(id2.uri().toString());
        }
    };

    @Override
    protected Object doExecute() throws Exception {
        int cid = Integer.parseInt(id);
        init();
        TopologyCluster cluster = service.getCluster(topology, clusterId(cid));
        List<DeviceId> ids = Lists.newArrayList(service.getClusterDevices(topology, cluster));
        Collections.sort(ids, ID_COMPARATOR);
        for (DeviceId deviceId : ids) {
            print("%s", deviceId);
        }
        return null;
    }


}
