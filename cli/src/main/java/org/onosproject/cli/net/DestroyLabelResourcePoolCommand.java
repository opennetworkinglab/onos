package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.resource.LabelResourceAdminService;

@Command(scope = "onos", name = "destroy-label-resource-pool",
    description = "Destroys label resource pool by a specific device id")
public class DestroyLabelResourcePoolCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId", description = "Device identity", required = true, multiValued = false)
    String deviceId = null;

    @Override
    protected void execute() {
        LabelResourceAdminService lrs = get(LabelResourceAdminService.class);
        lrs.destroyDevicePool(DeviceId.deviceId(deviceId));
    }

}
