/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.routing.NextHop;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteService;

import java.util.Collection;
import java.util.Set;

/**
 * Command to show information about routing next hops.
 */
@Command(scope = "onos", name = "next-hops",
        description = "Lists all next hops in the route store")
public class NextHopsListCommand extends AbstractShellCommand {

    private static final String FORMAT_HEADER =
        "   Network            Next Hop";
    private static final String FORMAT_ROUTE =
        "   %-18s %-15s";

    private static final String FORMAT_TABLE = "Table: %s";
    private static final String FORMAT_TOTAL = "   Total: %d";

    private static final String FORMAT = "ip=%s, mac=%s, loc=%s, numRoutes=%s";

    @Override
    protected void execute() {
        RouteService service = AbstractShellCommand.get(RouteService.class);

        Set<NextHop> nextHops = service.getNextHops();

        nextHops.forEach(nextHop -> {
            Collection<Route> routes = service.getRoutesForNextHop(nextHop.ip());
            print(FORMAT, nextHop.ip(), nextHop.mac(), nextHop.location(), routes.size());
        });
    }

}
