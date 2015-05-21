package org.onosproject.cli.net;

import java.util.Collection;
import java.util.Iterator;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.resource.label.DefaultLabelResource;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceService;

@Command(scope = "onos", name = "apply-global-label-resource-pool",
      description = "Apply global labels from global resource pool")
public class ApplyGlobalLabelResourceCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "applyNum",
            description = "Applying number means how many labels applications want to use.",
            required = true, multiValued = false)
    String applyNum = null;

    private static final String FMT = "deviceid=%s, labelresourceid=%s";

    @Override
    protected void execute() {
        LabelResourceService lrs = get(LabelResourceService.class);
        Collection<LabelResource> result =
                lrs.applyFromGlobalPool(Long.parseLong(applyNum));
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
