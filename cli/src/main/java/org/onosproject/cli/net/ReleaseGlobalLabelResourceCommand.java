package org.onosproject.cli.net;

import java.util.HashSet;
import java.util.Set;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceService;

@Command(scope = "onos", name = "release-global-label-resource-pool",
description = "Releases labels to global label resource pool.")
public class ReleaseGlobalLabelResourceCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "releaseLabelIds",
            description = "Represents for the label ids that are released. They are splited by dot symbol",
            required = true, multiValued = false)
    String releaseLabelIds = null;

    @Override
    protected void execute() {
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
