package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.intent.HostToHostIntent;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentService;

/**
 * Installs host-to-host connectivity intent.
 */
@Command(scope = "onos", name = "add-host-intent",
         description = "Installs host-to-host connectivity intent")
public class AddHostToHostIntentCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "one", description = "One host ID",
              required = true, multiValued = false)
    String one = null;

    @Argument(index = 1, name = "two", description = "Another host ID",
              required = true, multiValued = false)
    String two = null;

    private static long id = 1;

    @Override
    protected void execute() {
        IntentService service = get(IntentService.class);

        HostId oneId = HostId.hostId(one);
        HostId twoId = HostId.hostId(two);

        TrafficSelector selector = DefaultTrafficSelector.builder().build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

        HostToHostIntent intent =
                new HostToHostIntent(new IntentId(id++), oneId, twoId,
                                     selector, treatment);
        service.submit(intent);
    }

}
