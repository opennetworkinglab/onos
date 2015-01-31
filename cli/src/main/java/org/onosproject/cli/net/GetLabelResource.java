package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.resource.LabelResourcePool;
import org.onosproject.net.resource.LabelResourceService;

@Command(scope = "onos", name = "get-label-resource-pool",
      description = "get label resource pool by specific device id")
public class GetLabelResource extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Id of device", required = true, multiValued = false)
    String deviceId = null;
    private static final String FMT = "deviceid=%s, beginLabel=%s,"
            + "endLabel=%s, totalNum=%s, usedNum=%s, currentUsedMaxLabelId=%s,"
            + "releaseLabelIds=%s";

    @Override
    protected void execute() {
        // TODO Auto-generated method stub
        LabelResourceService lrs = get(LabelResourceService.class);
        LabelResourcePool pool = lrs.getLabelResourcePool(DeviceId
                .deviceId(deviceId));
        if (pool != null) {
            print(FMT, pool.getDeviceId().toString(), pool.getBeginLabel(),
                  pool.getEndLabel(), pool.getTotalNum(), pool.getUsedNum(),
                  pool.getCurrentUsedMaxLabelId(), pool.getReleaseLabelId()
                          .toString());
        } else {
            print(FMT, deviceId, null, null, null, null, null, null);
        }
    }

}
