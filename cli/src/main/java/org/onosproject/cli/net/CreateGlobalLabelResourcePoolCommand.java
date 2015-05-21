package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceId;

/**
 * create label resource pool by specific device id.
 */
@Command(scope = "onos", name = "create-global-label-resource-pool",
description = "Creates global label resource pool.")
public class CreateGlobalLabelResourcePoolCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "beginLabel",
            description = "The first label of global label resource pool.",
            required = true, multiValued = false)
    String beginLabel = null;
    @Argument(index = 1, name = "endLabel",
            description = "The last label of global label resource pool.",
            required = true, multiValued = false)
    String endLabel = null;

    @Override
    protected void execute() {
        LabelResourceAdminService lrs = get(LabelResourceAdminService.class);
        lrs.createGlobalPool(LabelResourceId.labelResourceId(Long
                .parseLong(beginLabel)), LabelResourceId.labelResourceId(Long
                .parseLong(endLabel)));
    }

}
