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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.DHCP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.UDP;
import org.onlab.packet.dhcp.DhcpOption;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.LeadershipServiceAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.openstack4j.model.network.AllowedAddressPair;
import org.openstack4j.model.network.HostRoute;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.IPVersionType;
import org.openstack4j.model.network.Ipv6AddressMode;
import org.openstack4j.model.network.Ipv6RaMode;
import org.openstack4j.model.network.Pool;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.State;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.network.builder.PortBuilder;
import org.openstack4j.model.network.builder.SubnetBuilder;
import org.openstack4j.openstack.networking.domain.NeutronIP;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.onosproject.net.NetTestTools.connectPoint;

/**
 * Unit tests for Openstack switching DHCP handler.
 */
public class OpenstackSwitchingDhcpHandlerTest {

    private OpenstackSwitchingDhcpHandler dhcpHandler;

    protected PacketProcessor packetProcessor;

    private static final HostId CLIENT_HOST =
                         HostId.hostId(MacAddress.valueOf("1a:1a:1a:1a:1a:1a"));
    private static final String EXPECTED_IP = "10.2.0.2";
    private static final String EXPECTED_CIDR = "10.2.0.0/24";
    private static final String EXPECTED_GATEWAY = "10.2.0.1";
    private static final Ip4Address BROADCAST =
                         Ip4Address.valueOf("255.255.255.255");
    private static final int TRANSACTION_ID = 1986;

    private static final String IP_SUBNET_ID = "1";

    private DHCP.MsgType testMsgType;

    @Before
    public void setUp() {
        dhcpHandler = new OpenstackSwitchingDhcpHandler();
        dhcpHandler.coreService = new TestCoreService();
        dhcpHandler.configService = new TestConfigService();
        dhcpHandler.packetService = new TestPacketService();
        dhcpHandler.instancePortService = new TestInstancePortService();
        dhcpHandler.osNetworkService = new TestOpenstackNetworkService();
        dhcpHandler.osNodeService = new TestOpenstackNodeService();
        dhcpHandler.osFlowRuleService = new TestOpenstackFlowRuleService();
        dhcpHandler.clusterService = new TestClusterService();
        dhcpHandler.leadershipService = new TestLeadershipService();
        dhcpHandler.activate();
    }

    @After
    public void tearDown() {
        dhcpHandler.deactivate();
    }

    /**
     * Tests the response to a DHCP Discover Packet.
     */
    @Test
    public void testDiscover() {
        Ethernet dhcpDiscover = constructDhcpPacket(DHCP.MsgType.DHCPDISCOVER);
        testMsgType = DHCP.MsgType.DHCPDISCOVER;
        sendPacket(dhcpDiscover);
    }

    /**
     * Tests the response to a DHCP Request Packet.
     */
    @Test
    public void testRequest() {
        Ethernet dhcpRequest = constructDhcpPacket(DHCP.MsgType.DHCPREQUEST);
        testMsgType = DHCP.MsgType.DHCPREQUEST;
        sendPacket(dhcpRequest);
    }

    /**
     * Sends an Ethernet packet to the process method of the Packet Processor.
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
     * Validates the contents of the packet sent by the OpenstackSwitchingDhcpHandler.
     *
     * @param packet Ethernet packet received
     */
    private void validatePacket(Ethernet packet) {
        DHCP dhcpPacket = (DHCP) packet.getPayload().getPayload().getPayload();
        assertEquals(MacAddress.valueOf(dhcpPacket.getClientHardwareAddress()), CLIENT_HOST.mac());
        assertEquals(Ip4Address.valueOf(dhcpPacket.getYourIPAddress()), Ip4Address.valueOf(EXPECTED_IP));
        assertEquals(dhcpPacket.getTransactionId(), TRANSACTION_ID);

        if (testMsgType == DHCP.MsgType.DHCPDISCOVER) {
            assertEquals(dhcpPacket.getPacketType(), DHCP.MsgType.DHCPOFFER);
        }

        if (testMsgType == DHCP.MsgType.DHCPREQUEST) {
            assertEquals(dhcpPacket.getPacketType(), DHCP.MsgType.DHCPACK);
        }
    }

    /**
     * Constructs an Ethernet packet containing a DHCP payload.
     *
     * @param msgType DHCP message type
     * @return Ethernet packet
     */
    private Ethernet constructDhcpPacket(DHCP.MsgType msgType) {

        // Ethernet frame
        Ethernet ethFrame = new Ethernet();
        ethFrame.setSourceMACAddress(CLIENT_HOST.mac());
        ethFrame.setDestinationMACAddress(MacAddress.BROADCAST);
        ethFrame.setEtherType(Ethernet.TYPE_IPV4);
        ethFrame.setVlanID((short) 2);

        // IP packet
        IPv4 ipv4Pkt = new IPv4();
        ipv4Pkt.setSourceAddress(0);
        ipv4Pkt.setDestinationAddress(BROADCAST.toInt());
        ipv4Pkt.setTtl((byte) 127);

        // UDP datagram
        UDP udpDatagram = new UDP();
        udpDatagram.setSourcePort((byte) UDP.DHCP_CLIENT_PORT);
        udpDatagram.setDestinationPort((byte) UDP.DHCP_SERVER_PORT);

        // DHCP payload
        DHCP dhcp = new DHCP();

        dhcp.setOpCode(DHCP.OPCODE_REQUEST);

        dhcp.setYourIPAddress(0);
        dhcp.setServerIPAddress(0);

        dhcp.setTransactionId(TRANSACTION_ID);
        dhcp.setClientHardwareAddress(CLIENT_HOST.mac().toBytes());
        dhcp.setHardwareType(DHCP.HWTYPE_ETHERNET);
        dhcp.setHardwareAddressLength((byte) 6);

        // DHCP options start...
        DhcpOption option = new DhcpOption();
        List<DhcpOption> optionList = new ArrayList<>();

        // DHCP message type
        option.setCode(DHCP.DHCPOptionCode.OptionCode_MessageType.getValue());
        option.setLength((byte) 1);
        byte[] optionData = {(byte) msgType.getValue()};
        option.setData(optionData);
        optionList.add(option);

        // DHCP requested IP address
        option = new DhcpOption();
        option.setCode(DHCP.DHCPOptionCode.OptionCode_RequestedIP.getValue());
        option.setLength((byte) 4);
        optionData = Ip4Address.valueOf(EXPECTED_IP).toOctets();
        option.setData(optionData);
        optionList.add(option);

        // DHCP options end...
        option = new DhcpOption();
        option.setCode(DHCP.DHCPOptionCode.OptionCode_END.getValue());
        option.setLength((byte) 1);
        optionList.add(option);

        dhcp.setOptions(optionList);

        udpDatagram.setPayload(dhcp);
        ipv4Pkt.setPayload(udpDatagram);
        ethFrame.setPayload(ipv4Pkt);

        return ethFrame;
    }

    /**
     * Mocks the CoreService.
     */
    private class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return new DefaultApplicationId(100, "dhcpTestApp");
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
     * Mocks the InstancePortService.
     */
    private class TestInstancePortService extends InstancePortServiceAdapter {

        @Override
        public InstancePort instancePort(MacAddress macAddress) {
            return new TestInstancePort();
        }
    }

    /**
     * Mocks the OpenstackNetworkService.
     */
    private class TestOpenstackNetworkService extends OpenstackNetworkServiceAdapter {
        @Override
        public Port port(String portId) {
            return new TestPort();
        }

        @Override
        public Subnet subnet(String subnetId) {
            return new TestSubnet();
        }

        /**
         * Mocks the Neutron port.
         */
        private class TestPort implements Port {

            @Override
            public State getState() {
                return null;
            }

            @Override
            public boolean isAdminStateUp() {
                return false;
            }

            @Override
            public String getNetworkId() {
                return null;
            }

            @Override
            public String getDeviceId() {
                return null;
            }

            @Override
            public String getDeviceOwner() {
                return null;
            }

            @Override
            public Set<? extends IP> getFixedIps() {
                return ImmutableSet.of(new NeutronIP(EXPECTED_IP, IP_SUBNET_ID));
            }

            @Override
            public Set<? extends AllowedAddressPair> getAllowedAddressPairs() {
                return null;
            }

            @Override
            public String getMacAddress() {
                return null;
            }

            @Override
            public List<String> getSecurityGroups() {
                return null;
            }

            @Override
            public Boolean isPortSecurityEnabled() {
                return null;
            }

            @Override
            public String getHostId() {
                return null;
            }

            @Override
            public String getVifType() {
                return null;
            }

            @Override
            public Map<String, Object> getVifDetails() {
                return null;
            }

            @Override
            public String getvNicType() {
                return null;
            }

            @Override
            public Map<String, Object> getProfile() {
                return null;
            }

            @Override
            public PortBuilder toBuilder() {
                return null;
            }

            @Override
            public String getTenantId() {
                return null;
            }

            @Override
            public void setTenantId(String tenantId) {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setName(String name) {

            }

            @Override
            public String getId() {
                return null;
            }

            @Override
            public void setId(String id) {

            }
        }

        /**
         * Mocks the Neutron subnet.
         */
        private class TestSubnet implements Subnet {

            @Override
            public boolean isDHCPEnabled() {
                return false;
            }

            @Override
            public String getNetworkId() {
                return null;
            }

            @Override
            public List<String> getDnsNames() {
                return null;
            }

            @Override
            public List<? extends Pool> getAllocationPools() {
                return null;
            }

            @Override
            public List<? extends HostRoute> getHostRoutes() {
                return Lists.newArrayList();
            }

            @Override
            public IPVersionType getIpVersion() {
                return null;
            }

            @Override
            public String getGateway() {
                return EXPECTED_GATEWAY;
            }

            @Override
            public String getCidr() {
                return EXPECTED_CIDR;
            }

            @Override
            public Ipv6AddressMode getIpv6AddressMode() {
                return null;
            }

            @Override
            public Ipv6RaMode getIpv6RaMode() {
                return null;
            }

            @Override
            public SubnetBuilder toBuilder() {
                return null;
            }

            @Override
            public String getTenantId() {
                return null;
            }

            @Override
            public void setTenantId(String tenantId) {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setName(String name) {

            }

            @Override
            public String getId() {
                return null;
            }

            @Override
            public void setId(String id) {

            }
        }
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
            return null;
        }

        @Override
        public IpAddress ipAddress() {
            return IpAddress.valueOf(EXPECTED_IP);
        }

        @Override
        public DeviceId deviceId() {
            return null;
        }

        @Override
        public PortNumber portNumber() {
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
}
