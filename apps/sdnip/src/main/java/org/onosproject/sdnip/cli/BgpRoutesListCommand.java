/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.sdnip.cli;

import java.util.Collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.sdnip.SdnIpService;
import org.onosproject.sdnip.bgp.BgpConstants.Update.AsPath;
import org.onosproject.sdnip.bgp.BgpConstants.Update.Origin;
import org.onosproject.sdnip.bgp.BgpRouteEntry;

/**
 * Command to show the routes learned through BGP.
 */
@Command(scope = "onos", name = "bgp-routes",
         description = "Lists all routes received from BGP")
public class BgpRoutesListCommand extends AbstractShellCommand {
    @Option(name = "-s", aliases = "--summary",
            description = "BGP routes summary",
            required = false, multiValued = false)
    private boolean routesSummary = false;

    private static final String FORMAT_SUMMARY = "Total BGP routes = %d";
    private static final String FORMAT_ROUTE =
            "prefix=%s, nexthop=%s, origin=%s, localpref=%s, med=%s, aspath=%s, bgpid=%s";

    @Override
    protected void execute() {
        SdnIpService service = get(SdnIpService.class);

        // Print summary of the routes
        if (routesSummary) {
            printSummary(service.getBgpRoutes());
            return;
        }

        // Print all routes
        printRoutes(service.getBgpRoutes());
    }

    /**
     * Prints summary of the routes.
     *
     * @param routes the routes
     */
    private void printSummary(Collection<BgpRouteEntry> routes) {
        if (outputJson()) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode result = mapper.createObjectNode();
            result.put("totalRoutes", routes.size());
            print("%s", result);
        } else {
            print(FORMAT_SUMMARY, routes.size());
        }
    }

    /**
     * Prints all routes.
     *
     * @param routes the routes to print
     */
    private void printRoutes(Collection<BgpRouteEntry> routes) {
        if (outputJson()) {
            print("%s", json(routes));
        } else {
            for (BgpRouteEntry route : routes) {
                printRoute(route);
            }
        }
    }

    /**
     * Prints a BGP route.
     *
     * @param route the route to print
     */
    private void printRoute(BgpRouteEntry route) {
        if (route != null) {
            print(FORMAT_ROUTE, route.prefix(), route.nextHop(),
                  Origin.typeToString(route.getOrigin()),
                  route.getLocalPref(), route.getMultiExitDisc(),
                  route.getAsPath(), route.getBgpSession().getRemoteBgpId());
        }
    }

    /**
     * Produces a JSON array of routes.
     *
     * @param routes the routes with the data
     * @return JSON array with the routes
     */
    private JsonNode json(Collection<BgpRouteEntry> routes) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        for (BgpRouteEntry route : routes) {
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
    private ObjectNode json(ObjectMapper mapper, BgpRouteEntry route) {
        ObjectNode result = mapper.createObjectNode();

        result.put("prefix", route.prefix().toString());
        result.put("nextHop", route.nextHop().toString());
        result.put("bgpId", route.getBgpSession().getRemoteBgpId().toString());
        result.put("origin", Origin.typeToString(route.getOrigin()));
        result.put("asPath", json(mapper, route.getAsPath()));
        result.put("localPref", route.getLocalPref());
        result.put("multiExitDisc", route.getMultiExitDisc());

        return result;
    }

    /**
     * Produces JSON object for an AS path.
     *
     * @param mapper the JSON object mapper to use
     * @param asPath the AS path with the data
     * @return JSON object for the AS path
     */
    private ObjectNode json(ObjectMapper mapper, BgpRouteEntry.AsPath asPath) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode pathSegmentsJson = mapper.createArrayNode();
        for (BgpRouteEntry.PathSegment pathSegment : asPath.getPathSegments()) {
            ObjectNode pathSegmentJson = mapper.createObjectNode();
            pathSegmentJson.put("type",
                                AsPath.typeToString(pathSegment.getType()));
            ArrayNode segmentAsNumbersJson = mapper.createArrayNode();
            for (Long asNumber : pathSegment.getSegmentAsNumbers()) {
                segmentAsNumbersJson.add(asNumber);
            }
            pathSegmentJson.put("segmentAsNumbers", segmentAsNumbersJson);
            pathSegmentsJson.add(pathSegmentJson);
        }
        result.put("pathSegments", pathSegmentsJson);

        return result;
    }
}
