package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.incubator.net.resource.label.LabelResourcePool;
import org.onosproject.incubator.net.resource.label.LabelResourceService;

@Command(scope = "onos", name = "get-label-resource-pool",
      description = "Gets label resource pool information by a specific device id")
public class GetLabelResourceCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Device identity", required = true, multiValued = false)
    String deviceId = null;
    private static final String FMT = "deviceid=%s, beginLabel=%s,"
            + "endLabel=%s, totalNum=%s, usedNum=%s, currentUsedMaxLabelId=%s,"
            + "releaseLabelIds=%s";

    @Override
    protected void execute() {
        LabelResourceService lrs = get(LabelResourceService.class);
        LabelResourcePool pool = lrs.getDeviceLabelResourcePool(DeviceId
                .deviceId(deviceId));
        if (pool != null) {
            print(FMT, pool.deviceId().toString(), pool.beginLabel(),
                  pool.endLabel(), pool.totalNum(), pool.usedNum(),
                  pool.currentUsedMaxLabelId(), pool.releaseLabelId()
                          .toString());
        } else {
            print(FMT, deviceId, null, null, null, null, null, null);
        }
    }

}
