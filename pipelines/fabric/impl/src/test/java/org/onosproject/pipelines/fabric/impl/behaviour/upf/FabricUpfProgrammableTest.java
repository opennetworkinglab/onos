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

package org.onosproject.pipelines.fabric.impl.behaviour.upf;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.util.HexString;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.upf.ForwardingActionRule;
import org.onosproject.net.behaviour.upf.PacketDetectionRule;
import org.onosproject.net.behaviour.upf.PdrStats;
import org.onosproject.net.behaviour.upf.UpfInterface;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.pi.model.PiCounterModel;
import org.onosproject.net.pi.model.PiTableModel;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.net.pi.service.PiTranslationService;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.onosproject.pipelines.fabric.impl.FabricPipeconfLoader;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricCapabilities;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_EGRESS_SPGW_PDR_COUNTER;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_INGRESS_SPGW_DOWNLINK_PDRS;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_INGRESS_SPGW_FARS;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_INGRESS_SPGW_PDR_COUNTER;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_INGRESS_SPGW_UPLINK_PDRS;

public class FabricUpfProgrammableTest {

    private static final ApplicationId APP_ID =
            TestApplicationId.create(FabricPipeconfLoader.PIPELINE_APP_NAME);

    private final DistributedFabricUpfStore upfStore = TestDistributedFabricUpfStore.build();
    private MockPacketService packetService;
    private FabricUpfProgrammable upfProgrammable;

    // Bytes of a random but valid Ethernet frame.
    private static final byte[] ETH_FRAME_BYTES = HexString.fromHexString(
            "00060708090a0001020304058100000a08004500006a000100004011f92ec0a80001c0a8000204d2005" +
                    "00056a8d5000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20" +
                    "2122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f4041424344454" +
                    "64748494a4b4c4d", "");
    private static final TrafficTreatment TABLE_OUTPUT_TREATMENT = DefaultTrafficTreatment.builder()
            .setOutput(PortNumber.TABLE)
            .build();

    private static final List<PiTableModel> TABLE_MODELS = ImmutableList.of(
            new MockTableModel(FABRIC_INGRESS_SPGW_UPLINK_PDRS,
                               TestUpfConstants.PHYSICAL_MAX_PDRS / 2),
            new MockTableModel(FABRIC_INGRESS_SPGW_DOWNLINK_PDRS,
                               TestUpfConstants.PHYSICAL_MAX_PDRS / 2),
            new MockTableModel(FABRIC_INGRESS_SPGW_FARS,
                               TestUpfConstants.PHYSICAL_MAX_FARS)
    );
    private static final List<PiCounterModel> COUNTER_MODELS = ImmutableList.of(
            new MockCounterModel(FABRIC_INGRESS_SPGW_PDR_COUNTER,
                                 TestUpfConstants.PHYSICAL_COUNTER_SIZE),
            new MockCounterModel(FABRIC_EGRESS_SPGW_PDR_COUNTER,
                                 TestUpfConstants.PHYSICAL_COUNTER_SIZE)
    );

    @Before
    public void setUp() throws Exception {
        FabricCapabilities capabilities = createMock(FabricCapabilities.class);
        expect(capabilities.supportUpf()).andReturn(true).anyTimes();
        replay(capabilities);

        // Services mock
        packetService = new MockPacketService();
        CoreService coreService = createMock(CoreService.class);
        NetworkConfigService netcfgService = createMock(NetworkConfigService.class);
        DeviceService deviceService = createMock(DeviceService.class);
        PiTranslationService piTranslationService = createMock(PiTranslationService.class);
        expect(coreService.getAppId(anyString())).andReturn(APP_ID).anyTimes();
        expect(netcfgService.getConfig(TestUpfConstants.DEVICE_ID, BasicDeviceConfig.class))
                .andReturn(TestUpfUtils.getBasicConfig(TestUpfConstants.DEVICE_ID, "/basic.json"))
                .anyTimes();
        replay(coreService, netcfgService);

        // Mock driverData to get the right device ID
        DriverData driverData = createMock(DriverData.class);
        expect(driverData.deviceId()).andReturn(TestUpfConstants.DEVICE_ID).anyTimes();
        replay(driverData);

        // Mock DriverHandler to get all the required mocked services
        DriverHandler driverHandler = createMock(DriverHandler.class);
        expect(driverHandler.get(FlowRuleService.class)).andReturn(new MockFlowRuleService()).anyTimes();
        expect(driverHandler.get(PacketService.class)).andReturn(packetService).anyTimes();
        expect(driverHandler.get(FabricUpfStore.class)).andReturn(upfStore).anyTimes();
        expect(driverHandler.get(NetworkConfigService.class)).andReturn(netcfgService).anyTimes();
        expect(driverHandler.get(CoreService.class)).andReturn(coreService).anyTimes();
        expect(driverHandler.get(DeviceService.class)).andReturn(deviceService).anyTimes();
        expect(driverHandler.get(PiTranslationService.class)).andReturn(piTranslationService).anyTimes();
        expect(driverHandler.get(PiPipeconfService.class))
                .andReturn(new MockPiPipeconfService(TABLE_MODELS, COUNTER_MODELS))
                .anyTimes();
        expect(driverHandler.get(P4RuntimeController.class))
                .andReturn(new MockP4RuntimeController(TestUpfConstants.DEVICE_ID,
                                                       TestUpfConstants.COUNTER_PKTS,
                                                       TestUpfConstants.COUNTER_BYTES,
                                                       TestUpfConstants.PHYSICAL_COUNTER_SIZE))
                .anyTimes();
        expect(driverHandler.data()).andReturn(driverData).anyTimes();
        replay(driverHandler);

        upfProgrammable = new FabricUpfProgrammable();
        TestUtils.setField(upfProgrammable, "handler", driverHandler);
        TestUtils.setField(upfProgrammable, "data", driverData);
        ConcurrentMap<DeviceId, URI> channelUris = TestUtils.getField(upfProgrammable, "CHANNEL_URIS");
        channelUris.put(TestUpfConstants.DEVICE_ID, new URI("grpc://localhost:1234?device_id=1"));
    }

    @Test
    public void testUplinkPdr() throws Exception {
        assertTrue(upfProgrammable.getPdrs().isEmpty());
        PacketDetectionRule expectedPdr = TestUpfConstants.UPLINK_PDR;
        upfProgrammable.addPdr(expectedPdr);
        Collection<PacketDetectionRule> installedPdrs = upfProgrammable.getPdrs();
        assertThat(installedPdrs.size(), equalTo(1));
        for (var readPdr : installedPdrs) {
            assertThat(readPdr, equalTo(expectedPdr));
        }
        upfProgrammable.removePdr(expectedPdr.withoutActionParams());
        assertTrue(upfProgrammable.getPdrs().isEmpty());
    }

    @Test
    public void testDownlinkPdr() throws Exception {
        assertTrue(upfProgrammable.getPdrs().isEmpty());
        PacketDetectionRule expectedPdr = TestUpfConstants.DOWNLINK_PDR;
        upfProgrammable.addPdr(expectedPdr);
        Collection<PacketDetectionRule> installedPdrs = upfProgrammable.getPdrs();
        assertThat(installedPdrs.size(), equalTo(1));
        for (var readPdr : installedPdrs) {
            assertThat(readPdr, equalTo(expectedPdr));
        }
        upfProgrammable.removePdr(expectedPdr.withoutActionParams());
        assertTrue(upfProgrammable.getPdrs().isEmpty());
    }

    @Test
    public void testUplinkFar() throws Exception {
        assertTrue(upfProgrammable.getFars().isEmpty());
        ForwardingActionRule expectedFar = TestUpfConstants.UPLINK_FAR;
        upfProgrammable.addFar(expectedFar);
        Collection<ForwardingActionRule> installedFars = upfProgrammable.getFars();
        assertThat(installedFars.size(), equalTo(1));
        for (var readFar : installedFars) {
            assertThat(readFar, equalTo(expectedFar));
        }
        upfProgrammable.removeFar(expectedFar.withoutActionParams());
        assertTrue(upfProgrammable.getFars().isEmpty());
    }

    @Test
    public void testDownlinkFar() throws Exception {
        assertTrue(upfProgrammable.getFars().isEmpty());
        ForwardingActionRule expectedFar = TestUpfConstants.DOWNLINK_FAR;
        upfProgrammable.addFar(expectedFar);
        Collection<ForwardingActionRule> installedFars = upfProgrammable.getFars();
        assertThat(installedFars.size(), equalTo(1));
        for (var readFar : installedFars) {
            assertThat(readFar, equalTo(expectedFar));
        }
        upfProgrammable.removeFar(expectedFar.withoutActionParams());
        assertTrue(upfProgrammable.getFars().isEmpty());
    }

    @Test
    public void testUplinkInterface() throws Exception {
        assertTrue(upfProgrammable.getInterfaces().isEmpty());
        UpfInterface expectedInterface = TestUpfConstants.UPLINK_INTERFACE;
        upfProgrammable.addInterface(expectedInterface);
        Collection<UpfInterface> installedInterfaces = upfProgrammable.getInterfaces();
        assertThat(installedInterfaces.size(), equalTo(1));
        for (var readInterface : installedInterfaces) {
            assertThat(readInterface, equalTo(expectedInterface));
        }
        upfProgrammable.removeInterface(expectedInterface);
        assertTrue(upfProgrammable.getInterfaces().isEmpty());
    }

    @Test
    public void testDownlinkInterface() throws Exception {
        assertTrue(upfProgrammable.getInterfaces().isEmpty());
        UpfInterface expectedInterface = TestUpfConstants.DOWNLINK_INTERFACE;
        upfProgrammable.addInterface(expectedInterface);
        Collection<UpfInterface> installedInterfaces = upfProgrammable.getInterfaces();
        assertThat(installedInterfaces.size(), equalTo(1));
        for (var readInterface : installedInterfaces) {
            assertThat(readInterface, equalTo(expectedInterface));
        }
        upfProgrammable.removeInterface(expectedInterface);
        assertTrue(upfProgrammable.getInterfaces().isEmpty());
    }

    @Test
    public void testClearInterfaces() throws Exception {
        assertTrue(upfProgrammable.getInterfaces().isEmpty());
        upfProgrammable.addInterface(TestUpfConstants.UPLINK_INTERFACE);
        upfProgrammable.addInterface(TestUpfConstants.DOWNLINK_INTERFACE);
        assertThat(upfProgrammable.getInterfaces().size(), equalTo(2));
        upfProgrammable.clearInterfaces();
        assertTrue(upfProgrammable.getInterfaces().isEmpty());
    }

    @Test
    public void testReadAllCounters() {
        Collection<PdrStats> allStats = upfProgrammable.readAllCounters(-1);
        assertThat(allStats.size(), equalTo(TestUpfConstants.PHYSICAL_COUNTER_SIZE));
        for (PdrStats stat : allStats) {
            assertThat(stat.getIngressBytes(), equalTo(TestUpfConstants.COUNTER_BYTES));
            assertThat(stat.getEgressBytes(), equalTo(TestUpfConstants.COUNTER_BYTES));
            assertThat(stat.getIngressPkts(), equalTo(TestUpfConstants.COUNTER_PKTS));
            assertThat(stat.getEgressPkts(), equalTo(TestUpfConstants.COUNTER_PKTS));
        }
    }

    @Test
    public void testReadAllCountersLimitedCounters() {
        Collection<PdrStats> allStats = upfProgrammable.readAllCounters(10);
        assertThat(allStats.size(), equalTo(10));
    }

    @Test
    public void testReadAllCountersPhysicalLimit() {
        Collection<PdrStats> allStats = upfProgrammable.readAllCounters(1024);
        assertThat(allStats.size(), equalTo(TestUpfConstants.PHYSICAL_COUNTER_SIZE));
    }

    @Test
    public void testSendPacketOut() {
        upfProgrammable.sendPacketOut(ByteBuffer.wrap(ETH_FRAME_BYTES));
        var emittedPkt = packetService.emittedPackets.poll();
        assertNotNull(emittedPkt);
        assertThat(emittedPkt.data().array(), equalTo(ETH_FRAME_BYTES));
        assertThat(emittedPkt.treatment(), equalTo(TABLE_OUTPUT_TREATMENT));
    }
}
