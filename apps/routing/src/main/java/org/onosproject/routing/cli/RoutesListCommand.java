/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.routing.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.routing.RouteEntry;
import org.onosproject.routing.RoutingService;

import java.util.Collection;

/**
 * Command to show the list of routes in SDN-IP's routing table.
 */
@Command(scope = "onos", name = "routes",
        description = "Lists all SDN-IP best routes")
public class RoutesListCommand extends AbstractShellCommand {
    @Option(name = "-s", aliases = "--summary",
            description = "SDN-IP routes summary",
            required = false, multiValued = false)
    private boolean routesSummary = false;

    private static final String FORMAT_SUMMARY_V4 =
        "Total SDN-IP IPv4 routes = %d";
    private static final String FORMAT_SUMMARY_V6 =
        "Total SDN-IP IPv6 routes = %d";
    private static final String FORMAT_HEADER =
        "   Network            Next Hop";
    private static final String FORMAT_ROUTE =
        "   %-18s %-15s";

    @Override
    protected void execute() {
        RoutingService service = AbstractShellCommand.get(RoutingService.class);

        // Print summary of the routes
        if (routesSummary) {
            printSummary(service.getRoutes4(), service.getRoutes6());
            return;
        }

        // Print all routes
        printRoutes(service.getRoutes4(), service.getRoutes6());
    }

    /**
     * Prints summary of the routes.
     *
     * @param routes4 the IPv4 routes
     * @param routes6 the IPv6 routes
     */
    private void printSummary(Collection<RouteEntry> routes4,
                              Collection<RouteEntry> routes6) {
        if (outputJson()) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode result = mapper.createObjectNode();
            result.put("totalRoutes4", routes4.size());
            result.put("totalRoutes6", routes6.size());
            print("%s", result);
        } else {
            print(FORMAT_SUMMARY_V4, routes4.size());
            print(FORMAT_SUMMARY_V6, routes6.size());
        }
    }

    /**
     * Prints all routes.
     *
     * @param routes4 the IPv4 routes to print
     * @param routes6 the IPv6 routes to print
     */
    private void printRoutes(Collection<RouteEntry> routes4,
                             Collection<RouteEntry> routes6) {
        if (outputJson()) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode result = mapper.createObjectNode();
            result.set("routes4", json(routes4));
            result.set("routes6", json(routes6));
            print("%s", result);
        } else {
            // The IPv4 routes
            print(FORMAT_HEADER);
            for (RouteEntry route : routes4) {
                printRoute(route);
            }
            print(FORMAT_SUMMARY_V4, routes4.size());
            print("");                  // Empty separator line
            // The IPv6 routes
            print(FORMAT_HEADER);
            for (RouteEntry route : routes6) {
                printRoute(route);
            }
            print(FORMAT_SUMMARY_V6, routes6.size());
        }
    }

    /**
     * Prints a route.
     *
     * @param route the route to print
     */
    private void printRoute(RouteEntry route) {
        if (route != null) {
            print(FORMAT_ROUTE, route.prefix(), route.nextHop());
        }
    }

    /**
     * Produces a JSON array of routes.
     *
     * @param routes the routes with the data
     * @return JSON array with the routes
     */
    private JsonNode json(Collection<RouteEntry> routes) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        for (RouteEntry route : routes) {
            result.add(json(mapper, route));
        }
        return result;
    }

    /**
     * Produces JSON object for a route.
     *
     * @param mapper the JSON object mapper to use
     * @param route the route with the data
     * @return JSON object for the route
     */
    private ObjectNode json(ObjectMapper mapper, RouteEntry route) {
        ObjectNode result = mapper.createObjectNode();

        result.put("prefix", route.prefix().toString());
        result.put("nextHop", route.nextHop().toString());

        return result;
    }
}
