package org.onlab.onos.sdnip.cli;

import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.sdnip.RouteEntry;
import org.onlab.onos.sdnip.SdnIpService;

/**
 * Command to show the list of routes in SDN-IP's routing table.
 */
@Command(scope = "onos", name = "routes",
        description = "Lists all routes known to SDN-IP")
public class RoutesListCommand extends AbstractShellCommand {

    private static final String FORMAT =
            "prefix=%s, nexthop=%s";

    @Override
    protected void execute() {
        SdnIpService service = get(SdnIpService.class);

        for (RouteEntry route : service.getRoutes()) {
            printRoute(route);
        }
    }

    private void printRoute(RouteEntry route) {
        if (route != null) {
            print(FORMAT, route.prefix(), route.nextHop());
        }
    }
}
