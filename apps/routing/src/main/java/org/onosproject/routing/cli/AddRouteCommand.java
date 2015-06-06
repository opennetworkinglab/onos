/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.routing.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.routing.FibEntry;
import org.onosproject.routing.FibListener;
import org.onosproject.routing.FibUpdate;
import org.onosproject.routing.StaticRoutingService;

import java.util.Arrays;
import java.util.Collections;

@Command(scope = "onos", name = "add-route", description = "Installs static route")
public class AddRouteCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "prefix IP MAC",
            description = "prefix nexthopIP nexthopMAC",
            required = true, multiValued = true)
    String[] fibEntryString = null;

    @Override
    protected void execute() {
        StaticRoutingService routingService = get(StaticRoutingService.class);

        if (fibEntryString.length < 3) {
            return;
        }

        IpPrefix prefix = IpPrefix.valueOf(fibEntryString[0]);
        IpAddress nextHopIp = IpAddress.valueOf(fibEntryString[1]);
        MacAddress nextHopMac = MacAddress.valueOf(fibEntryString[2]);
        FibEntry fibEntry = new FibEntry(prefix, nextHopIp, nextHopMac);
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.UPDATE, fibEntry);

        FibListener fibListener = routingService.getFibListener();
        fibListener.update(Arrays.asList(fibUpdate), Collections.emptyList());
    }
}
