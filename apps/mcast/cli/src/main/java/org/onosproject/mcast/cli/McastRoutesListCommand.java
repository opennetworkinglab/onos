/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.mcast.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.mcast.api.McastRoute;
import org.onosproject.mcast.api.MulticastRouteService;
import org.onosproject.net.ConnectPoint;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Displays the source, multicast group flows entries.
 */
@Service
@Command(scope = "onos", name = "mcast-host-routes",
        description = "Lists routes in the mcast route store")
public class McastRoutesListCommand extends AbstractShellCommand {

    // Format for total
    private static final String FORMAT_TOTAL = "   Total: %d";

    // Format for ipv4
    private static final String FORMAT_ROUTE = "%-1s   %-18s %-15s %s   %s    %s";
    // Format for ipv6
    private static final String FORMAT_ROUTE6 = "%-1s   %-40s %-36s %s   %s    %s";

    // Table header
    private static final String FORMAT_TABLE = "Table: %s";
    private static final String GROUP = "Group";
    private static final String SOURCE = "Source";
    private static final String ORIGIN = "Origin";
    private static final String SOURCES = "Sources";
    private static final String SINKS = "Sinks";

    @Override
    protected void doExecute() {
        // Get the service
        MulticastRouteService mcastService = get(MulticastRouteService.class);
        // Get the routes
        Set<McastRoute> routes = mcastService.getRoutes();
        // Filter ipv4
        Set<McastRoute> ipv4Routes = routes.stream()
                .filter(mcastRoute -> mcastRoute.group().isIp4())
                .collect(Collectors.toSet());
        // Filter ipv6
        Set<McastRoute> ipv6Routes = routes.stream()
                .filter(mcastRoute -> mcastRoute.group().isIp6())
                .collect(Collectors.toSet());
        // Print header
        print(FORMAT_TABLE, "ipv4");
        print(FORMAT_ROUTE, "", GROUP, SOURCE, ORIGIN, SOURCES, SINKS);
        // Print ipv4 mcast routing entries
        ipv4Routes.stream()
                .sorted(Comparator.comparing(McastRoute::group))
                .forEach(route -> {
                    // Get sinks and sources
                    Set<ConnectPoint> sources = mcastService.sources(route);
                    Set<ConnectPoint> sinks = mcastService.sinks(route);
                    Optional<IpAddress> sourceIp = route.source();
                    String src = "*      ";
                    if (sourceIp.isPresent()) {
                        src = sourceIp.get().toString();
                    }
                    print(FORMAT_ROUTE, "", route.group(), src,
                            route.type(), sources.size(), "       " + sinks.size());
                });
        print(FORMAT_TOTAL, ipv4Routes.size());
        print("");

        // Print header
        print(FORMAT_TABLE, "ipv6");
        print(FORMAT_ROUTE6, "", GROUP, SOURCE, ORIGIN, SOURCES, SINKS);
        // Print ipv6 mcast routing entries
        ipv6Routes.stream()
                .sorted(Comparator.comparing(McastRoute::group))
                .forEach(route -> {
                    // Get sinks and sources
                    Set<ConnectPoint> sources = mcastService.sources(route);
                    Set<ConnectPoint> sinks = mcastService.sinks(route);
                    Optional<IpAddress> sourceIp = route.source();
                    String src = "*      ";
                    if (sourceIp.isPresent()) {
                        src = sourceIp.get().toString();
                    }
                    print(FORMAT_ROUTE6, "", route.group(), src,
                            route.type(), sources.size(), "       " + sinks.size());
                });
        print(FORMAT_TOTAL, ipv6Routes.size());
        print("");
    }


}
