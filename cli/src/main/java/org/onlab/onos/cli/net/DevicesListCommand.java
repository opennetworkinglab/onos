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
            "id=%s, available=%s, type=%s, mfr=%s, hw=%s, sw=%s, serial=%s";

    @Override
    protected Object doExecute() throws Exception {
        DeviceService service = getService(DeviceService.class);
        for (Device device : service.getDevices()) {
            printDevice(device, service.isAvailable(device.id()));
        }
        return null;
    }

    /**
     * Prints information about the specified device.
     *
     * @param device      infrastructure device
     * @param isAvailable true of device is available
     */
    protected void printDevice(Device device, boolean isAvailable) {
        print(FMT, device.id(), isAvailable, device.type(),
              device.manufacturer(), device.hwVersion(), device.swVersion(),
              device.serialNumber());
    }

}
