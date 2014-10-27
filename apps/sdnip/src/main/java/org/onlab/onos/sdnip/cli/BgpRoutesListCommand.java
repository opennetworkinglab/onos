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
import org.onlab.onos.sdnip.SdnIpService;
import org.onlab.onos.sdnip.bgp.BgpConstants;
import org.onlab.onos.sdnip.bgp.BgpRouteEntry;

/**
 * Command to show the routes learned through BGP.
 */
@Command(scope = "onos", name = "bgp-routes",
         description = "Lists all routes received from BGP")
public class BgpRoutesListCommand extends AbstractShellCommand {

    private static final String FORMAT =
            "prefix=%s, nexthop=%s, origin=%s, localpref=%s, med=%s, aspath=%s, bgpid=%s";

    @Override
    protected void execute() {
        SdnIpService service = get(SdnIpService.class);

        for (BgpRouteEntry route : service.getBgpRoutes()) {
            printRoute(route);
        }
    }

    private void printRoute(BgpRouteEntry route) {
        if (route != null) {
            print(FORMAT, route.prefix(), route.nextHop(),
                    originToString(route.getOrigin()), route.getLocalPref(),
                    route.getMultiExitDisc(), route.getAsPath(),
                    route.getBgpSession().getRemoteBgpId());
        }
    }

    private static String originToString(int origin) {
        String originString = "UNKNOWN";

        switch (origin) {
        case BgpConstants.Update.Origin.IGP:
            originString = "IGP";
            break;
        case BgpConstants.Update.Origin.EGP:
            originString = "EGP";
            break;
        case BgpConstants.Update.Origin.INCOMPLETE:
            originString = "INCOMPLETE";
            break;
        default:
            break;
        }

        return originString;
    }

}
