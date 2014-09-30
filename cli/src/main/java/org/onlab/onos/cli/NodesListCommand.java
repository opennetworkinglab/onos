package org.onlab.onos.cli;

import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Lists all controller cluster nodes.
 */
@Command(scope = "onos", name = "nodes",
         description = "Lists all controller cluster nodes")
public class NodesListCommand extends AbstractShellCommand {

    private static final String FMT =
            "id=%s, address=%s:%s, state=%s %s";

    @Override
    protected void execute() {
        ClusterService service = get(ClusterService.class);
        List<ControllerNode> nodes = newArrayList(service.getNodes());
        Collections.sort(nodes, Comparators.NODE_COMPARATOR);
        ControllerNode self = service.getLocalNode();
        for (ControllerNode node : nodes) {
            print(FMT, node.id(), node.ip(), node.tcpPort(),
                  service.getState(node.id()),
                  node.equals(self) ? "*" : "");
        }
    }

}
