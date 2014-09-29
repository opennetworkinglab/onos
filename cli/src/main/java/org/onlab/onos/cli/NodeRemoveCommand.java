package org.onlab.onos.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cluster.ClusterAdminService;
import org.onlab.onos.cluster.NodeId;

/**
 * Removes a controller cluster node.
 */
@Command(scope = "onos", name = "remove-node",
         description = "Removes a new controller cluster node")
public class NodeRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "nodeId", description = "Node ID",
              required = true, multiValued = false)
    String nodeId = null;

    @Override
    protected void execute() {
        ClusterAdminService service = get(ClusterAdminService.class);
        service.removeNode(new NodeId(nodeId));
    }

}
