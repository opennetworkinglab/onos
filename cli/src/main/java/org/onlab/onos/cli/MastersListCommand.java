package org.onlab.onos.cli;

import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.net.Comparators;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.MastershipService;
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
        ControllerNode self = service.getLocalNode();
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
