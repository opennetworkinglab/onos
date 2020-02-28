/*
 * Copyright 2016-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.mcast.api.McastRoute;
import org.onosproject.mcast.api.MulticastRouteService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Displays the source, multicast group flows entries.
 */
@Service
@Command(scope = "onos", name = "mcast-host-show", description = "Displays the source, multicast group flows")
public class McastShowHostCommand extends AbstractShellCommand {

    // Format for group line
    private static final String FORMAT_MAPPING = "origin=%s, group=%s, source IP=%s, sources=%s, sinks=%s\n";
    private StringBuilder routesBuilder = new StringBuilder();
    private ArrayNode routesNode = mapper().createArrayNode();

    @Option(name = "-gAddr", aliases = "--groupAddress",
            description = "IP Address of the multicast group",
            valueToShowInHelp = "224.0.0.0",
            required = false, multiValued = false)
    @Completion(McastGroupCompleter.class)
    String gAddr = null;

    @Override
    protected void doExecute() {
        // Get the service
        MulticastRouteService mcastService = get(MulticastRouteService.class);
        // Get the routes
        Set<McastRoute> routes = mcastService.getRoutes();
        // Verify mcast group
        if (!isNullOrEmpty(gAddr)) {
            // Let's find the group
            IpAddress mcastGroup = IpAddress.valueOf(gAddr);
            McastRoute mcastRoute = routes.stream()
                    .filter(route -> route.group().equals(mcastGroup))
                    .findAny().orElse(null);
            // If it exists
            if (mcastRoute != null) {
                prepareResult(mcastService, mcastRoute);
            }
        } else {
            routes.stream()
                    .filter(mcastRoute -> mcastRoute.group().isIp4())
                    .sorted(Comparator.comparing(McastRoute::group))
                    .forEach(route -> {
                        prepareResult(mcastService, route);
                    });
            routes.stream()
                    .filter(mcastRoute -> mcastRoute.group().isIp6())
                    .sorted(Comparator.comparing(McastRoute::group))
                    .forEach(route -> {
                        prepareResult(mcastService, route);
                    });
        }
        if (outputJson()) {
            print("%s", routesNode);
        } else {
            print("%s", routesBuilder.toString());
        }
    }

    private void prepareResult(MulticastRouteService mcastService, McastRoute route) {
        if (outputJson()) {
            // McastHostRouteCodec is used to encode McastRoute
            ObjectNode routeNode = jsonForEntity(route, McastRoute.class);
            routesNode.add(routeNode);
        } else {
            Map<HostId, Set<ConnectPoint>> sinks = mcastService.routeData(route).sinks();
            Map<HostId, Set<ConnectPoint>> sources = mcastService.routeData(route).sources();
            String srcIp = "*";
            if (route.source().isPresent()) {
                srcIp = route.source().get().toString();
            }
            routesBuilder.append(String.format(FORMAT_MAPPING, route.type(), route.group(), srcIp, sources, sinks));
        }
    }

}
