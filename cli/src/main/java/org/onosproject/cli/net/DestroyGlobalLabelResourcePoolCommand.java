package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.resource.LabelResourceService;
@Command(scope = "onos", name = "destroy-global-label-resource-pool",
      description = "destroy global label resource pool by specific device id")
public class DestroyGlobalLabelResourcePoolCommand extends AbstractShellCommand {
    @Override
    protected void execute() {
        // TODO Auto-generated method stub
        LabelResourceService lrs = get(LabelResourceService.class);
        lrs.destroyGlobalPool();
    }

}
