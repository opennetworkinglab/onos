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

package org.onosproject.incubator.net.dpi.impl;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.dpi.DpiStatInfo;
import org.onosproject.incubator.net.dpi.DpiStatistics;
import org.onosproject.incubator.net.dpi.DpiStatisticsManagerService;
import org.onosproject.incubator.net.dpi.FlowStatInfo;
import org.onosproject.incubator.net.dpi.ProtocolStatInfo;
import org.onosproject.incubator.net.dpi.TrafficStatInfo;

import java.util.List;

import static java.lang.Thread.sleep;

/**
 * Fetches DPI statistics list.
 */
@Service
@Command(scope = "onos", name = "dpis",
        description = "Fetches the DPI result entries that is received from DPI engine server")
public class DpisListCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "receivedTime", description = "received time: format 'yyyy-MM-dd HH:mm:ss', "
            + "ex:'2016-08-30 10:31:20', default = null(latest time)",
            required = false, multiValued = false)
    String receivedTime = null; // default is latest time

    @Option(name = "-l", aliases = "--latest",
            description = "Show the latest dpi stats result entry",
            required = false, multiValued = false)
    boolean latest = true; // default

    @Option(name = "-d", aliases = "--detectedProtocols",
            description = "Show the detected protocols only for each statistic entry",
            required = false, multiValued = false)
    boolean dProtocols = false; // default

    @Option(name = "-k", aliases = "--knownFlows",
            description = "Show the known flows only for each statistic entry",
            required = false, multiValued = false)
    boolean kFlows = false; // default

    @Option(name = "-u", aliases = "--unknownFlows",
            description = "Show the unknown flows only for each statistic entry",
            required = false, multiValued = false)
    boolean uFlows = false; // default

    @Option(name = "-a", aliases = "--all",
            description = "Show the all statistics information in detail for each statistic entry",
            required = false, multiValued = false)
    boolean all = false; // default is traffic statistics only display

    @Option(name = "-p", aliases = "--permanent",
            description = "Show the latest dpi stats result entry permanently at 5 second, use Ctrl+C for quitting",
            required = false, multiValued = false)
    boolean permanent = false;

    @Option(name = "-n", aliases = "--lastn",
            description = "Show the last N Dpi stats result entries, MAX_REQUEST_ENTRY = 100",
            required = false, multiValued = false)
    String lastn = null;

    @Option(name = "-P", aliases = "--topnProtocols",
            description = "Show the topn detected Protocol result entries, MAX_PROTOCOLS_TOPN = 100",
            required = false, multiValued = false)
    String topnProtocols = null;

    @Option(name = "-F", aliases = "--topnFlows",
            description = "Show the topn known and unknown Flows result entries, MAX_FLOWS_TOPN = 100",
            required = false, multiValued = false)
    String topnFlows = null;

    private static final int DEFAULT_LASTN = 100;
    private static final int DEFAULT_TOPNP = -1;
    private static final int DEFAULT_TOPNF = -1;
    private static final String NO_DPI_ENTRY_ERROR_MSG = "No DPI statistic entry,"
                                                        + " please check remote DPI engine is running";
    private static final String RECEIVED_TIME_ERROR_MSG = NO_DPI_ENTRY_ERROR_MSG + "\n"
                    + " or correct receivedTime format: 'yyyy-MM-dd HH:mm:ss', ex:'2016-08-30 10:31:20'";

    @Override
    protected void doExecute() {
        DpiStatisticsManagerService dsms = get(DpiStatisticsManagerService.class);

        DpiStatistics ds;

        int topnP = DEFAULT_TOPNP;
        int topnF = DEFAULT_TOPNF;

        if (topnProtocols != null) {
            topnP = parseIntWithDefault(topnProtocols, DEFAULT_TOPNP);
            if (topnP <= 0) {
                print("Invalid detected protocol topn number: 0 < valid number <= 100");
                return;
            }
        }

        if (topnFlows != null) {
            topnF = parseIntWithDefault(topnFlows, DEFAULT_TOPNF);
            if (topnF <= 0) {
                print("Invalid known or unknown flows topn number: 0 < valid number <= 100");
                return;
            }
        }

        boolean isTopn = (topnP > 0 || topnF > 0);

        if (all) {
            dProtocols = true;
            kFlows = true;
            uFlows = true;
        }

        if (receivedTime != null) {
            if (isTopn) {
                ds = dsms.getDpiStatistics(receivedTime, topnP, topnF);
            } else {
                ds = dsms.getDpiStatistics(receivedTime);
            }
            if (ds == null) {
                print(RECEIVED_TIME_ERROR_MSG);
                return;
            }

            printDpiStatistics(0, ds);
        } else if (lastn != null) {
            int lastN = parseIntWithDefault(lastn, DEFAULT_LASTN);

            List<DpiStatistics> dsList;
            if (isTopn) {
                dsList = dsms.getDpiStatistics(lastN, topnP, topnF);

            } else {
                dsList = dsms.getDpiStatistics(lastN);
            }

            printDpiStatisticsList(dsList);
        } else if (permanent) {
            int i = 0;
            while (true) {
                try {
                    if (isTopn) {
                        ds = dsms.getDpiStatisticsLatest(topnP, topnF);
                    } else {
                        ds = dsms.getDpiStatisticsLatest();
                    }
                    if (ds == null) {
                        print(NO_DPI_ENTRY_ERROR_MSG);
                        return;
                    }

                    printDpiStatistics(i++, ds);
                    sleep(5000);
                } catch (Exception e) {
                    return;
                }
            }
        } else { // latest == true
            if (isTopn) {
                ds = dsms.getDpiStatisticsLatest(topnP, topnF);
            } else {
                ds = dsms.getDpiStatisticsLatest();
            }
            if (ds == null) {
                print(NO_DPI_ENTRY_ERROR_MSG);
                return;
            }

            printDpiStatistics(0, ds);
        }
    }


    /**
     * Parse unsigned integer from input lastn string.
     *
     * @param lastN string lastn number
     * @param defaultN integer default lastn number = 100
     * @return integer lastN number, defaultN if input format is not a number
     */
    private int parseIntWithDefault(String lastN, int defaultN) {
        try {
            lastN = lastN.trim();
            return Integer.parseUnsignedInt(lastN);
        } catch (NumberFormatException e) {
            return defaultN;
        }
    }

    private void printDpiStatistics(int number, DpiStatistics ds) {
        if (outputJson()) {
            printDpiStatisticsJson(number, ds);
        } else {
            printDpiStatisticsClass(number, ds);
        }
    }

    private void printDpiStatisticsJson(int number, DpiStatistics ds) {
        String index = number < 0 ? "  -  " : String.format("%5d", number);
        if ("".equals(ds.receivedTime())) {
            print("ReceivedTime is null, No valid DPI Statistics!");
            return;
        }

        print("<--- (%s) DPI Statistics Time [%s] --->", index, ds.receivedTime());
        print("      %s", ds.toString());
        print("<--------------------------------------------------------->");
    }

    private void printDpiStatisticsClass(int number, DpiStatistics ds) {
        String printLine = "";
        String index = number < 0 ? "  -  " : String.format("%5d", number);

        DpiStatInfo dsi = ds.dpiStatInfo();
        if (dsi == null) {
            return;
        }

        if ("".equals(ds.receivedTime())) {
            print("ReceivedTime is null, No valid DPI Statistics!");
            return;
        }

        print("<--- (%s) DPI Statistics Time [%s] --->", index, ds.receivedTime());

        print("Traffic Statistics:");
        TrafficStatInfo tsi = dsi.trafficStatistics();

        printLine = String.format("        %-30s %-30s", "ethernet.bytes:" + ":", tsi.ethernetBytes());
        print("%s", printLine);
        printLine = String.format("        %-30s %-30s", "discarded.bytes" + ":", tsi.discardedBytes());
        print("%s", printLine);
        printLine = String.format("        %-30s %-30s", "ip.packets" + ":", tsi.ipPackets());
        print("%s", printLine);
        printLine = String.format("        %-30s %-30s", "total.packets" + ":", tsi.totalPackets());
        print("%s", printLine);
        printLine = String.format("        %-30s %-30s", "ip.bytes" + ":", tsi.ipBytes());
        print("%s", printLine);
        printLine = String.format("        %-30s %-30s", "avg.pkt.size" + ":", tsi.avgPktSize());
        print("%s", printLine);
        printLine = String.format("        %-30s %-30s", "unique.flows" + ":", tsi.uniqueFlows());
        print("%s", printLine);
        printLine = String.format("        %-30s %-30s", "tcp.packets" + ":", tsi.tcpPackets());
        print("%s", printLine);
        printLine = String.format("        %-30s %-30s", "udp.packets" + ":", tsi.tcpPackets());
        print("%s", printLine);
        printLine = String.format("        %-30s %-30s", "dpi.throughput.pps" + ":",
                                  tsi.dpiThroughputPps() + " pps");
        print("%s", printLine);
        printLine = String.format("        %-30s %-30s", "dpi.throughput.bps" + ":",
                                  tsi.dpiThroughputBps() + " bps");
        print("%s", printLine);
        printLine = String.format("        %-30s %-30s", "traffic.throughput.pps" + ":",
                                  tsi.trafficThroughputPps() + " pps");
        print("%s", printLine);
        printLine = String.format("        %-30s %-30s", "traffic.throughput.bps" + ":",
                                  tsi.trafficThroughputBps() + " bps");
        print("%s", printLine);
        printLine = String.format("        %-30s %-30s", "traffic.duration.sec" + ":",
                                  tsi.trafficDurationSec() + " sec");
        print("%s", printLine);
        printLine = String.format("        %-30s %-30s", "guessed.flow.protos" + ":", tsi.guessedFlowProtos());
        print("%s", printLine);

        if (dProtocols || topnProtocols != null) {
            print("");
            print("Detected Protocols:");
            List<ProtocolStatInfo> psiList = dsi.detectedProtos();
            if (psiList != null) {
                psiList.forEach(psi -> print(makeProtocolString(psi)));
            }
        }

        List<FlowStatInfo> fsiList;
        if (kFlows || topnFlows != null) {
            print("");
            print("Known Flows:");
            fsiList = dsi.knownFlows();
            if (fsiList != null) {
                for (int i = 0; i < fsiList.size(); i++) {
                    print(makeFlowString(fsiList.get(i), i));
                }
            }
        }

        if (uFlows || topnFlows != null) {
            print("");
            print("Unknown Flows:");
            fsiList = dsi.unknownFlows();
            if (fsiList != null) {
                for (int i = 0; i < fsiList.size(); i++) {
                    print(makeFlowString(fsiList.get(i), i));
                }
            }
        }

        print("<--------------------------------------------------------->");
    }

    private void printDpiStatisticsList(List<DpiStatistics> dsList) {
        if (outputJson()) {
            printDpiStatisticsListJson(dsList);
        } else {
            printDpiStatisticsListClass(dsList);
        }
    }

    private void printDpiStatisticsListJson(List<DpiStatistics> dsList) {
        for (int i = 0; i < dsList.size(); i++) {
            printDpiStatisticsJson(i, dsList.get(i));
        }
    }

    private void printDpiStatisticsListClass(List<DpiStatistics> dsList) {
        for (int i = 0; i < dsList.size(); i++) {
            printDpiStatisticsClass(i, dsList.get(i));
        }
    }

    private String makeProtocolString(ProtocolStatInfo psi) {
        StringBuffer sb = new StringBuffer("        ");

        sb.append(String.format("%-20s", psi.name()));
        sb.append(String.format(" %s: %-20s", "packets", psi.packets()));
        sb.append(String.format(" %s: %-20s", "bytes", psi.bytes()));
        sb.append(String.format(" %s: %-20s", "flows", psi.flows()));

        return sb.toString();
    }

    private String makeFlowString(FlowStatInfo fsi, int index) {
        StringBuffer sb = new StringBuffer("        ");

        sb.append(String.format("%-8d ", index));
        sb.append(String.format("%s ", fsi.protocol()));
        sb.append(String.format("%s", fsi.hostAName()));
        sb.append(String.format(":%s", fsi.hostAPort()));
        sb.append(String.format(" <-> %s", fsi.hostBName()));
        sb.append(String.format(":%s", fsi.hostBPort()));
        sb.append(String.format(" [proto: %d", fsi.detectedProtocol()));
        sb.append(String.format("/%s]", fsi.detectedProtocolName()));
        sb.append(String.format(" [%s pkts/", fsi.packets()));
        sb.append(String.format("%s bytes]", fsi.bytes()));
        String serverHostName = fsi.hostServerName();
        if (serverHostName != null && !"".equals(serverHostName)) {
            sb.append(String.format("[Host: %s]", serverHostName));
        }

        return sb.toString();
    }
}
