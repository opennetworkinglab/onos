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
package org.onosproject.openstacknetworking.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.LeadershipServiceAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.openstacknetworking.api.InstancePort;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.onosproject.net.NetTestTools.connectPoint;

public class OpenstackSwitchingArpHandlerTest {

    private OpenstackSwitchingArpHandler arpHandler;

    protected PacketProcessor packetProcessor;

    private static final MacAddress CLIENT_MAC = MacAddress.valueOf("1a:1a:1a:1a:1a:1a");
    private static final IpAddress CLIENT_IP = IpAddress.valueOf("10.10.10.10");
    private static final IpAddress TARGET_IP = IpAddress.valueOf("10.10.10.20");
    private static final MacAddress EXPECTED_MAC = MacAddress.valueOf("2b:2b:2b:2b:2b:2b");

    @Before
    public void setUp() {
        arpHandler = new OpenstackSwitchingArpHandler();
        arpHandler.coreService = new TestCoreService();
        arpHandler.configService = new TestConfigService();
        arpHandler.instancePortService = new TestInstancePortService();
        arpHandler.packetService = new TestPacketService();
        arpHandler.osNetworkService = new TestOpenstackNetworkService();
        arpHandler.osNodeService = new TestOpenstackNodeService();
        arpHandler.osFlowRuleService = new TestOpenstackFlowRuleService();
        arpHandler.clusterService = new TestClusterService();
        arpHandler.leadershipService = new TestLeadershipService();
        arpHandler.activate();
    }

    @After
    public void tearDown() {
        arpHandler.deactivate();
    }

    /**
     * Tests the response to an ARP Request Packet.
     */
    @Test
    public void testRequest() {
        Ethernet arpRequest = constructArpPacket(ARP.OP_REQUEST);
        sendPacket(arpRequest);
    }

    /**
     * Sends an Ethernet packet to the process method of the Packet processor.
     *
     * @param ethernet Ethernet packet
     */
    private void sendPacket(Ethernet ethernet) {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(ethernet.serialize());
        InboundPacket inPacket = new DefaultInboundPacket(connectPoint("1", 1),
                ethernet,
                byteBuffer);

        PacketContext context = new TestPacketContext(127L, inPacket, null, false);
        packetProcessor.process(context);
    }

    /**
     * Validates the contents of the packet sent by the OpenstackSwitchingArpHandler.
     *
     * @param packet Ethernet packet received
     */
    private void validatePacket(Ethernet packet) {
        ARP arpPacket = (ARP) packet.getPayload();

        assertEquals(arpPacket.getOpCode(), ARP.OP_REPLY);
        assertArrayEquals(arpPacket.getSenderHardwareAddress(), EXPECTED_MAC.toBytes());
        assertArrayEquals(arpPacket.getSenderProtocolAddress(), TARGET_IP.toOctets());
        assertArrayEquals(arpPacket.getTargetHardwareAddress(), CLIENT_MAC.toBytes());
        assertArrayEquals(arpPacket.getTargetProtocolAddress(), CLIENT_IP.toOctets());
        assertEquals(arpPacket.getProtocolType(), ARP.PROTO_TYPE_IP);
        assertEquals(arpPacket.getHardwareType(), ARP.HW_TYPE_ETHERNET);
    }

    /**
     * Constructs an Ethernet packet containing an ARP payload.
     *
     * @return Ethernet packet
     */
    private Ethernet constructArpPacket(short pktType) {
        if (pktType == ARP.OP_REQUEST) {

            return ARP.buildArpRequest(
                    CLIENT_MAC.toBytes(),
                    CLIENT_IP.toOctets(),
                    TARGET_IP.toOctets(),
                    (short) 2);
        }

        return null;
    }

    /**
     * Mocks the CoreService.
     */
    private class TestCoreService extends CoreServiceAdapter {
        @Override
        public ApplicationId registerApplication(String name) {
            return new DefaultApplicationId(100, "arpTestApp");
        }
    }

    /**
     * Mocks the ComponentConfigRegistry.
     */
    private class TestConfigService extends ComponentConfigAdapter {
    }

    /**
     * Mocks the ClusterService.
     */
    private class TestClusterService extends ClusterServiceAdapter {
    }

    /**
     * Mocks the LeadershipService.
     */
    private class TestLeadershipService extends LeadershipServiceAdapter {
    }

    /**
     * Mocks the OpenstackNodeService.
     */
    private class TestOpenstackNodeService extends OpenstackNodeServiceAdapter {
    }

    /**
     * Mocks the OpenstackFlowRuleService.
     */
    private class TestOpenstackFlowRuleService extends OpenstackFlowRuleServiceAdapter {
    }

    /**
     * Mocks the InstancePortService.
     */
    private class TestInstancePortService extends InstancePortServiceAdapter {

        @Override
        public InstancePort instancePort(MacAddress macAddress) {
            return new TestInstancePort();
        }

        @Override
        public InstancePort instancePort(IpAddress ipAddress, String osNetId) {
            return new TestInstancePort();
        }
    }

    /**
     * Mocks the instance port.
     */
    private final class TestInstancePort implements InstancePort {

        @Override
        public String networkId() {
            return null;
        }

        @Override
        public String portId() {
            return null;
        }

        @Override
        public MacAddress macAddress() {
            return EXPECTED_MAC;
        }

        @Override
        public IpAddress ipAddress() {
            return null;
        }

        @Override
        public DeviceId deviceId() {
            return null;
        }

        @Override
        public DeviceId oldDeviceId() {
            return null;
        }

        @Override
        public PortNumber portNumber() {
            return null;
        }

        @Override
        public PortNumber oldPortNumber() {
            return null;
        }

        @Override
        public State state() {
            return null;
        }

        @Override
        public InstancePort updateState(State newState) {
            return null;
        }

        @Override
        public InstancePort updatePrevLocation(DeviceId oldDeviceId, PortNumber oldPortNumber) {
            return null;
        }
    }

    /**
     * Mocks the DefaultPacket context.
     */
    private final class TestPacketContext extends DefaultPacketContext {
        private TestPacketContext(long time, InboundPacket inPkt,
                                  OutboundPacket outPkt, boolean block) {
            super(time, inPkt, outPkt, block);
        }

        @Override
        public void send() {
            // We don't send anything out.
        }
    }

    /**
     * Keeps a reference to the PacketProcessor and verifies the OutboundPackets.
     */
    private class TestPacketService extends PacketServiceAdapter {
        @Override
        public void addProcessor(PacketProcessor processor, int priority) {
            packetProcessor = processor;
        }

        @Override
        public void emit(OutboundPacket packet) {
            try {
                Ethernet eth = Ethernet.deserializer().deserialize(packet.data().array(),
                        0, packet.data().array().length);
                validatePacket(eth);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
    }

    /**
     * Mocks the OpenstackNetworkService.
     */
    private class TestOpenstackNetworkService extends OpenstackNetworkServiceAdapter {
    }
}
