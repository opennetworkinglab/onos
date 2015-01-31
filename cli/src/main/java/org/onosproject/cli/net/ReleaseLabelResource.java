package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
@Command(scope = "onos", name = "release-label-resource-pool",
        description = "release label resource pool by specific device id")
public class ReleaseLabelResource extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId", description = "Id of device", required = true, multiValued = false)
    String deviceId = null;
    @Argument(index = 1, name = "applyNum", description = "applyNum", required = true, multiValued = false)
    String applyNum = null;
    @Override
    protected void execute() {
        // TODO Auto-generated method stub

    }

}
