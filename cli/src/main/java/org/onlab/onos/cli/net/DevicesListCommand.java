package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.device.DeviceService;

/**
 * Lists all infrastructure devices.
 */
@Command(scope = "onos", name = "devices",
         description = "Lists all infrastructure devices")
public class DevicesListCommand extends AbstractShellCommand {

    private static final String FMT =
            "id=%s, type=%s, mfr=%s, hw=%s, sw=%s, serial=%s";

    @Override
    protected Object doExecute() throws Exception {
        for (Device device : getService(DeviceService.class).getDevices()) {
            print(FMT, device.id().uri(), device.type(), device.manufacturer(),
                  device.hwVersion(), device.swVersion(), device.serialNumber());
        }
        return null;
    }
}
