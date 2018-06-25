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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.HostIdCompleter;
import org.onosproject.net.HostId;
import org.onosproject.t3.api.StaticPacketTrace;
import org.onosproject.t3.api.TroubleshootService;

import java.util.Set;

import static org.onlab.packet.EthType.EtherType;

/**
 * Starts a Static Packet Trace for a given input and prints the result.
 */
@Command(scope = "onos", name = "t3-troubleshoot-simple",
        description = "Given two hosts troubleshoots flows and groups between them, in case of segment routing")
public class TroubleshootSimpleTraceCommand extends AbstractShellCommand {

    // OSGi workaround to introduce package dependency
    HostIdCompleter completer;
    @Argument(index = 0, name = "one", description = "One host ID",
            required = true, multiValued = false)
    String srcHost = null;

    @Argument(index = 1, name = "two", description = "Another host ID",
            required = true, multiValued = false)
    String dstHost = null;

    @Option(name = "-v", aliases = "--verbose", description = "Outputs complete path")
    private boolean verbosity1 = false;

    @Option(name = "-vv", aliases = "--veryverbose", description = "Outputs flows and groups for every device")
    private boolean verbosity2 = false;

    @Option(name = "-et", aliases = "--ethType", description = "ETH Type", valueToShowInHelp = "ipv4")
    String ethType = "ipv4";

    @Override
    protected void execute() {
        TroubleshootService service = get(TroubleshootService.class);

        EtherType type = EtherType.valueOf(ethType.toUpperCase());

        //Printing the traced hosts
        print("Tracing between: %s and %s", srcHost, dstHost);

        //Build the traces
        Set<StaticPacketTrace> traces = service.trace(HostId.hostId(srcHost), HostId.hostId(dstHost), type);
        traces.forEach(trace -> {
            if (trace.getInitialPacket() != null) {
                print("Tracing Packet: %s", trace.getInitialPacket());
                print("%s", T3CliUtils.printTrace(trace, verbosity1, verbosity2));
            } else {
                print("Cannot obtain trace between %s and %s", srcHost, dstHost);
                print("Reason: %s", trace.resultMessage());
            }
        });


    }
}
