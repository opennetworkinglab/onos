package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.resource.LabelResourcePool;
import org.onosproject.net.resource.LabelResourceService;

@Command(scope = "onos", name = "get-global-label-resource-pool",
      description = "Gets global label resource pool information.")
public class GetGlobalLabelResourceCommand extends AbstractShellCommand {
    private static final String FMT = "deviceid=%s, beginLabel=%s,"
            + "endLabel=%s, totalNum=%s, usedNum=%s, currentUsedMaxLabelId=%s,"
            + "releaseLabelIds=%s";

    @Override
    protected void execute() {
        LabelResourceService lrs = get(LabelResourceService.class);
        LabelResourcePool pool = lrs.getGlobalLabelResourcePool();
        if (pool != null) {
            print(FMT, pool.deviceId().toString(), pool.beginLabel(),
                  pool.endLabel(), pool.totalNum(), pool.usedNum(),
                  pool.currentUsedMaxLabelId(), pool.releaseLabelId()
                          .toString());
        }
    }

}
