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
import org.onlab.packet.EthType;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.t3.api.StaticPacketTrace;
import org.onosproject.t3.api.TroubleshootService;
import org.onosproject.t3.impl.Generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Starts a Static Packet Trace for all the multicast routes in the system and prints the result.
 */
@Command(scope = "onos", name = "t3-troubleshoot-mcast",
        description = "Traces all the mcast routes present in the system")
public class TroubleshootMcastCommand extends AbstractShellCommand {


    @Option(name = "-v", aliases = "--verbose", description = "Outputs trace for each mcast route")
    private boolean verbosity1 = false;

    @Option(name = "-vv", aliases = "--veryverbose", description = "Outputs middle level details of every trace")
    private boolean verbosity2 = false;

    @Option(name = "-vvv", aliases = "--veryveryverbose", description = "Outputs complete details of every trace")
    private boolean verbosity3 = false;

    @Option(name = "-vid", aliases = "--vlanId", description = "Vlan of incoming packet", valueToShowInHelp = "None")
    String vlan = "None";


    @Override
    protected void execute() {
        TroubleshootService service = get(TroubleshootService.class);
        print("Tracing all Multicast routes in the System");

        //Create the generator for the list of traces.
        VlanId vlanId = vlan == null || vlan.isEmpty() ? VlanId.NONE : VlanId.vlanId(vlan);
        Generator<Set<StaticPacketTrace>> generator = service.traceMcast(vlanId);
        int totalTraces = 0;
        List<StaticPacketTrace> failedTraces = new ArrayList<>();
        StaticPacketTrace previousTrace = null;
        while (generator.iterator().hasNext()) {
            totalTraces++;
            //Print also Route if possible or packet
            Set<StaticPacketTrace> traces = generator.iterator().next();
            if (!verbosity1 && !verbosity2 && !verbosity3) {
                for (StaticPacketTrace trace : traces) {
                    previousTrace = printTrace(previousTrace, trace);
                    if (!trace.isSuccess()) {
                        print("Failure: %s", trace.resultMessage());
                        failedTraces.add(trace);
                    } else {
                        print("Success");
                    }
                }
            } else {
                traces.forEach(trace -> {
                    print("Tracing packet: %s", trace.getInitialPacket());
                    print("%s", T3CliUtils.printTrace(trace, verbosity2, verbosity3));
                    print("%s", StringUtils.rightPad("", 125, '-'));
                });
            }
        }

        if (!verbosity1 && !verbosity2 && !verbosity3) {
            if (failedTraces.size() != 0) {
                print("%s", StringUtils.rightPad("", 125, '-'));
                print("Failed Traces: %s", failedTraces.size());
            }
            previousTrace = null;
            for (StaticPacketTrace trace : failedTraces) {
                previousTrace = printTrace(previousTrace, trace);
                print("Failure: %s", trace.resultMessage());
            }
            print("%s", StringUtils.rightPad("", 125, '-'));
            print("Summary");
            print("Total Traces %s, errors %s", totalTraces, failedTraces.size());
            print("%s", StringUtils.rightPad("", 125, '-'));
        }

    }

    private StaticPacketTrace printTrace(StaticPacketTrace previousTrace, StaticPacketTrace trace) {
        if (previousTrace == null || !previousTrace.equals(trace)) {
            print("%s", StringUtils.rightPad("", 125, '-'));
            previousTrace = trace;
            ConnectPoint initialConnectPoint = trace.getInitialConnectPoint();
            TrafficSelector initialPacket = trace.getInitialPacket();
            boolean isIPv4 = ((EthTypeCriterion) initialPacket.getCriterion(Criterion.Type.ETH_TYPE))
                    .ethType().equals(EthType.EtherType.IPV4.ethType()
                    );
            IpPrefix group = ((IPCriterion) (isIPv4 ? trace.getInitialPacket()
                    .getCriterion(Criterion.Type.IPV4_DST) : trace.getInitialPacket()
                    .getCriterion(Criterion.Type.IPV6_DST))).ip();
            print("Source %s, group %s", initialConnectPoint, group);
        }
        StringBuilder destinations = new StringBuilder();
        if (trace.getCompletePaths().size() > 1) {
            destinations.append("Sinks: ");
        } else {
            destinations.append("Sink: ");
        }
        trace.getCompletePaths().forEach(path -> {
            destinations.append(path.get(path.size() - 1) + " ");
        });
        print("%s", destinations.toString());
        return previousTrace;
    }
}
