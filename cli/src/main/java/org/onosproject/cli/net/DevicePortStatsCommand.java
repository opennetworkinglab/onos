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
package org.onosproject.cli.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.onosproject.cli.net.DevicesListCommand.getSortedDevices;
import static org.onosproject.net.DeviceId.deviceId;

/**
 * Lists port statistic of all ports in the system.
 */
@Service
@Command(scope = "onos", name = "portstats",
        description = "Lists statistics of all ports in the system")
public class DevicePortStatsCommand extends AbstractShellCommand {

    @Option(name = "-nz", aliases = "--nonzero", description = "Show only non-zero portstats",
            required = false, multiValued = false)
    private boolean nonzero = false;

    @Option(name = "-d", aliases = "--delta",
            description = "Show delta port statistics,"
                    + "only for the last polling interval",
            required = false, multiValued = false)
    private boolean delta = false;

    @Option(name = "-t", aliases = "--table",
            description = "Show delta port statistics in table format "
                    + "using human readable unit",
            required = false, multiValued = false)
    private boolean table = false;

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = false, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String uri = null;

    @Argument(index = 1, name = "portNumber", description = "Port Number",
            required = false, multiValued = false)
    @Completion(PortNumberCompleter.class)
    String portNumberStr = null;

    PortNumber portNumber = null;

    private static final String FORMAT =
            "   port=%s, pktRx=%s, pktTx=%s, bytesRx=%s, bytesTx=%s, pktRxDrp=%s, pktTxDrp=%s, Dur=%s%s";

    @Override
    protected void doExecute() {
        DeviceService deviceService = get(DeviceService.class);

        if (portNumberStr != null) {
            portNumber = PortNumber.fromString(portNumberStr);
        }

        if (uri == null) {
            if (outputJson()) {
                if (delta) {
                    print("%s", jsonPortStatsDelta(deviceService, getSortedDevices(deviceService)));
                } else {
                    print("%s", jsonPortStats(deviceService, getSortedDevices(deviceService)));
                }
            } else {
                for (Device d : getSortedDevices(deviceService)) {
                    if (delta) {
                        if (table) {
                            printPortStatsDeltaTable(d.id(), deviceService.getPortDeltaStatistics(d.id()));
                        } else {
                            printPortStatsDelta(d.id(), deviceService.getPortDeltaStatistics(d.id()));
                        }
                    } else {
                        printPortStats(d.id(), deviceService.getPortStatistics(d.id()));
                    }
                }
            }
        } else {
            Device d = deviceService.getDevice(deviceId(uri));
            if (d == null) {
                error("No such device %s", uri);
            } else if (outputJson()) {
                if (delta) {
                    print("%s", jsonPortStatsDelta(d.id(), new ObjectMapper(),
                            deviceService.getPortDeltaStatistics(d.id())));
                } else {
                    print("%s", jsonPortStats(d.id(), new ObjectMapper(), deviceService.getPortStatistics(d.id())));
                }
            } else if (delta) {
                if (table) {
                    printPortStatsDeltaTable(d.id(), deviceService.getPortDeltaStatistics(d.id()));
                } else {
                    printPortStatsDelta(d.id(), deviceService.getPortDeltaStatistics(d.id()));
                }
            } else {
                printPortStats(d.id(), deviceService.getPortStatistics(d.id()));
            }
        }
    }

    /**
     * Produces JSON array containing portstats of the specified devices.
     *
     * @param deviceService device service
     * @param devices       collection of devices
     * @return JSON Array
     */
    protected JsonNode jsonPortStats(DeviceService deviceService, Iterable<Device> devices) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        for (Device device : devices) {
            result.add(jsonPortStats(device.id(), mapper, deviceService.getPortStatistics(device.id())));
        }

        return result;
    }

    /**
     * Produces JSON array containing portstats of the specified device.
     *
     * @param deviceId  device id
     * @param portStats collection of port statistics
     * @return JSON array
     */
    private JsonNode jsonPortStats(DeviceId deviceId, ObjectMapper mapper, Iterable<PortStatistics> portStats) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode portStatsNode = mapper.createArrayNode();

        for (PortStatistics stat : sortByPort(portStats)) {
            if (isIrrelevant(stat)) {
                continue;
            }
            if (nonzero && stat.isZero()) {
                continue;
            }

            portStatsNode.add(mapper.createObjectNode()
                    .put("port", stat.portNumber().toString())
                    .put("pktRx", stat.packetsReceived())
                    .put("pktTx", stat.packetsSent())
                    .put("bytesRx", stat.bytesReceived())
                    .put("bytesTx", stat.bytesSent())
                    .put("pktRxDrp", stat.packetsRxDropped())
                    .put("pktTxDrp", stat.packetsTxDropped())
                    .put("Dur", stat.durationSec())
                    .set("annotations", annotations(mapper, stat.annotations())));
        }

        result.put("deviceId", deviceId.toString());
        result.set("portStats", portStatsNode);

        return result;
    }

    /**
     * Produces JSON array containing delta portstats of the specified devices.
     *
     * @param deviceService device service
     * @param devices       collection of devices
     * @return JSON Array
     */
    protected JsonNode jsonPortStatsDelta(DeviceService deviceService, Iterable<Device> devices) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        for (Device device : devices) {
            result.add(jsonPortStatsDelta(device.id(), mapper, deviceService.getPortDeltaStatistics(device.id())));
        }

        return result;
    }

    /**
     * Produces JSON array containing delta portstats of the specified device id.
     *
     * @param deviceId  device id
     * @param portStats collection of port statistics
     * @return JSON array
     */
    private JsonNode jsonPortStatsDelta(DeviceId deviceId, ObjectMapper mapper, Iterable<PortStatistics> portStats) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode portStatsNode = mapper.createArrayNode();

        for (PortStatistics stat : sortByPort(portStats)) {
            if (isIrrelevant(stat)) {
                continue;
            }
            if (nonzero && stat.isZero()) {
                continue;
            }

            float duration = ((float) stat.durationSec()) +
                    (((float) stat.durationNano()) / TimeUnit.SECONDS.toNanos(1));
            float rateRx = stat.bytesReceived() * 8 / duration;
            float rateTx = stat.bytesSent() * 8 / duration;

            portStatsNode.add(mapper.createObjectNode()
                    .put("port", stat.portNumber().toString())
                    .put("pktRx", stat.packetsReceived())
                    .put("pktTx", stat.packetsSent())
                    .put("bytesRx", stat.bytesReceived())
                    .put("bytesTx", stat.bytesSent())
                    .put("rateRx", String.format("%.1f", rateRx))
                    .put("rateTx", String.format("%.1f", rateTx))
                    .put("pktRxDrp", stat.packetsRxDropped())
                    .put("pktTxDrp", stat.packetsTxDropped())
                    .put("interval", String.format("%.3f", duration)));
        }

        result.put("deviceId", deviceId.toString());
        result.set("portStats", portStatsNode);

        return result;
    }


    /**
     * Prints Port Statistics.
     *
     * @param deviceId
     * @param portStats
     */
    private void printPortStats(DeviceId deviceId, Iterable<PortStatistics> portStats) {
        print("deviceId=%s", deviceId);
        for (PortStatistics stat : sortByPort(portStats)) {
            if (isIrrelevant(stat)) {
                continue;
            }
            if (nonzero && stat.isZero()) {
                continue;
            }
            print(FORMAT, stat.portNumber(), stat.packetsReceived(), stat.packetsSent(), stat.bytesReceived(),
                    stat.bytesSent(), stat.packetsRxDropped(), stat.packetsTxDropped(), stat.durationSec(),
                    annotations(stat.annotations()));
        }
    }

    private boolean isIrrelevant(PortStatistics stat) {
        // TODO revisit logical port (e.g., ALL) handling
        return portNumber != null && !portNumber.equals(stat.portNumber());
    }

    /**
     * Prints Port delta statistics.
     *
     * @param deviceId
     * @param portStats
     */
    private void printPortStatsDelta(DeviceId deviceId, Iterable<PortStatistics> portStats) {
        final String formatDelta = "   port=%s, pktRx=%s, pktTx=%s, bytesRx=%s, bytesTx=%s,"
                + " rateRx=%s, rateTx=%s, pktRxDrp=%s, pktTxDrp=%s, interval=%s";
        print("deviceId=%s", deviceId);
        for (PortStatistics stat : sortByPort(portStats)) {
            if (isIrrelevant(stat)) {
                continue;
            }
            if (nonzero && stat.isZero()) {
                continue;
            }
            float duration = ((float) stat.durationSec()) +
                    (((float) stat.durationNano()) / TimeUnit.SECONDS.toNanos(1));
            float rateRx = stat.bytesReceived() * 8 / duration;
            float rateTx = stat.bytesSent() * 8 / duration;
            print(formatDelta, stat.portNumber(),
                    stat.packetsReceived(),
                    stat.packetsSent(),
                    stat.bytesReceived(),
                    stat.bytesSent(),
                    String.format("%.1f", rateRx),
                    String.format("%.1f", rateTx),
                    stat.packetsRxDropped(),
                    stat.packetsTxDropped(),
                    String.format("%.3f", duration));
        }
    }

    /**
     * Prints human readable table with delta Port Statistics for specific device.
     *
     * @param deviceId
     * @param portStats
     */
    private void printPortStatsDeltaTable(DeviceId deviceId, Iterable<PortStatistics> portStats) {
        final String formatDeltaTable = "|%5s | %7s | %7s |  %7s | %7s | %7s | %7s |  %7s | %7s |%9s |";
        print("+---------------------------------------------------------------------------------------------------+");
        print("| DeviceId = %-86s |", deviceId);
        print("|---------------------------------------------------------------------------------------------------|");
        print("|      | Receive                                | Transmit                               | Time [s] |");
        print("| Port | Packets |  Bytes  | Rate bps |   Drop  | Packets |  Bytes  | Rate bps |   Drop  | Interval |");
        print("|---------------------------------------------------------------------------------------------------|");

        for (PortStatistics stat : sortByPort(portStats)) {
            if (isIrrelevant(stat)) {
                continue;
            }
            if (nonzero && stat.isZero()) {
                continue;
            }
            float duration = ((float) stat.durationSec()) +
                    (((float) stat.durationNano()) / TimeUnit.SECONDS.toNanos(1));
            float rateRx = duration > 0 ? stat.bytesReceived() * 8 / duration : 0;
            float rateTx = duration > 0 ? stat.bytesSent() * 8 / duration : 0;
            print(formatDeltaTable, stat.portNumber(),
                    humanReadable(stat.packetsReceived()),
                    humanReadable(stat.bytesReceived()),
                    humanReadableBps(rateRx),
                    humanReadable(stat.packetsRxDropped()),
                    humanReadable(stat.packetsSent()),
                    humanReadable(stat.bytesSent()),
                    humanReadableBps(rateTx),
                    humanReadable(stat.packetsTxDropped()),
                    String.format("%.3f", duration));
        }
        print("+---------------------------------------------------------------------------------------------------+");
    }

    /**
     * Converts bytes to human readable string with Kilo, Mega, Giga, etc.
     *
     * @param bytes input byte array
     * @return human readble string
     */
    public static String humanReadable(long bytes) {
        int unit = 1000;
        if (bytes < unit) {
            return String.format("%s ", bytes);
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        Character pre = ("KMGTPE").charAt(exp - 1);
        return String.format("%.2f%s", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * Converts bps to human readable format.
     *
     * @param bps input rate
     * @return human readble string
     */
    public static String humanReadableBps(float bps) {
        int unit = 1000;
        if (bps < unit) {
            return String.format("%.0f ", bps);
        }
        int exp = (int) (Math.log(bps) / Math.log(unit));
        Character pre = ("KMGTPE").charAt(exp - 1);
        return String.format("%.2f%s", bps / Math.pow(unit, exp), pre);
    }

    private static List<PortStatistics> sortByPort(Iterable<PortStatistics> portStats) {
        List<PortStatistics> portStatsList = Lists.newArrayList(portStats);

        portStatsList.sort(Comparator.comparing(ps -> ps.portNumber().toLong()));
        return portStatsList;
    }
}
