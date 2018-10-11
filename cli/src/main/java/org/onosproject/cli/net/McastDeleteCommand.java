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
package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.PlaceholderCompleter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.MulticastRouteService;

/**
 * Deletes a multicast route.
 */
@Service
@Command(scope = "onos", name = "mcast-delete",
        description = "Delete a multicast route flow")
public class McastDeleteCommand extends AbstractShellCommand {

    // Delete format for group line
    private static final String D_FORMAT_MAPPING = "Deleted the mcast route: " +
            "origin=%s, group=%s, source=%s";

    // Update format for group line
    private static final String U_FORMAT_MAPPING = "Updated the mcast route: " +
            "origin=%s, group=%s, source=%s";

    @Argument(index = 0, name = "sAddr",
            description = "IP Address of the multicast source. '*' can be used for any source (*, G) entry",
            required = true, multiValued = false)
    @Completion(PlaceholderCompleter.class)
    String sAddr = null;

    @Argument(index = 1, name = "gAddr",
            description = "IP Address of the multicast group. '*' can be used to denote all groups",
            required = true, multiValued = false)
    @Completion(McastGroupCompleter.class)
    String gAddr = null;

    @Argument(index = 2, name = "egressList",
            description = "Egress id/port",
            required = false, multiValued = true)
    @Completion(ConnectPointCompleter.class)
    String[] egressList = null;


    @Override
    protected void doExecute() {
        MulticastRouteService mcastRouteManager = get(MulticastRouteService.class);

        if ("*".equals(sAddr) && "*".equals(gAddr)) {
            // Clear all routes
            mcastRouteManager.getRoutes().forEach(mcastRouteManager::remove);
            return;
        }

        McastRoute mRoute = new McastRoute(IpAddress.valueOf(sAddr),
                IpAddress.valueOf(gAddr), McastRoute.Type.STATIC);

        if (egressList == null) {
            mcastRouteManager.remove(mRoute);
            print(D_FORMAT_MAPPING, mRoute.type(), mRoute.group(), mRoute.source());
        } else {
            // check list for validity before we begin to delete.
            for (String egress : egressList) {
                ConnectPoint eCp = ConnectPoint.deviceConnectPoint(egress);
                mcastRouteManager.removeSink(mRoute, eCp);
            }
            print(U_FORMAT_MAPPING, mRoute.type(), mRoute.group(), mRoute.source());
        }
    }
}
