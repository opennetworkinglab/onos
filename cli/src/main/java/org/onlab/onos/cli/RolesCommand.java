package org.onlab.onos.cli;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collections;
import java.util.List;

import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.mastership.MastershipService;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceService;


/**
 * Lists mastership roles of nodes for each device.
 */
@Command(scope = "onos", name = "roles",
        description = "Lists mastership roles of nodes for each device.")
public class RolesCommand extends AbstractShellCommand {

    private static final String FMT_HDR = "%s: master=%s\nstandbys: %s nodes";
    private static final String FMT_SB = "\t%s";

    @Override
    protected void execute() {
        DeviceService deviceService = get(DeviceService.class);
        MastershipService roleService = get(MastershipService.class);

        for (Device d : getSortedDevices(deviceService)) {
            DeviceId did = d.id();
            printRoles(roleService, did);
        }
    }

    /**
     * Returns the list of devices sorted using the device ID URIs.
     *
     * @param service device service
     * @return sorted device list
     */
    protected static List<Device> getSortedDevices(DeviceService service) {
        List<Device> devices = newArrayList(service.getDevices());
        Collections.sort(devices, Comparators.ELEMENT_COMPARATOR);
        return devices;
    }

    /**
     * Prints the role information for a device.
     *
     * @param deviceId the ID of the device
     * @param master the current master
     */
    protected void printRoles(MastershipService service, DeviceId deviceId) {
        List<NodeId> nodes = service.getNodesFor(deviceId);
        NodeId first = null;
        NodeId master = null;

        if (!nodes.isEmpty()) {
            first = nodes.get(0);
        }
        if (first != null &&
                first.equals(service.getMasterFor(deviceId))) {
            master = nodes.get(0);
            nodes.remove(master);
        }
        print(FMT_HDR, deviceId, master == null ? "NONE" : master, nodes.size());

        for (NodeId nid : nodes) {
            print(FMT_SB, nid);
        }
    }
}
