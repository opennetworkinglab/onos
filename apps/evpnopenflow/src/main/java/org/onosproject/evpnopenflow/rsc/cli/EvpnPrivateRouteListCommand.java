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
import org.onosproject.evpnopenflow.manager.EvpnService;
import org.onosproject.evpnopenflow.manager.impl.EvpnManager;
import org.onosproject.evpnrouteservice.EvpnInstanceRoute;

import java.util.Collection;

import static org.onosproject.evpnopenflow.rsc.EvpnConstants.FORMAT_PRIVATE_ROUTE;

/**
 * Support for displaying EVPN private routes.
 */
@Service
@Command(scope = "onos", name = "evpn-private-routes", description = "Lists" +
        " all EVPN private routes")
public class EvpnPrivateRouteListCommand extends AbstractShellCommand {
    private static final String FORMAT_HEADER =
            "   VPN name            Prefix         Next Hop";

    @Override
    protected void doExecute() {
        EvpnService service = AbstractShellCommand.get(EvpnService.class);
        EvpnManager evpnManager = (EvpnManager) service;
        Collection<EvpnInstanceRoute> evpnRoutes = evpnManager.evpnInstanceRoutes;
        if (evpnRoutes != null) {
            print(FORMAT_HEADER);
            evpnRoutes.forEach(evpnInstanceRoute -> {
                print(FORMAT_PRIVATE_ROUTE, evpnInstanceRoute.evpnInstanceName(),
                      evpnInstanceRoute.prefix().address().getIp4Address(), evpnInstanceRoute
                              .getNextHopl());
            });
        }
    }

}
