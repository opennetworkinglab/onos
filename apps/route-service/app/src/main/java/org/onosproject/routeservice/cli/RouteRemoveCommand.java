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

package org.onosproject.routeservice.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteAdminService;

import java.util.Collections;

/**
 * Command to remove a route from the routing table.
 */
@Service
@Command(scope = "onos", name = "route-remove",
        description = "Removes a route from the route table")
public class RouteRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "prefix", description = "IP prefix of the route",
            required = true)
    String prefixString = null;

    @Argument(index = 1, name = "nextHop", description = "IP address of the next hop",
            required = true)
    String nextHopString = null;

    @Argument(index = 2, name = "source", description = "Source type of the route",
            required = false)
    String source = null;

    @Override
    protected void doExecute() {
        RouteAdminService service = AbstractShellCommand.get(RouteAdminService.class);

        IpPrefix prefix = IpPrefix.valueOf(prefixString);
        IpAddress nextHop = IpAddress.valueOf(nextHopString);

        // Routes through cli without mentioning source then it is created as STATIC,
        // otherwise routes are created with corresponding source.

        Route route = source == null ?
                new Route(Route.Source.STATIC, prefix, nextHop) :
                new Route(Route.Source.valueOf(source), prefix, nextHop);

        service.withdraw(Collections.singleton(route));
    }

}
