package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.HostToHostIntent;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentService;

/**
 * Lists all shortest-paths paths between the specified source and
 * destination devices.
 */
@Command(scope = "onos", name = "add-intent",
         description = "Installs HostToHostIntent between the specified source and destination devices")
public class IntentInstallCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "src", description = "Source device ID",
              required = true, multiValued = false)
    String src = null;

    @Argument(index = 1, name = "dst", description = "Destination device ID",
              required = true, multiValued = false)
    String dst = null;

    private static long id = 1;

    @Override
    protected void execute() {
        IntentService service = get(IntentService.class);
        HostService hosts = get(HostService.class);

        HostId srcId = HostId.hostId(src);
        HostId dstId = HostId.hostId(dst);

        TrafficSelector.Builder builder = DefaultTrafficSelector.builder();
        builder.matchEthSrc(hosts.getHost(srcId).mac())
                .matchEthDst(hosts.getHost(dstId).mac());

        TrafficTreatment.Builder treat = DefaultTrafficTreatment.builder();

        HostToHostIntent intent =
                new HostToHostIntent(new IntentId(id++), srcId, dstId,
                                     builder.build(), treat.build());

        log.info("Adding intent {}", intent);

        service.submit(intent);
    }

}
