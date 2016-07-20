package org.onosproject.segmentrouting.cli;


import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.segmentrouting.SegmentRoutingService;

/**
 * Command to manually trigger routing and rule-population in the network.
 *
 */
@Command(scope = "onos", name = "sr-reroute-network",
        description = "Repopulate routing rules given current network state")
public class RerouteNetworkCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);
        srService.rerouteNetwork();
    }

}
