package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.device.DeviceService;

import static org.onlab.onos.net.DeviceId.deviceId;

/**
 * Lists all infrastructure links.
 */
@Command(scope = "onos", name = "ports",
         description = "Lists all ports of a device")
public class DevicePortsListCommand extends AbstractShellCommand {

    private static final String FMT = "port=%s, state=%s";

    @Argument(index = 0, name = "deviceId", description = "Device ID",
              required = true, multiValued = false)
    String deviceId = null;

    @Override
    protected Object doExecute() throws Exception {
        DeviceService service = getService(DeviceService.class);
        Iterable<Port> ports = service.getPorts(deviceId(deviceId));
        for (Port port : ports) {
            print(FMT, port.number(), port.isEnabled() ? "enabled" : "disabled");
        }
        return null;
    }
}
