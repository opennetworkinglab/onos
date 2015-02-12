package org.onosproject.cli.net;

import java.util.Collection;
import java.util.Iterator;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.resource.ApplyLabelNumber;
import org.onosproject.net.resource.DefaultLabelResource;
import org.onosproject.net.resource.LabelResourceService;

@Command(scope = "onos", name = "apply-label-resource-pool",
      description = "apply label resource pool by specific device id")
public class ApplyLabelResourceCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Id of device",
            required = true, multiValued = false)
    String deviceId = null;
    @Argument(index = 1, name = "applyNum",
            description = "applyNum", required = true, multiValued = false)
    String applyNum = null;

    private static final String FMT = "deviceid=%s, labelresourceid=%s";

    @Override
    protected void execute() {
        // TODO Auto-generated method stub
        LabelResourceService lrs = get(LabelResourceService.class);
        Collection<DefaultLabelResource> result = lrs.applyFromDevicePool(DeviceId
                .deviceId(deviceId), ApplyLabelNumber.applyLabelNumber(Long.parseLong(applyNum)));
        if (result.size() > 0) {
            for (Iterator<DefaultLabelResource> iterator = result.iterator(); iterator
                    .hasNext();) {
                DefaultLabelResource defaultLabelResource = (DefaultLabelResource) iterator
                        .next();
                print(FMT, defaultLabelResource.getDeviceId().toString(),
                      defaultLabelResource.getLabelResourceId().toString());
            }
        }
    }

}
