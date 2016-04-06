/*
 * Copyright 2016 Open Networking Laboratory
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
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteService;
import org.onosproject.incubator.net.routing.RouteTableId;

import java.util.Collection;
import java.util.Map;

/**
 * Command to show the routes in the routing tables.
 */
// TODO update command name when we switch over to new rib
@Command(scope = "onos", name = "routes2",
        description = "Lists all routes in the route store")
public class RoutesListCommand extends AbstractShellCommand {

    private static final String FORMAT_HEADER =
        "   Network            Next Hop";
    private static final String FORMAT_ROUTE =
        "   %-18s %-15s";

    private static final String FORMAT_TABLE = "Table: %s";
    private static final String FORMAT_TOTAL = "   Total: %d";

    @Override
    protected void execute() {
        RouteService service = AbstractShellCommand.get(RouteService.class);

        Map<RouteTableId, Collection<Route>> allRoutes = service.getAllRoutes();

        allRoutes.forEach((id, routes) -> {
            print(FORMAT_TABLE, id);
            print(FORMAT_HEADER);
            routes.forEach(r -> print(FORMAT_ROUTE, r.prefix(), r.nextHop()));
            print(FORMAT_TOTAL, routes.size());
            print("");
        });

    }

}
