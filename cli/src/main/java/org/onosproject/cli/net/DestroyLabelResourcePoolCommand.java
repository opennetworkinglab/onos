package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.resource.LabelResourceService;
@Command(scope = "onos", name = "destroy-label-resource-pool",
      description = "destroy label resource pool by specific device id")
public class DestroyLabelResourcePoolCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Id of device", required = true, multiValued = false)
    String deviceId = null;
    @Override
    protected void execute() {
        // TODO Auto-generated method stub
        LabelResourceService lrs = get(LabelResourceService.class);
        lrs.destroyDevicePool(DeviceId.deviceId(deviceId));
    }

}
