/*
 * Copyright 2015-present Open Networking Laboratory
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

import static org.onosproject.cli.net.DevicesListCommand.getSortedDevices;
import static org.onosproject.net.DeviceId.deviceId;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;

/**
 * Lists port statistic of all ports in the system.
 */
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
    String uri = null;

    @Argument(index = 1, name = "portNumber", description = "Port Number",
            required = false, multiValued = false)
    Integer portNumber = null;

    private static final String FORMAT =
            "   port=%s, pktRx=%s, pktTx=%s, bytesRx=%s, bytesTx=%s, pktRxDrp=%s, pktTxDrp=%s, Dur=%s";

    @Override
    protected void execute() {
        DeviceService deviceService = get(DeviceService.class);

        if (uri == null) {
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
        } else {
            Device d = deviceService.getDevice(deviceId(uri));
            if (d == null) {
                error("No such device %s", uri);
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
     * Prints Port Statistics.
     *
     * @param deviceId
     * @param portStats
     */
    private void printPortStats(DeviceId deviceId, Iterable<PortStatistics> portStats) {
        print("deviceId=%s", deviceId);
        for (PortStatistics stat : sortByPort(portStats)) {
            if (portNumber != null && stat.port() != portNumber) {
                continue;
            }
            if (nonzero && stat.isZero()) {
                continue;
            }
            print(FORMAT, stat.port(), stat.packetsReceived(), stat.packetsSent(), stat.bytesReceived(),
                    stat.bytesSent(), stat.packetsRxDropped(), stat.packetsTxDropped(), stat.durationSec());
        }
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
            if (portNumber != null && stat.port() != portNumber) {
                continue;
            }
            if (nonzero && stat.isZero()) {
                continue;
            }
            float duration = ((float) stat.durationSec()) +
                    (((float) stat.durationNano()) / TimeUnit.SECONDS.toNanos(1));
            float rateRx = stat.bytesReceived() * 8 / duration;
            float rateTx = stat.bytesSent() * 8 / duration;
            print(formatDelta, stat.port(),
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
        print("| DeviceId = %s                                                                    |", deviceId);
        print("|---------------------------------------------------------------------------------------------------|");
        print("|      | Receive                                | Transmit                               | Time [s] |");
        print("| Port | Packets |  Bytes  | Rate bps |   Drop  | Packets |  Bytes  | Rate bps |   Drop  | Interval |");
        print("|---------------------------------------------------------------------------------------------------|");

        for (PortStatistics stat : sortByPort(portStats)) {
            if (portNumber != null && stat.port() != portNumber) {
                continue;
            }
            if (nonzero && stat.isZero()) {
                continue;
            }
            float duration = ((float) stat.durationSec()) +
                    (((float) stat.durationNano()) / TimeUnit.SECONDS.toNanos(1));
            float rateRx = stat.bytesReceived() * 8 / duration;
            float rateTx = stat.bytesSent() * 8 / duration;
            print(formatDeltaTable, stat.port(),
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
        portStatsList.sort((PortStatistics o1, PortStatistics o2) ->
                o1.port() - o2.port());
        return portStatsList;
    }
}
