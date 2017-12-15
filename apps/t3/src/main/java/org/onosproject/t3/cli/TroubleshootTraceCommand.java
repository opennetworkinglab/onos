/*
 * Copyright 2015-present Open Networking Foundation
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

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.t3.api.StaticPacketTrace;
import org.onosproject.t3.api.TroubleshootService;

import java.util.List;

/**
 * Starts a Static Packet Trace for a given input and prints the result.
 */
@Command(scope = "onos", name = "troubleshoot",
        description = "troubleshoots flows and groups between source and destination")
public class TroubleshootTraceCommand extends AbstractShellCommand {


    private static final String FLOW_SHORT_FORMAT = "    %s, bytes=%s, packets=%s, "
            + "table=%s, priority=%s, selector=%s, treatment=%s";

    private static final String GROUP_FORMAT =
            "   id=0x%s, state=%s, type=%s, bytes=%s, packets=%s, appId=%s, referenceCount=%s";
    private static final String GROUP_BUCKET_FORMAT =
            "       id=0x%s, bucket=%s, bytes=%s, packets=%s, actions=%s";

    @Option(name = "-v", aliases = "--verbose", description = "Outputs complete path")
    private boolean verbosity1 = false;

    @Option(name = "-vv", aliases = "--veryverbose", description = "Outputs flows and groups for every device")
    private boolean verbosity2 = false;

    @Option(name = "-s", aliases = "--srcIp", description = "Source IP")
    String srcIp = null;

    @Option(name = "-sp", aliases = "--srcPort", description = "Source Port", required = true)
    String srcPort = null;

    @Option(name = "-sm", aliases = "--srcMac", description = "Source MAC")
    String srcMac = null;

    @Option(name = "-et", aliases = "--ethType", description = "ETH Type", valueToShowInHelp = "ipv4")
    String ethType = "ipv4";

    @Option(name = "-stp", aliases = "--srcTcpPort", description = "Source TCP Port")
    String srcTcpPort = null;

    @Option(name = "-d", aliases = "--dstIp", description = "Destination IP", valueToShowInHelp = "255.255.255.255")
    String dstIp = "255.255.255.255";

    @Option(name = "-dm", aliases = "--dstMac", description = "Destination MAC")
    String dstMac = null;

    @Option(name = "-dtp", aliases = "dstTcpPort", description = "destination TCP Port")
    String dstTcpPort = null;

    @Option(name = "-vid", aliases = "--vlanId", description = "Vlan of incoming packet", valueToShowInHelp = "None")
    String vlan = "None";

    @Option(name = "-mb", aliases = "--mplsBos", description = "MPLS BOS", valueToShowInHelp = "True")
    String mplsBos = "true";

    @Override
    protected void execute() {
        TroubleshootService service = get(TroubleshootService.class);
        ConnectPoint cp = ConnectPoint.deviceConnectPoint(srcPort);

        //Input Port must be specified
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder()
                .matchInPort(cp.port());

        if (srcIp != null) {
            selectorBuilder.matchIPSrc(IpAddress.valueOf(srcIp).toIpPrefix());
        }

        if (srcMac != null) {
            selectorBuilder.matchEthSrc(MacAddress.valueOf(srcMac));
        }

        //if EthType option is not specified using IPv4
        selectorBuilder.matchEthType(EthType.EtherType.valueOf(ethType.toUpperCase()).ethType().toShort());

        if (srcTcpPort != null) {
            selectorBuilder.matchTcpSrc(TpPort.tpPort(Integer.parseInt(srcTcpPort)));
        }

        //if destination Ip option is not specified using broadcast 255.255.255.255
        selectorBuilder.matchIPDst(IpAddress.valueOf(dstIp).toIpPrefix());

        if (dstMac != null) {
            selectorBuilder.matchEthDst(MacAddress.valueOf(dstMac));
        }
        if (dstTcpPort != null) {
            selectorBuilder.matchTcpDst(TpPort.tpPort(Integer.parseInt(dstTcpPort)));
        }

        //if vlan option is not specified using NONE
        selectorBuilder.matchVlanId(VlanId.vlanId(vlan));

        //if mplsBos option is not specified using True
        selectorBuilder.matchMplsBos(Boolean.valueOf(mplsBos));

        TrafficSelector packet = selectorBuilder.build();

        //Printing the created packet
        print("Tracing packet: %s", packet.criteria());

        //Build the trace
        StaticPacketTrace trace = service.trace(packet, cp);

        //Print based on verbosity
        if (verbosity1) {
            printTrace(trace, false);
        } else if (verbosity2) {
            printTrace(trace, true);
        } else {
            print("Paths");
            List<List<ConnectPoint>> paths = trace.getCompletePaths();
            paths.forEach(path -> print("%s", path));
        }
        print("Result: \n%s", trace.resultMessage());
    }

    //prints the trace
    private void printTrace(StaticPacketTrace trace, boolean verbose) {
        List<List<ConnectPoint>> paths = trace.getCompletePaths();
        paths.forEach(path -> {
            print("Path %s", path);
            ConnectPoint previous = null;
            for (ConnectPoint connectPoint : path) {
                if (previous == null || !previous.deviceId().equals(connectPoint.deviceId())) {
                    print("Device %s", connectPoint.deviceId());
                    print("Input from %s", connectPoint);
                    printFlows(trace, verbose, connectPoint);
                } else {
                    printGroups(trace, verbose, connectPoint);
                    print("Output through %s", connectPoint);
                    print("");
                }
                previous = connectPoint;
            }
        });
    }

    //Prints the flows for a given trace and a specified level of verbosity
    private void printFlows(StaticPacketTrace trace, boolean verbose, ConnectPoint connectPoint) {
        print("Flows");
        trace.getFlowsForDevice(connectPoint.deviceId()).forEach(f -> {
            if (verbose) {
                print(FLOW_SHORT_FORMAT, f.state(), f.bytes(), f.packets(),
                        f.table(), f.priority(), f.selector().criteria(),
                        printTreatment(f.treatment()));
            } else {
                print("   flowId=%s, selector=%s ", f.id(), f.selector().criteria());
            }
        });
    }

    //Prints the groups for a given trace and a specified level of verbosity
    private void printGroups(StaticPacketTrace trace, boolean verbose, ConnectPoint connectPoint) {
        print("Groups");
        trace.getGroupOuputs(connectPoint.deviceId()).forEach(output -> {
            if (output.getOutput().equals(connectPoint)) {
                output.getGroups().forEach(group -> {
                    if (verbose) {
                        print(GROUP_FORMAT, Integer.toHexString(group.id().id()), group.state(), group.type(),
                                group.bytes(), group.packets(), group.appId().name(), group.referenceCount());
                        int i = 0;
                        for (GroupBucket bucket : group.buckets().buckets()) {
                            print(GROUP_BUCKET_FORMAT, Integer.toHexString(group.id().id()), ++i,
                                    bucket.bytes(), bucket.packets(),
                                    bucket.treatment().allInstructions());
                        }
                    } else {
                        print("   groupId=%s", group.id());
                    }
                });
                print("Outgoing Packet %s", output.getFinalPacket());
            }
        });
    }

    private String printTreatment(TrafficTreatment treatment) {
        final String delimiter = ", ";
        StringBuilder builder = new StringBuilder("[");
        if (!treatment.immediate().isEmpty()) {
            builder.append("immediate=" + treatment.immediate() + delimiter);
        }
        if (!treatment.deferred().isEmpty()) {
            builder.append("deferred=" + treatment.deferred() + delimiter);
        }
        if (treatment.clearedDeferred()) {
            builder.append("clearDeferred" + delimiter);
        }
        if (treatment.tableTransition() != null) {
            builder.append("transition=" + treatment.tableTransition() + delimiter);
        }
        if (treatment.metered() != null) {
            builder.append("meter=" + treatment.metered() + delimiter);
        }
        if (treatment.writeMetadata() != null) {
            builder.append("metadata=" + treatment.writeMetadata() + delimiter);
        }
        // Chop off last delimiter
        builder.replace(builder.length() - delimiter.length(), builder.length(), "");
        builder.append("]");
        return builder.toString();
    }
}
