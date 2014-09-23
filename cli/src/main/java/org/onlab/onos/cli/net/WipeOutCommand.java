package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.device.DeviceAdminService;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.host.HostAdminService;
import org.onlab.onos.net.host.HostService;

/**
 * Wipes-out the entire network information base, i.e. devices, links, hosts.
 */
@Command(scope = "onos", name = "wipe-out",
         description = "Wipes-out the entire network information base, i.e. devices, links, hosts")
public class WipeOutCommand extends ClustersListCommand {

    @Override
    protected void execute() {
        DeviceAdminService deviceAdminService = get(DeviceAdminService.class);
        DeviceService deviceService = get(DeviceService.class);
        for (Device device : deviceService.getDevices()) {
            deviceAdminService.removeDevice(device.id());
        }

        HostAdminService hostAdminService = get(HostAdminService.class);
        HostService hostService = get(HostService.class);
        for (Host host : hostService.getHosts()) {
            hostAdminService.removeHost(host.id());
        }
    }


}
