package org.onlab.onos.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.mastership.MastershipService;
import org.onlab.onos.net.DeviceId;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Lists device mastership information.
 */
@Command(scope = "onos", name = "masters",
         description = "Lists device mastership information")
public class MastersListCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        ClusterService service = get(ClusterService.class);
        MastershipService mastershipService = get(MastershipService.class);
        List<ControllerNode> nodes = newArrayList(service.getNodes());
        Collections.sort(nodes, Comparators.NODE_COMPARATOR);

        if (outputJson()) {
            print("%s", json(service, mastershipService, nodes));
        } else {
            for (ControllerNode node : nodes) {
                List<DeviceId> ids = Lists.newArrayList(mastershipService.getDevicesOf(node.id()));
                Collections.sort(ids, Comparators.ELEMENT_ID_COMPARATOR);
                print("%s: %d devices", node.id(), ids.size());
                for (DeviceId deviceId : ids) {
                    print("  %s", deviceId);
                }
            }
        }
    }

    // Produces JSON structure.
    private JsonNode json(ClusterService service, MastershipService mastershipService,
                          List<ControllerNode> nodes) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        ControllerNode self = service.getLocalNode();
        for (ControllerNode node : nodes) {
            List<DeviceId> ids = Lists.newArrayList(mastershipService.getDevicesOf(node.id()));
            result.add(mapper.createObjectNode()
                               .put("id", node.id().toString())
                               .put("size", ids.size())
                               .set("devices", json(mapper, ids)));
        }
        return result;
    }

    /**
     * Produces a JSON array containing the specified device identifiers.
     *
     * @param mapper object mapper
     * @param ids    collection of device identifiers
     * @return JSON array
     */
    public static JsonNode json(ObjectMapper mapper, Iterable<DeviceId> ids) {
        ArrayNode result = mapper.createArrayNode();
        for (DeviceId deviceId : ids) {
            result.add(deviceId.toString());
        }
        return result;
    }

}
