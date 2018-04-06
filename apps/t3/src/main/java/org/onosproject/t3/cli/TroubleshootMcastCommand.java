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

package org.onosproject.t3.cli;

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.t3.api.StaticPacketTrace;
import org.onosproject.t3.api.TroubleshootService;
import org.onosproject.t3.impl.Generator;

import java.util.Set;

/**
 * Starts a Static Packet Trace for all the multicast routes in the system and prints the result.
 */
@Command(scope = "onos", name = "t3-troubleshoot-mcast",
        description = "Traces all the mcast routes present in the system")
public class TroubleshootMcastCommand extends AbstractShellCommand {


    @Option(name = "-v", aliases = "--verbose", description = "Outputs trace for each host to host combination")
    private boolean verbosity1 = false;

    @Option(name = "-vv", aliases = "--veryverbose", description = "Outputs details of every trace")
    private boolean verbosity2 = false;

    @Option(name = "-vid", aliases = "--vlanId", description = "Vlan of incoming packet", valueToShowInHelp = "None")
    String vlan = "None";


    @Override
    protected void execute() {
        TroubleshootService service = get(TroubleshootService.class);
        print("Tracing all Multicast routes in the System");
        print("%s", StringUtils.rightPad("", 125, '-'));

            //Create the generator for the list of traces.
        VlanId vlanId = vlan == null || vlan.isEmpty() ? VlanId.NONE : VlanId.vlanId(vlan);
        Generator<Set<StaticPacketTrace>> generator = service.traceMcast(vlanId);
        while (generator.iterator().hasNext()) {
            //Print also Route if possible or packet
            Set<StaticPacketTrace> traces = generator.iterator().next();
            traces.forEach(trace -> {
                print("Tracing packet: %s", trace.getInitialPacket());
                print("%s", T3CliUtils.printTrace(trace, verbosity1, verbosity2));
                print("%s", StringUtils.rightPad("", 125, '-'));
            });
        }

    }
}
