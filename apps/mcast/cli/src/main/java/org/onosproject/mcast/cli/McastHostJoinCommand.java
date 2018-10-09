/*
 * Copyright 2018-present Open Networking Foundation
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
 * Installs a source, multicast group flow.
 */
@Service
@Command(scope = "onos", name = "mcast-host-join",
        description = "Installs a source, multicast group flow")
public class McastHostJoinCommand extends AbstractShellCommand {

    // Format for group line
    private static final String FORMAT_MAPPING = "Added the mcast route: " +
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

    @Option(name = "-srcs", aliases = "--sources",
            description = "Host sink format: MAC/VLAN",
            valueToShowInHelp = "00:00:00:00:00:00/None",
            multiValued = true)
    @Completion(HostIdCompleter.class)
    String[] sources = null;

    @Option(name = "-sinks",
            aliases = "--hostsinks",
            description = "Host sink format: MAC/VLAN",
            valueToShowInHelp = "00:00:00:00:00:00/None",
            multiValued = true)
    @Completion(HostIdCompleter.class)
    String[] sinks = null;

    @Override
    protected void doExecute() {
        MulticastRouteService mcastRouteManager = get(MulticastRouteService.class);

        IpAddress sAddrIp = null;
        //If the source Ip is * we want ASM so we leave it as null and the route will have it as an optional.empty()
        if (!sAddr.equals("*")) {
            sAddrIp = IpAddress.valueOf(sAddr);
        }

        McastRoute mRoute = new McastRoute(sAddrIp, IpAddress.valueOf(gAddr), McastRoute.Type.STATIC);
        mcastRouteManager.add(mRoute);

        if (sources != null) {
            for (String hostId : sources) {
                mcastRouteManager.addSource(mRoute, HostId.hostId(hostId));
            }
        }

        if (sinks != null) {
            for (String hostId : sinks) {
                mcastRouteManager.addSink(mRoute, HostId.hostId(hostId));
            }
        }
        printMcastRoute(mRoute);
    }

    private void printMcastRoute(McastRoute mcastRoute) {
        // If the source is present let's use it, otherwise we need to print *
        print(FORMAT_MAPPING, mcastRoute.type(), mcastRoute.group(),
                mcastRoute.source().isPresent() ? mcastRoute.source().get() : "*");
    }

}
