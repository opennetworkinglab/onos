/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.mfwd.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpPrefix;
import org.onosproject.cli.AbstractShellCommand;

import org.onosproject.mfwd.impl.McastRouteBase;
import org.onosproject.mfwd.impl.McastRouteTable;

/**
 * Installs a source, multicast group flow.
 */
@Command(scope = "onos", name = "mcast-join",
         description = "Installs a source, multicast group flow")
public class McastJoinCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "sAddr",
              description = "IP Address of the multicast source. '*' can be used for any source (*, G) entry",
              required = true, multiValued = false)
    String sAddr = null;

    @Argument(index = 1, name = "gAddr",
              description = "IP Address of the multicast group",
              required = true, multiValued = false)
    String gAddr = null;

    @Argument(index = 2, name = "ingressPort",
            description = "Ingress port and Egress ports",
            required = false, multiValued = false)
    String ingressPort = null;

    @Argument(index = 3, name = "ports",
              description = "Ingress port and Egress ports",
              required = false, multiValued = true)
    String[] ports = null;

    @Override
    protected void execute() {
        McastRouteTable mrib = McastRouteTable.getInstance();
        IpPrefix mcast = IpPrefix.valueOf("224.0.0.0/4");
        IpPrefix saddr = IpPrefix.valueOf(sAddr);
        if (mcast.contains(saddr)) {
            print("Error: the source address " + sAddr + " must be an IPv4 unicast address");
            return;
        }

        IpPrefix gaddr = IpPrefix.valueOf(gAddr);
        if (!mcast.contains(gaddr)) {
            print("Error: " + gAddr + " must be a multicast group address");
            return;
        }

        McastRouteBase mr = mrib.addRoute(sAddr, gAddr);
        if (mr == null) {
            print("Error: unable to save the multicast state");
            return;
        }

        // Port format "of:0000000000000023/4"
        if (ingressPort != null) {
            String inCP = ingressPort;
            log.debug("Ingress port provided: " + inCP);
            String [] cp = inCP.split("/");
            mr.addIngressPoint(cp[0], Long.parseLong(cp[1]));
        } else {
            return;
        }

        if (ports == null) {
            return;
        }

        for (int i = 0; i < ports.length; i++) {
            String egCP = ports[i];
            log.debug("Egress port provided: " + egCP);
            String [] cp = egCP.split("/");
            mr.addEgressPoint(cp[0], Long.parseLong(cp[1]));
        }
        print("Added the mcast route");
    }
}
