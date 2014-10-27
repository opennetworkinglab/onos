/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.sdnip.cli;

import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.sdnip.RouteEntry;
import org.onlab.onos.sdnip.SdnIpService;

/**
 * Command to show the list of routes in SDN-IP's routing table.
 */
@Command(scope = "onos", name = "routes",
        description = "Lists all routes known to SDN-IP")
public class RoutesListCommand extends AbstractShellCommand {

    private static final String FORMAT =
            "prefix=%s, nexthop=%s";

    @Override
    protected void execute() {
        SdnIpService service = get(SdnIpService.class);

        for (RouteEntry route : service.getRoutes()) {
            printRoute(route);
        }
    }

    private void printRoute(RouteEntry route) {
        if (route != null) {
            print(FORMAT, route.prefix(), route.nextHop());
        }
    }
}
