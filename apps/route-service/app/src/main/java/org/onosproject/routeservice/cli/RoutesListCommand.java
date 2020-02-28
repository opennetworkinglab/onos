/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.routeservice.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteInfo;
import org.onosproject.routeservice.RouteService;
import org.onosproject.routeservice.RouteTableId;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * Command to show the routes in the routing tables.
 */
@Service
@Command(scope = "onos", name = "routes",
        description = "Lists routes in the route store")
public class RoutesListCommand extends AbstractShellCommand {

    private static final String NETWORK = "Network";
    private static final String NEXTHOP = "Next Hop";
    private static final String SOURCE = "Source";
    private static final String NODE = "Node";

    private static final String FORMAT_ROUTE = "%-1s %-1s  %-18s %-15s %s (%s)";
    private static final String FORMAT_ROUTE6 = "%-1s %-1s  %-43s %-39s %s (%s)";

    private static final String FORMAT_TABLE = "Table: %s";
    private static final String FORMAT_TOTAL = "   Total: %d";

    @Override
    protected void doExecute() {
        RouteService service = AbstractShellCommand.get(RouteService.class);

        if (outputJson()) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode result = mapper.createObjectNode();
            result.set("routes4", json(service.getRoutes(new RouteTableId("ipv4"))));
            result.set("routes6", json(service.getRoutes(new RouteTableId("ipv6"))));
            print("%s", result);
        } else {
            print("B: Best route, R: Resolved route\n");
            service.getRouteTables().forEach(id -> {
                Collection<RouteInfo> tableRoutes = service.getRoutes(id);

                String format = tableRoutes.stream().anyMatch(route -> route.prefix().isIp6()) ?
                        FORMAT_ROUTE6 : FORMAT_ROUTE;

                // Print header
                print(FORMAT_TABLE, id);
                print(format, "B", "R", NETWORK, NEXTHOP, SOURCE, NODE);

                // Print routing entries
                tableRoutes.stream()
                        .sorted(Comparator.comparing(r -> r.prefix().address()))
                        .forEach(route -> this.print(format, route));

                print(FORMAT_TOTAL, tableRoutes.size());
                print("");
            });
        }
    }

    private void print(String format, RouteInfo routeInfo) {
        routeInfo.allRoutes().stream()
                .sorted(Comparator.comparing(ResolvedRoute::nextHop))
                .forEach(r -> print(format,
                        isBestRoute(routeInfo.bestRoute(), r) ? ">" : "",
                        isResolvedRoute(r) ? "*" : "",
                        r.prefix(), r.nextHop(), r.route().source(), r.route().sourceNode()));
    }

    private boolean isBestRoute(Optional<ResolvedRoute> bestRoute, ResolvedRoute route) {
        return Objects.equals(bestRoute.orElse(null), route);
    }

    private boolean isResolvedRoute(ResolvedRoute route) {
        return route.nextHopMac() != null && route.nextHopVlan() != null;
    }

    /**
     * Produces a JSON array of routes.
     *
     * @param routes the routes with the data
     * @return JSON array with the routes
     */
    private JsonNode json(Collection<RouteInfo> routes) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        routes.stream()
                .flatMap(ri -> ri.allRoutes().stream())
                .forEach(r -> {
                    // use RouteCodec to encode the Route object inside ResolvedRoute
                    ObjectNode routeNode = jsonForEntity(r.route(), Route.class);
                    if (r.nextHopMac() != null) {
                        routeNode.put("nextHopMac", r.nextHopMac().toString());
                    }
                    if (r.nextHopVlan() != null) {
                        routeNode.put("nextHopVlan", r.nextHopVlan().toString());
                    }
                    result.add(routeNode);
                });

        return result;
    }

}
