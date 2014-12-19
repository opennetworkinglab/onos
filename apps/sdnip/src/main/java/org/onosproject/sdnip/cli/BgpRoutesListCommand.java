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

import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.sdnip.SdnIpService;
import org.onosproject.sdnip.bgp.BgpConstants.Update;
import org.onosproject.sdnip.bgp.BgpRouteEntry;
import org.onosproject.sdnip.bgp.BgpSession;

/**
 * Command to show the routes learned through BGP.
 */
@Command(scope = "onos", name = "bgp-routes",
         description = "Lists all BGP best routes")
public class BgpRoutesListCommand extends AbstractShellCommand {
    @Option(name = "-s", aliases = "--summary",
            description = "BGP routes summary",
            required = false, multiValued = false)
    private boolean routesSummary = false;

    @Option(name = "-n", aliases = "--neighbor",
            description = "Routes from a BGP neighbor",
            required = false, multiValued = false)
    private String bgpNeighbor;

    private static final String FORMAT_SUMMARY = "Total BGP routes = %d";
    private static final String FORMAT_HEADER =
        "   Network            Next Hop        Origin LocalPref       MED BGP-ID";
    private static final String FORMAT_ROUTE_LINE1 =
        "   %-18s %-15s %6s %9s %9s %-15s";
    private static final String FORMAT_ROUTE_LINE2 =
        "                      AsPath %s";

    @Override
    protected void execute() {
        SdnIpService service = get(SdnIpService.class);

        // Print summary of the routes
        if (routesSummary) {
            printSummary(service.getBgpRoutes());
            return;
        }

        BgpSession foundBgpSession = null;
        if (bgpNeighbor != null) {
            // Print the routes from a single neighbor (if found)
            for (BgpSession bgpSession : service.getBgpSessions()) {
                if (bgpSession.getRemoteBgpId().toString().equals(bgpNeighbor)) {
                    foundBgpSession = bgpSession;
                    break;
                }
            }
            if (foundBgpSession == null) {
                print("BGP neighbor %s not found", bgpNeighbor);
                return;
            }
        }

        // Print the routes
        if (foundBgpSession != null) {
            printRoutes(foundBgpSession.bgpRibIn().values());
        } else {
            printRoutes(service.getBgpRoutes());
        }
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
            print(FORMAT_HEADER);
            for (BgpRouteEntry route : routes) {
                printRoute(route);
            }
            print(FORMAT_SUMMARY, routes.size());
        }
    }

    /**
     * Prints a BGP route.
     *
     * @param route the route to print
     */
    private void printRoute(BgpRouteEntry route) {
        if (route != null) {
            print(FORMAT_ROUTE_LINE1, route.prefix(), route.nextHop(),
                  Update.Origin.typeToString(route.getOrigin()),
                  route.getLocalPref(), route.getMultiExitDisc(),
                  route.getBgpSession().getRemoteBgpId());
            print(FORMAT_ROUTE_LINE2, asPath4Cli(route.getAsPath()));
        }
    }

    /**
     * Formats the AS Path as a string that can be shown on the CLI.
     *
     * @param asPath the AS Path to format
     * @return the AS Path as a string
     */
    private String asPath4Cli(BgpRouteEntry.AsPath asPath) {
        ArrayList<BgpRouteEntry.PathSegment> pathSegments =
            asPath.getPathSegments();

        if (pathSegments.isEmpty()) {
            return "[none]";
        }

        final StringBuilder builder = new StringBuilder();
        for (BgpRouteEntry.PathSegment pathSegment : pathSegments) {
            String prefix = null;
            String suffix = null;
            switch (pathSegment.getType()) {
            case Update.AsPath.AS_SET:
                prefix = "[AS-Set";
                suffix = "]";
                break;
            case Update.AsPath.AS_SEQUENCE:
                break;
            case Update.AsPath.AS_CONFED_SEQUENCE:
                prefix = "[AS-Confed-Seq";
                suffix = "]";
                break;
            case Update.AsPath.AS_CONFED_SET:
                prefix = "[AS-Confed-Set";
                suffix = "]";
                break;
            default:
                builder.append(String.format("(type = %s)",
                        Update.AsPath.typeToString(pathSegment.getType())));
                break;
            }

            if (prefix != null) {
                if (builder.length() > 0) {
                    builder.append(" ");        // Separator
                }
                builder.append(prefix);
            }
            // Print the AS numbers
            for (Long asn : pathSegment.getSegmentAsNumbers()) {
                if (builder.length() > 0) {
                    builder.append(" ");        // Separator
                }
                builder.append(String.format("%d", asn));
            }
            if (suffix != null) {
                // No need for separator
                builder.append(prefix);
            }
        }
        return builder.toString();
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
        result.put("origin", Update.Origin.typeToString(route.getOrigin()));
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
                                Update.AsPath.typeToString(pathSegment.getType()));
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
