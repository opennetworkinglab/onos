package org.onosproject.cli.net;

import java.util.Collection;
import java.util.Iterator;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.resource.DefaultLabelResource;
import org.onosproject.net.resource.LabelResource;
import org.onosproject.net.resource.LabelResourceService;

@Command(scope = "onos", name = "apply-label-resource-pool",
      description = "Apply label resource from device pool by specific device id")
public class ApplyLabelResourceCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Device identity",
            required = true, multiValued = false)
    String deviceId = null;
    @Argument(index = 1, name = "applyNum",
            description = "Applying number means how many labels applications want to use.",
            required = true, multiValued = false)
    String applyNum = null;

    private static final String FMT = "deviceid=%s, labelresourceid=%s";

    @Override
    protected void execute() {
        LabelResourceService lrs = get(LabelResourceService.class);
        Collection<LabelResource> result = lrs.applyFromDevicePool(DeviceId
                .deviceId(deviceId), Long.parseLong(applyNum));
        if (result.size() > 0) {
            for (Iterator<LabelResource> iterator = result.iterator(); iterator
                    .hasNext();) {
                DefaultLabelResource defaultLabelResource = (DefaultLabelResource) iterator
                        .next();
                print(FMT, defaultLabelResource.deviceId().toString(),
                      defaultLabelResource.labelResourceId().toString());
            }
        }
    }

}
