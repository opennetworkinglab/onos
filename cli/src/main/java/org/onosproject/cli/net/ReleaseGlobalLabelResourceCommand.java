package org.onosproject.cli.net;

import java.util.HashSet;
import java.util.Set;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.resource.DefaultLabelResource;
import org.onosproject.net.resource.LabelResourceId;
import org.onosproject.net.resource.LabelResourceService;

@Command(scope = "onos", name = "release-global-label-resource-pool",
description = "release global label resource pool by specific device id")
public class ReleaseGlobalLabelResourceCommand extends AbstractShellCommand {
    @Argument(index = 1, name = "releaseLabelIds",
            description = "releaseLabelIds",
            required = true, multiValued = false)
    String releaseLabelIds = null;

    private static final String GLOBAL_RESOURCE_POOL_DEVICE_ID = "global_resource_pool_device_id";

    @Override
    protected void execute() {
        // TODO Auto-generated method stub
        LabelResourceService lrs = get(LabelResourceService.class);
        Set<DefaultLabelResource> release = new HashSet<DefaultLabelResource>();
        String[] labelIds = releaseLabelIds.split(",");
        DefaultLabelResource resource = null;
        for (int i = 0; i < labelIds.length; i++) {
            resource = new DefaultLabelResource(
                                                DeviceId.deviceId(GLOBAL_RESOURCE_POOL_DEVICE_ID),
                                                LabelResourceId.labelResourceId(Long
                                                        .parseLong(labelIds[i])));
            release.add(resource);
        }
        lrs.releaseToGlobalPool(release);
    }

}
