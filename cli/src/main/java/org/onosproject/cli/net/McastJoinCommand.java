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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.MulticastRouteService;

/**
 * Installs a source, multicast group flow.
 */
@Command(scope = "onos", name = "mcast-join",
         description = "Installs a source, multicast group flow")
public class McastJoinCommand extends AbstractShellCommand {

    // Format for group line
    private static final String FORMAT_MAPPING = "Added the mcast route: " +
            "origin=%s, group=%s, source=%s";

    @Argument(index = 0, name = "sAddr",
              description = "IP Address of the multicast source. '*' can be used for any source (*, G) entry",
              required = true, multiValued = false)
    String sAddr = null;

    @Argument(index = 1, name = "gAddr",
              description = "IP Address of the multicast group",
              required = true, multiValued = false)
    String gAddr = null;

    @Argument(index = 2, name = "ingressPort",
            description = "Ingress port of:XXXXXXXXXX/XX",
            required = false, multiValued = false)
    String ingressPort = null;

    @Argument(index = 3, name = "ports",
              description = "Egress ports of:XXXXXXXXXX/XX...",
              required = false, multiValued = true)
    String[] ports = null;

    @Override
    protected void execute() {
        MulticastRouteService mcastRouteManager = get(MulticastRouteService.class);

        McastRoute mRoute = new McastRoute(IpAddress.valueOf(sAddr),
                IpAddress.valueOf(gAddr), McastRoute.Type.STATIC);
        mcastRouteManager.add(mRoute);

        if (ingressPort != null) {
            ConnectPoint ingress = ConnectPoint.deviceConnectPoint(ingressPort);
            mcastRouteManager.addSource(mRoute, ingress);
        }

        if (ports != null) {
            for (String egCP : ports) {
                ConnectPoint egress = ConnectPoint.deviceConnectPoint(egCP);
                mcastRouteManager.addSink(mRoute, egress);

            }
        }
        printMcastRoute(mRoute);
    }

    private void printMcastRoute(McastRoute mcastRoute) {
        print(FORMAT_MAPPING, mcastRoute.type(), mcastRoute.group(), mcastRoute.source());
    }
}
