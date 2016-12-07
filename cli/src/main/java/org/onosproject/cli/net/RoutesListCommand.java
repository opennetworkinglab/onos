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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteService;
import org.onosproject.incubator.net.routing.RouteTableId;

import java.util.Collection;
import java.util.Map;

/**
 * Command to show the routes in the routing tables.
 */
@Command(scope = "onos", name = "routes",
        description = "Lists all routes in the route store")
public class RoutesListCommand extends AbstractShellCommand {

    @Option(name = "-s", aliases = "--summary",
            description = "Show summary of routes")
    private boolean summary = false;

    private static final String FORMAT_SUMMARY =
            "Number of routes in table %s: %s";
    private static final String FORMAT_HEADER =
        "   Network            Next Hop        Source";
    private static final String FORMAT_ROUTE =
        "   %-18s %-15s %-10s";

    private static final String FORMAT_TABLE = "Table: %s";
    private static final String FORMAT_TOTAL = "   Total: %d";

    @Override
    protected void execute() {
        RouteService service = AbstractShellCommand.get(RouteService.class);

        Map<RouteTableId, Collection<Route>> allRoutes = service.getAllRoutes();

        if (summary) {
            if (outputJson()) {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode result = mapper.createObjectNode();
                result.put("totalRoutes4", allRoutes.get(new RouteTableId("ipv4")).size());
                result.put("totalRoutes6", allRoutes.get(new RouteTableId("ipv6")).size());
                print("%s", result);
            } else {
                allRoutes.forEach((id, routes) -> print(FORMAT_SUMMARY, id, routes.size()));
            }

            return;
        }

        if (outputJson()) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode result = mapper.createObjectNode();
            result.set("routes4", json(allRoutes.get(new RouteTableId("ipv4"))));
            result.set("routes6", json(allRoutes.get(new RouteTableId("ipv6"))));
            print("%s", result);
        } else {
            allRoutes.forEach((id, routes) -> {
                print(FORMAT_TABLE, id);
                print(FORMAT_HEADER);
                routes.forEach(r -> print(FORMAT_ROUTE, r.prefix(), r.nextHop(), r.source()));
                print(FORMAT_TOTAL, routes.size());
                print("");
            });
        }

    }

    /**
     * Produces a JSON array of routes.
     *
     * @param routes the routes with the data
     * @return JSON array with the routes
     */
    private JsonNode json(Collection<Route> routes) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        for (Route route : routes) {
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
    private ObjectNode json(ObjectMapper mapper, Route route) {
        ObjectNode result = mapper.createObjectNode();

        result.put("prefix", route.prefix().toString());
        result.put("nextHop", route.nextHop().toString());

        return result;
    }

}
