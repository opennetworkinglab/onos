package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.device.DeviceService;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Lists all infrastructure devices.
 */
@Command(scope = "onos", name = "devices",
         description = "Lists all infrastructure devices")
public class DevicesListCommand extends AbstractShellCommand {

    private static final String FMT =
            "id=%s, available=%s, role=%s, type=%s, mfr=%s, hw=%s, sw=%s, serial=%s";

    protected static final Comparator<Device> ID_COMPARATOR = new Comparator<Device>() {
        @Override
        public int compare(Device d1, Device d2) {
            return d1.id().uri().toString().compareTo(d2.id().uri().toString());
        }
    };

    @Override
    protected Object doExecute() throws Exception {
        DeviceService service = getService(DeviceService.class);
        for (Device device : getSortedDevices(service)) {
            printDevice(service, device);
        }
        return null;
    }

    /**
     * Returns the list of devices sorted using the device ID URIs.
     *
     * @param service device service
     * @return sorted device list
     */
    protected List<Device> getSortedDevices(DeviceService service) {
        List<Device> devices = newArrayList(service.getDevices());
        Collections.sort(devices, ID_COMPARATOR);
        return devices;
    }

    /**
     * Prints information about the specified device.
     *
     * @param service device service
     * @param device  infrastructure device
     */
    protected void printDevice(DeviceService service, Device device) {
        print(FMT, device.id(), service.isAvailable(device.id()),
              service.getRole(device.id()), device.type(),
              device.manufacturer(), device.hwVersion(), device.swVersion(),
              device.serialNumber());
    }

}
