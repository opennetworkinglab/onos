package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceAdminService;

/**
 * Removes an infrastructure device.
 */
@Command(scope = "onos", name = "device-remove",
         description = "Removes an infrastructure device")
public class DeviceRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
              required = true, multiValued = false)
    String uri = null;

    @Override
    protected Object doExecute() throws Exception {
        getService(DeviceAdminService.class).removeDevice(DeviceId.deviceId(uri));
        return null;
    }

}
