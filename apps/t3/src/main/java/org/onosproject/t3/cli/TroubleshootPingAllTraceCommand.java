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
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Host;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.t3.api.StaticPacketTrace;
import org.onosproject.t3.api.TroubleshootService;

import java.util.List;

import static org.onlab.packet.EthType.EtherType;

/**
 * Starts a Static Packet Trace for a given input and prints the result.
 */
@Command(scope = "onos", name = "t3-troubleshoot-pingall",
        description = "Traces a ping between all hosts in the system of a given ETH type")
public class TroubleshootPingAllTraceCommand extends AbstractShellCommand {

    private static final String FMT_SHORT =
            "id=%s, mac=%s, locations=%s, vlan=%s, ip(s)=%s";

    @Option(name = "-et", aliases = "--ethType", description = "ETH Type", valueToShowInHelp = "ipv4")
    String ethType = "ipv4";

    @Option(name = "-v", aliases = "--verbose", description = "Outputs trace for each host to host combination")
    private boolean verbosity1 = false;

    @Override
    protected void execute() {
        TroubleshootService service = get(TroubleshootService.class);

        EtherType type = EtherType.valueOf(ethType.toUpperCase());

        print("Tracing between all %s hosts", ethType);

        if (!type.equals(EtherType.IPV4) && !type.equals(EtherType.IPV6)) {
            print("Command only support IPv4 or IPv6");
        } else {
            print("%s", StringUtils.leftPad("", 100, '-'));
            //Obtain the list of traces
            List<StaticPacketTrace> traces = service.pingAll(type);

            if (traces.size() == 0) {
                print("No traces were obtained, please check system configuration");
            }
            boolean ipv4 = type.equals(EtherType.IPV4);
            traces.forEach(trace -> {
                if (trace.getInitialPacket() != null) {
                    if (verbosity1) {
                        printVerbose(trace);
                    } else {
                        printResultOnly(trace, ipv4);
                    }
                } else {
                    print("Error in obtaining trace: %s", trace.resultMessage());
                }
                print("%s", StringUtils.leftPad("", 100, '-'));
            });
        }


    }

    private void printResultOnly(StaticPacketTrace trace, boolean ipv4) {
        if (trace.getEndpointHosts().isPresent()) {
            Host source = trace.getEndpointHosts().get().getLeft();
            Host destination = trace.getEndpointHosts().get().getRight();
            IpAddress srcIP;
            IpAddress dstIP;
            if (ipv4 && trace.getInitialPacket().getCriterion(Criterion.Type.IPV4_SRC) != null) {
                srcIP = ((IPCriterion) trace.getInitialPacket().getCriterion(Criterion.Type.IPV4_SRC)).ip().address();
                dstIP = ((IPCriterion) trace.getInitialPacket().getCriterion(Criterion.Type.IPV4_DST)).ip().address();
                print("Source %s (%s) --> Destination %s (%s)", source.id(), srcIP, destination.id(), dstIP);
            } else if (trace.getInitialPacket().getCriterion(Criterion.Type.IPV6_SRC) != null) {
                srcIP = ((IPCriterion) trace.getInitialPacket().getCriterion(Criterion.Type.IPV6_SRC)).ip().address();
                dstIP = ((IPCriterion) trace.getInitialPacket().getCriterion(Criterion.Type.IPV6_DST)).ip().address();
                print("Source %s (%s) --> Destination %s (%s)", source.id(), srcIP, destination.id(), dstIP);
            } else {
                print("Source %s --> Destination %s", source.id(), destination.id());
            }
            print("%s", trace.resultMessage());
        } else {
            print("Can't gather host information from trace");
            print("%s", trace.resultMessage());
        }
    }

    private void printVerbose(StaticPacketTrace trace) {
        if (trace.getEndpointHosts().isPresent()) {
            Host source = trace.getEndpointHosts().get().getLeft();
            print("Source host %s", printHost(source));
            Host destination = trace.getEndpointHosts().get().getRight();
            print("Destination host %s", printHost(destination));
        }
        print("%s", trace.getInitialPacket());
        print("%s", T3CliUtils.printTrace(trace, false, false));
    }

    private String printHost(Host host) {
        return String.format(FMT_SHORT, host.id(), host.mac(),
                host.locations(),
                host.vlan(), host.ipAddresses());
    }
}
