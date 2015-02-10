package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.resource.DefaultLabelResource;
import org.onosproject.net.resource.LabelResourceId;
import org.onosproject.net.resource.LabelResourceService;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

@Command(scope = "onos", name = "release-label-resource-pool",
description = "release label resource pool by specific device id")
public class ReleaseLabelResourceCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Id of device",
            required = true, multiValued = false)
    String deviceId = null;
    @Argument(index = 1, name = "releaseLabelIds",
            description = "releaseLabelIds",
            required = true, multiValued = false)
    String releaseLabelIds = null;

    @Override
    protected void execute() {
        // TODO Auto-generated method stub
        LabelResourceService lrs = get(LabelResourceService.class);
        Multimap<DeviceId, DefaultLabelResource> map = ArrayListMultimap
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
