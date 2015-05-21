package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.incubator.net.resource.label.DefaultLabelResource;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceService;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

@Command(scope = "onos", name = "release-label-resource-pool",
description = "Releases label ids to label resource pool by a specific device id")
public class ReleaseLabelResourceCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Device identity",
            required = true, multiValued = false)
    String deviceId = null;
    @Argument(index = 1, name = "releaseLabelIds",
            description = "Represents for the label ids that are released. They are splited by dot symbol",
            required = true, multiValued = false)
    String releaseLabelIds = null;

    @Override
    protected void execute() {
        LabelResourceService lrs = get(LabelResourceService.class);
        Multimap<DeviceId, LabelResource> map = ArrayListMultimap
                .create();
        String[] labelIds = releaseLabelIds.split(",");
        DefaultLabelResource resource = null;
        for (int i = 0; i < labelIds.length; i++) {
            resource = new DefaultLabelResource(
                                                DeviceId.deviceId(deviceId),
                                                LabelResourceId.labelResourceId(Long
                                                        .parseLong(labelIds[i])));
            map.put(DeviceId.deviceId(deviceId), resource);
        }
        lrs.releaseToDevicePool(map);
    }

}
