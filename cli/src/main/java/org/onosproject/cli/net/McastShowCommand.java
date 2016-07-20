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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.MulticastRouteService;

import java.util.Set;

/**
 * Displays the source, multicast group flows entries.
 */
@Command(scope = "onos", name = "mcast-show", description = "Displays the source, multicast group flows")
public class McastShowCommand extends AbstractShellCommand {

    private static final String FORMAT = "route=%s, source=%s, sinks=%s";

    @Override
    protected void execute() {
        MulticastRouteService mcastService = get(MulticastRouteService.class);

        Set<McastRoute> routes = mcastService.getRoutes();

        for (McastRoute route : routes) {
            Set<ConnectPoint> sinks = mcastService.fetchSinks(route);
            ConnectPoint source = mcastService.fetchSource(route);

            print(FORMAT, route, source, sinks);
        }
    }

}
