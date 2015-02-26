package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.resource.LabelResourceId;
import org.onosproject.net.resource.LabelResourceService;

/**
 * create label resource pool by specific device id.
 */
@Command(scope = "onos", name = "create-global-label-resource-pool",
description = "create global label resource pool by specific device id")
public class CreateGlobalLabelResourcePoolCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "beginLabel",
            description = "beginning Number",
            required = true, multiValued = false)
    String beginLabel = null;
    @Argument(index = 1, name = "endLabel",
            description = "end Number",
            required = true, multiValued = false)
    String endLabel = null;

    @Override
    protected void execute() {
        // TODO Auto-generated method stub
        LabelResourceService lrs = get(LabelResourceService.class);
        lrs.createGlobalPool(LabelResourceId.labelResourceId(Long
                .parseLong(beginLabel)), LabelResourceId.labelResourceId(Long
                .parseLong(endLabel)));
    }

}
