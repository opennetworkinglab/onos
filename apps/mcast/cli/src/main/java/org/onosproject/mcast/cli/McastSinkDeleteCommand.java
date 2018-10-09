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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.HostIdCompleter;
import org.onosproject.mcast.api.McastRoute;
import org.onosproject.mcast.api.MulticastRouteService;
import org.onosproject.net.HostId;

/**
 * Deletes a multicast route.
 */
@Service
@Command(scope = "onos", name = "mcast-sink-delete",
        description = "Delete a sink from multicast route flow. If no sin is specified removes the whole route.")
public class McastSinkDeleteCommand extends AbstractShellCommand {

    // Delete format for group line
    private static final String D_FORMAT_MAPPING = "Deleted the mcast route: " +
            "origin=%s, group=%s, source=%s";

    // Update format for group line
    private static final String U_FORMAT_MAPPING = "Updated the mcast route: " +
            "origin=%s, group=%s, source=%s";

    @Option(name = "-sAddr", aliases = "--sourceAddress",
            description = "IP Address of the multicast source. '*' can be used for any source (*, G) entry",
            valueToShowInHelp = "1.1.1.1",
            required = true, multiValued = false)
    String sAddr = null;

    @Option(name = "-gAddr", aliases = "--groupAddress",
            description = "IP Address of the multicast group",
            valueToShowInHelp = "224.0.0.0",
            required = true, multiValued = false)
    @Completion(McastGroupCompleter.class)
    String gAddr = null;

    @Option(name = "-s", aliases = "--sinks",
            description = "Host sink format: MAC/VLAN",
            valueToShowInHelp = "00:00:00:00:00:00/None")
    @Completion(HostIdCompleter.class)
    String host = null;

    @Override
    protected void doExecute() {
        MulticastRouteService mcastRouteManager = get(MulticastRouteService.class);
        // Clear all routes
        if ("*".equals(sAddr) && "*".equals(gAddr)) {
            mcastRouteManager.getRoutes().forEach(mcastRouteManager::remove);
            return;
        }
        // Removing/updating a specific entry
        IpAddress sAddrIp = null;
        //If the source Ip is * we want ASM so we leave it as null and the route will have it as an optional.empty()
        if (!sAddr.equals("*")) {
            sAddrIp = IpAddress.valueOf(sAddr);
        }
        McastRoute mRoute = new McastRoute(sAddrIp, IpAddress.valueOf(gAddr),
                McastRoute.Type.STATIC);
        // If the user provides only sAddr and gAddr, we have to remove the route
        if (host == null || host.isEmpty()) {
            mcastRouteManager.remove(mRoute);
            printMcastRoute(D_FORMAT_MAPPING, mRoute);
            return;
        }
        // Otherwise we need to remove a specific sink
        HostId hostId = HostId.hostId(host);
        if (!mcastRouteManager.getRoutes().contains(mRoute)) {
            print("Route is not present, store it first");
            return;
        }
        // Remove the entire host id
        mcastRouteManager.removeSink(mRoute, hostId);

        // We have done
        printMcastRoute(U_FORMAT_MAPPING, mRoute);
    }

    private void printMcastRoute(String format, McastRoute mcastRoute) {
        // If the source is present let's use it, otherwise we need to print *
        print(format, mcastRoute.type(), mcastRoute.group(),
                mcastRoute.source().isPresent() ? mcastRoute.source().get() : "*");
    }
}
