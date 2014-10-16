package org.onlab.onos.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
        if (outputJson()) {
            print("%s", json(service, nodes));
        } else {
            ControllerNode self = service.getLocalNode();
            for (ControllerNode node : nodes) {
                print(FMT, node.id(), node.ip(), node.tcpPort(),
                      service.getState(node.id()),
                      node.equals(self) ? "*" : "");
            }
        }
    }

    // Produces JSON structure.
    private JsonNode json(ClusterService service, List<ControllerNode> nodes) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        ControllerNode self = service.getLocalNode();
        for (ControllerNode node : nodes) {
            result.add(mapper.createObjectNode()
                               .put("id", node.id().toString())
                               .put("ip", node.ip().toString())
                               .put("tcpPort", node.tcpPort())
                               .put("state", service.getState(node.id()).toString())
                               .put("self", node.equals(self)));
        }
        return result;
    }

}
