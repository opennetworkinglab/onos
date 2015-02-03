package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.resource.LabelResourceService;

/**
 * create label resource pool by specific device id.
 */
@Command(scope = "onos", name = "create-label-resource-pool",
           description = "create label resource pool by specific device id")
public class CreateLabelResourcePoolCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Id of device", required = true, multiValued = false)
    String deviceId = null;
    @Argument(index = 1, name = "beginLabel",
            description = "beginning Number", required = true, multiValued = false)
    String beginLabel = null;
    @Argument(index = 2, name = "endLabel",
            description = "end Number", required = true, multiValued = false)
    String endLabel = null;

    @Override
    protected void execute() {
        // TODO Auto-generated method stub
        LabelResourceService lrs = get(LabelResourceService.class);
        lrs.create(DeviceId.deviceId(deviceId), Long.parseLong(beginLabel), Long.parseLong(endLabel));
    }

}
