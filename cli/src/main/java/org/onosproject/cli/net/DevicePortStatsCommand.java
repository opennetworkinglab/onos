package org.onosproject.cli.net;

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

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;

/**
 * Lists port statistic of all ports in the system.
 */
@Command(scope = "onos", name = "portstats",
        description = "Lists statistics of all ports in the system")
public class DevicePortStatsCommand extends AbstractShellCommand {

    private static final String FORMAT =
            "   port=%s, pktRx=%s, pktTx=%s, bytesRx=%s, bytesTx=%s, pktRxDrp=%s, pktTxDrp=%s, Dur=%s";

    @Override
    protected void execute() {
        DeviceService deviceService = get(DeviceService.class);

        deviceService.getDevices().forEach(d ->
                        printPortStats(d.id(), deviceService.getPortStatistics(d.id()))
        );
    }

    private void printPortStats(DeviceId deviceId, Iterable<PortStatistics> portStats) {
        print("deviceId=%s", deviceId);
        for (PortStatistics stat : portStats) {
            print(FORMAT, stat.port(), stat.packetsReceived(), stat.packetsSent(), stat.bytesReceived(),
                    stat.bytesSent(), stat.packetsRxDropped(), stat.packetsTxDropped(), stat.durationSec());
        }
    }
}
