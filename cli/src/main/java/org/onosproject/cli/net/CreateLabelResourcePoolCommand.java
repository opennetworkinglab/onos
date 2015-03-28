package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.resource.LabelResourceAdminService;
import org.onosproject.net.resource.LabelResourceId;

/**
 * create label resource pool by specific device id.
 */
@Command(scope = "onos", name = "create-label-resource-pool",
     description = "Creates label resource pool by a specific device id")
public class CreateLabelResourcePoolCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId", description = "Device identity", required = true, multiValued = false)
    String deviceId = null;
    @Argument(index = 1, name = "beginLabel",
            description = "The first label of global label resource pool.", required = true, multiValued = false)
    String beginLabel = null;
    @Argument(index = 2, name = "endLabel",
            description = "The last label of global label resource pool.", required = true, multiValued = false)
    String endLabel = null;

    @Override
    protected void execute() {
        LabelResourceAdminService lrs = get(LabelResourceAdminService.class);
        lrs.createDevicePool(DeviceId.deviceId(deviceId), LabelResourceId
                .labelResourceId(Long.parseLong(beginLabel)), LabelResourceId
                .labelResourceId(Long.parseLong(endLabel)));
    }

}
