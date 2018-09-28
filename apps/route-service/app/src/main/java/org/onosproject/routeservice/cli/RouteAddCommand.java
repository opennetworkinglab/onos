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
 * Command to add a route to the routing table.
 */
@Service
@Command(scope = "onos", name = "route-add",
        description = "Adds a route to the route table")
public class RouteAddCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "prefix", description = "IP prefix of the route",
            required = true)
    String prefixString = null;

    @Argument(index = 1, name = "nextHop", description = "IP address of the next hop",
            required = true)
    String nextHopString = null;

    @Override
    protected void doExecute() {
        RouteAdminService service = AbstractShellCommand.get(RouteAdminService.class);

        IpPrefix prefix = IpPrefix.valueOf(prefixString);
        IpAddress nextHop = IpAddress.valueOf(nextHopString);

        service.update(Collections.singleton(new Route(Route.Source.STATIC, prefix, nextHop)));
    }

}
