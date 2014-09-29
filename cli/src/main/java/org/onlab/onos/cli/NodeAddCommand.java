package org.onlab.onos.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cluster.ClusterAdminService;
import org.onlab.onos.cluster.NodeId;
import org.onlab.packet.IpPrefix;

/**
 * Lists all controller cluster nodes.
 */
@Command(scope = "onos", name = "add-node",
         description = "Lists all controller cluster nodes")
public class NodeAddCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "nodeId", description = "Node ID",
              required = true, multiValued = false)
    String nodeId = null;

    @Argument(index = 1, name = "ip", description = "Node IP address",
              required = true, multiValued = false)
    String ip = null;

    @Argument(index = 2, name = "tcpPort", description = "TCP port",
              required = false, multiValued = false)
    int tcpPort = 9876;

    @Override
    protected void execute() {
        ClusterAdminService service = get(ClusterAdminService.class);
        service.addNode(new NodeId(nodeId), IpPrefix.valueOf(ip), tcpPort);
    }

}
