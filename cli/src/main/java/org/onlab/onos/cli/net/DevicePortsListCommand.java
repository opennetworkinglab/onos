package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.device.DeviceService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.onlab.onos.net.DeviceId.deviceId;

/**
 * Lists all infrastructure links.
 */
@Command(scope = "onos", name = "ports",
         description = "Lists all ports of a device")
public class DevicePortsListCommand extends DevicesListCommand {

    private static final String FMT = "  port=%s, state=%s";

    @Argument(index = 0, name = "deviceId", description = "Device ID",
              required = false, multiValued = false)
    String uri = null;

    private static final Comparator<Port> PORT_COMPARATOR = new Comparator<Port>() {
        @Override
        public int compare(Port p1, Port p2) {
            long delta = p1.number().toLong() - p2.number().toLong();
            return delta == 0 ? 0 : (delta < 0 ? -1 : +1);
        }
    };

    @Override
    protected Object doExecute() throws Exception {
        DeviceService service = getService(DeviceService.class);
        if (uri == null) {
            for (Device device : getSortedDevices(service)) {
                printDevicePorts(service, device);
            }
        } else {
            printDevicePorts(service, service.getDevice(deviceId(uri)));
        }
        return null;
    }

    private void printDevicePorts(DeviceService service, Device device) {
        List<Port> ports = new ArrayList<>(service.getPorts(device.id()));
        Collections.sort(ports, PORT_COMPARATOR);
        printDevice(device, service.isAvailable(device.id()));
        for (Port port : ports) {
            print(FMT, port.number(), port.isEnabled() ? "enabled" : "disabled");
        }
    }

}
