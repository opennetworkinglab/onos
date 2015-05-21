package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;

@Command(scope = "onos", name = "destroy-global-label-resource-pool",
description = "Destroys global label resource pool")
public class DestroyGlobalLabelResourcePoolCommand extends AbstractShellCommand {
    @Override
    protected void execute() {
        LabelResourceAdminService lrs = get(LabelResourceAdminService.class);
        lrs.destroyGlobalPool();
    }

}
