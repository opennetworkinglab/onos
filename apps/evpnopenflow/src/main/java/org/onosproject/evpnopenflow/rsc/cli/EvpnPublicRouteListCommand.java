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

package org.onosproject.evpnopenflow.rsc.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.evpnrouteservice.EvpnRoute;
import org.onosproject.evpnrouteservice.EvpnRouteSet;
import org.onosproject.evpnrouteservice.EvpnRouteStore;

import java.util.Collection;

import static org.onosproject.evpnopenflow.rsc.EvpnConstants.FORMAT_PUBLIC_ROUTE;

/**
 * Support for displaying EVPN public routes.
 */
@Service
@Command(scope = "onos", name = "evpn-public-routes", description = "Lists" +
        " all EVPN public routes")
public class EvpnPublicRouteListCommand extends AbstractShellCommand {
    private static final String FORMAT_HEADER =
            "   MAC                  Prefix          Next Hop";

    @Override
    protected void doExecute() {
        EvpnRouteStore evpnRouteStore = AbstractShellCommand.get(EvpnRouteStore.class);

        evpnRouteStore.getRouteTables().forEach(routeTableId -> {
            Collection<EvpnRouteSet> routes
                    = evpnRouteStore.getRoutes(routeTableId);
            if (routes != null) {
                routes.forEach(route -> {
                    Collection<EvpnRoute> evpnRoutes = route.routes();
                    print(FORMAT_HEADER);
                    evpnRoutes.forEach(evpnRoute -> {
                        print(FORMAT_PUBLIC_ROUTE, evpnRoute.prefixMac(),
                              evpnRoute.prefixIp().address().getIp4Address(),
                              evpnRoute.ipNextHop());
                    });
                });
            }
        });
    }
}
