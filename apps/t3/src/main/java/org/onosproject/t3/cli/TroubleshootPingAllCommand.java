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
import org.onosproject.t3.impl.Generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.lang.Thread.sleep;
import static org.onlab.packet.EthType.EtherType;

/**
 * Starts a Static Packet Trace for a given input and prints the result.
 */
@Command(scope = "onos", name = "t3-troubleshoot-pingall",
        description = "Traces a ping between all hosts in the system of a given ETH type")
public class TroubleshootPingAllCommand extends AbstractShellCommand {

    private static final String FMT_SHORT =
            "id=%s, mac=%s, locations=%s, vlan=%s, ip(s)=%s";

    @Option(name = "-et", aliases = "--ethType", description = "ETH Type", valueToShowInHelp = "ipv4")
    String ethType = "ipv4";

    @Option(name = "-v", aliases = "--verbose", description = "Outputs trace for each host to host combination")
    private boolean verbosity1 = false;

    @Option(name = "-vv", aliases = "--veryverbose", description = "Outputs details of every trace")
    private boolean verbosity2 = false;

    @Option(name = "-d", aliases = "--delay", description = "delay between host to host trace display")
    private long delay = 0;

    @Override
    protected void execute() {
        TroubleshootService service = get(TroubleshootService.class);

        EtherType type = EtherType.valueOf(ethType.toUpperCase());

        print("Tracing between all %s hosts", ethType);

        if (!type.equals(EtherType.IPV4) && !type.equals(EtherType.IPV6)) {
            print("Command only support IPv4 or IPv6");
        } else {
            //Create the generator for the list of traces.
            Generator<Set<StaticPacketTrace>> generator = service.pingAllGenerator(type);
            Host previousHost = null;
            int totalTraces = 0;
            List<StaticPacketTrace> failedTraces = new ArrayList<>();
            boolean ipv4 = type.equals(EtherType.IPV4);
            while (generator.iterator().hasNext()) {
                Set<StaticPacketTrace> traces = generator.iterator().next();
                totalTraces++;
                for (StaticPacketTrace trace : traces) {
                    //no verbosity is mininet style output
                    if (!verbosity1 && !verbosity2) {
                        if (trace.getEndpointHosts().isPresent()) {
                            Host src = trace.getEndpointHosts().get().getLeft();
                            if (previousHost == null || !previousHost.equals(src)) {
                                print("%s", StringUtils.rightPad("", 125, '-'));
                                previousHost = printSrc(trace, ipv4, src);
                            }
                            String host = getDstString(trace, ipv4, src);
                            if (!trace.isSuccess()) {
                                host = host + " " + trace.resultMessage();
                                failedTraces.add(trace);
                            }
                            print("%s", host);
                        }
                    } else {
                        print("%s", StringUtils.leftPad("", 125, '-'));

                        if (trace.getInitialPacket() != null) {
                            if (verbosity1) {
                                printResultOnly(trace, ipv4);
                            } else if (verbosity2) {
                                printVerbose(trace);
                            }
                        } else {
                            if (trace.getEndpointHosts().isPresent()) {
                                Host source = trace.getEndpointHosts().get().getLeft();
                                Host destination = trace.getEndpointHosts().get().getRight();
                                print("Source %s --> Destination %s", source.id(), destination.id());
                            }
                            print("Error in obtaining trace: %s", trace.resultMessage());
                        }
                    }
                }
                try {
                    sleep(delay);
                } catch (InterruptedException e) {
                    log.debug("interrupted while sleep");
                }
            }
            print("%s", StringUtils.rightPad("", 125, '-'));
            print("Failed Traces: %s", failedTraces.size());
            print("%s", StringUtils.rightPad("", 125, '-'));
            failedTraces.forEach(t -> {
                if (t.getEndpointHosts().isPresent()) {
                    printSrc(t, ipv4, t.getEndpointHosts().get().getLeft());
                    String dst = getDstString(t, ipv4, t.getEndpointHosts().get().getRight());
                    dst = dst + " " + t.resultMessage();
                    print("%s", dst);
                    print("%s", StringUtils.rightPad("", 125, '-'));
                }
            });
            print("Summary");
            print("Total Traces %s, errors %s", totalTraces, failedTraces.size());
        }
    }

    private String getDstString(StaticPacketTrace trace, boolean ipv4, Host src) {
        String host;
        IpAddress ipAddress = getIpAddress(trace, ipv4, src, false);
        if (ipAddress == null) {
            host = String.format("       %s %s", trace.getEndpointHosts().get().getRight().id(),
                    trace.isSuccess());
        } else {
            host = String.format("       %s (%s) %s",
                    trace.getEndpointHosts().get().getRight().id(), ipAddress,
                    trace.isSuccess());
        }
        return host;
    }

    private Host printSrc(StaticPacketTrace trace, boolean ipv4, Host src) {
        Host previousHost;
        IpAddress ipAddress = getIpAddress(trace, ipv4, src, true);
        if (ipAddress == null) {
            print("%s", src.id() + " -->");
        } else {
            print("%s (%s) -->", src.id(), ipAddress);
        }
        previousHost = src;
        return previousHost;
    }

    private IpAddress getIpAddress(StaticPacketTrace trace, boolean ipv4, Host host, boolean src) {
        IpAddress ipAddress;
        if (ipv4) {
            Criterion.Type type = src ? Criterion.Type.IPV4_SRC : Criterion.Type.IPV4_DST;
            if (trace.getInitialPacket().getCriterion(type) != null) {
                ipAddress = ((IPCriterion) trace.getInitialPacket()
                        .getCriterion(type)).ip().address();
            } else {
                ipAddress = host.ipAddresses().stream().filter(IpAddress::isIp4)
                        .findAny().orElseGet(null);
            }
        } else {
            Criterion.Type type = src ? Criterion.Type.IPV6_SRC : Criterion.Type.IPV6_DST;
            if (trace.getInitialPacket().getCriterion(type) != null) {
                ipAddress = ((IPCriterion) trace.getInitialPacket()
                        .getCriterion(type)).ip().address();
            } else {
                ipAddress = host.ipAddresses().stream().filter(IpAddress::isIp6)
                        .findAny().orElseGet(null);
            }
        }
        return ipAddress;
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
