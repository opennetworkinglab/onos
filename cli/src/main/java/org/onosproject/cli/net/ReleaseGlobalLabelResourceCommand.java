package org.onosproject.cli.net;

import java.util.HashSet;
import java.util.Set;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.resource.LabelResourceId;
import org.onosproject.net.resource.LabelResourceService;

@Command(scope = "onos", name = "release-global-label-resource-pool",
description = "release global label resource pool by specific device id")
public class ReleaseGlobalLabelResourceCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "releaseLabelIds",
            description = "releaseLabelIds",
            required = true, multiValued = false)
    String releaseLabelIds = null;

    @Override
    protected void execute() {
        // TODO Auto-generated method stub
        LabelResourceService lrs = get(LabelResourceService.class);
        Set<LabelResourceId> release = new HashSet<LabelResourceId>();
        String[] labelIds = releaseLabelIds.split(",");
        LabelResourceId resource = null;
        for (int i = 0; i < labelIds.length; i++) {
            resource = LabelResourceId.labelResourceId(Long.parseLong(labelIds[i]));
            release.add(resource);
        }
        lrs.releaseToGlobalPool(release);
    }

}
