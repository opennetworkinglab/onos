/*
 * Copyright 2021-present Open Networking Foundation
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
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static org.easymock.EasyMock.*;

/**
 * Unit test for DevicePortStatsCommand.
 */
public class DevicePortStatsCommandTest {

    private DevicePortStatsCommand devicePortStatsCommand;
    private PortStatistics portStatistics;
    private DeviceService deviceService;
    private List<Device> devices = new ArrayList<>();
    private List<PortStatistics> portStatisticsList = new ArrayList<>();
    private DeviceId id1;


    @Before
    public void setUp() {
        devicePortStatsCommand = new DevicePortStatsCommand();

        id1 = NetTestTools.did("d1");

        DefaultDevice d1 = new DefaultDevice(NetTestTools.PID, id1, Device.Type.SWITCH,
                                             "test", "1.0", "1.0",
                                             "abacab", new ChassisId("c"),
                                             DefaultAnnotations.EMPTY);

        devices.add(d1);

        portStatistics = DefaultPortStatistics.builder()
                .setDurationSec(1)
                .setBytesReceived(10)
                .setBytesSent(20)
                .setDurationNano(30)
                .setPacketsReceived(40)
                .setPacketsSent(50)
                .setPacketsRxDropped(60)
                .setPacketsRxErrors(70)
                .setPacketsTxDropped(80)
                .setPacketsTxErrors(90)
                .setPort(PortNumber.portNumber(81))
                .setDeviceId(id1)
                .build();

        portStatisticsList.add(portStatistics);

        deviceService = createMock(DeviceService.class);
        expect(deviceService.getPortStatistics(id1))
                .andReturn(portStatisticsList);
        expect(deviceService.getPortDeltaStatistics(id1))
                .andReturn(portStatisticsList);

        replay(deviceService);
    }


    /**
     * Tests json port stats output.
     */
    @Test
    public void testJsonPortStats() {

        JsonNode node = devicePortStatsCommand.jsonPortStats(deviceService, devices).get(0);

        assertEquals(node.findValue("deviceId").asText(), id1.toString());
        assertEquals(node.findValue("port").asText(), portStatistics.portNumber().toString());
        assertEquals(node.findValue("pktRx").asLong(), portStatistics.packetsReceived());
        assertEquals(node.findValue("pktTx").asLong(), portStatistics.packetsSent());
        assertEquals(node.findValue("bytesRx").asLong(), portStatistics.bytesReceived());
        assertEquals(node.findValue("bytesTx").asLong(), portStatistics.bytesSent());
        assertEquals(node.findValue("pktRxDrp").asLong(), portStatistics.packetsRxDropped());
        assertEquals(node.findValue("pktTxDrp").asLong(), portStatistics.packetsTxDropped());
        assertEquals(node.findValue("Dur").asLong(), portStatistics.durationSec());

    }

    /**
     * Tests json port stats delta output.
     */
    @Test
    public void testJsonPortStatsDelta() {

        JsonNode node = devicePortStatsCommand.jsonPortStatsDelta(deviceService, devices).get(0);

        float duration = ((float) portStatistics.durationSec()) +
                (((float) portStatistics.durationNano()) / TimeUnit.SECONDS.toNanos(1));
        float rateRx = portStatistics.bytesReceived() * 8 / duration;
        float rateTx = portStatistics.bytesSent() * 8 / duration;

        assertEquals(node.findValue("deviceId").asText(), id1.toString());
        assertEquals(node.findValue("port").asText(), portStatistics.portNumber().toString());
        assertEquals(node.findValue("pktRx").asLong(), portStatistics.packetsReceived());
        assertEquals(node.findValue("pktTx").asLong(), portStatistics.packetsSent());
        assertEquals(node.findValue("bytesRx").asLong(), portStatistics.bytesReceived());
        assertEquals(node.findValue("bytesTx").asLong(), portStatistics.bytesSent());
        assertEquals(node.findValue("rateRx").asText(), String.format("%.1f", rateRx));
        assertEquals(node.findValue("rateTx").asText(), String.format("%.1f", rateTx));
        assertEquals(node.findValue("pktRxDrp").asLong(), portStatistics.packetsRxDropped());
        assertEquals(node.findValue("pktTxDrp").asLong(), portStatistics.packetsTxDropped());
        assertEquals(node.findValue("interval").asText(), String.format("%.3f", duration));

    }
}
