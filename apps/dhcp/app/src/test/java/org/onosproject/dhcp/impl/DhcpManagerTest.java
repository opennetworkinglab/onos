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
package org.onosproject.dhcp.impl;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.DHCP;
import org.onlab.packet.DHCPOption;
import org.onlab.packet.DHCPPacketType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.UDP;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.dhcp.DhcpStore;
import org.onosproject.dhcp.IpAssignment;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.onosproject.net.NetTestTools.connectPoint;

/**
 * Set of tests of the ONOS application component.
 */

public class DhcpManagerTest {

    private DhcpManager dhcpXManager;

    protected PacketProcessor packetProcessor;

    protected HostProviderService hostProviderService;

    private static final HostId CLIENT1_HOST = HostId.hostId(MacAddress.valueOf("1a:1a:1a:1a:1a:1a"));

    private static final String EXPECTED_IP = "10.2.0.2";

    private static final Ip4Address BROADCAST = Ip4Address.valueOf("255.255.255.255");

    private static final int TRANSACTION_ID = 1000;

    private static final ProviderId PID = new ProviderId("of", "foo");

    @Before
    public void setUp() {
        dhcpXManager = new DhcpManager();
        dhcpXManager.cfgService = new TestNetworkConfigRegistry();
        dhcpXManager.packetService = new TestPacketService();
        dhcpXManager.coreService = new TestCoreService();
        dhcpXManager.dhcpStore = new TestDhcpStore();
        hostProviderService = new TestHostProviderService(new TestHostProvider());
        dhcpXManager.hostProviderService = hostProviderService;
        dhcpXManager.hostProviderRegistry = new TestHostRegistry();
        dhcpXManager.componentConfigService = new TestComponentConfig();
        dhcpXManager.activate();
    }

    @After
    public void tearDown() {
        dhcpXManager.deactivate();
    }

    /**
     * Tests the response to a DHCP Discover Packet.
     */
    @Test
    public void testDiscover() {
        Ethernet reply = constructDhcpPacket(DHCPPacketType.DHCPDISCOVER);
        sendPacket(reply);
    }

    /**
     * Tests the response to a DHCP Request Packet.
     */
    @Test
    public void testRequest() {
        Ethernet reply = constructDhcpPacket(DHCPPacketType.DHCPREQUEST);
        sendPacket(reply);
    }

    /**
     * Sends an Ethernet packet to the process method of the Packet Processor.
     * @param reply Ethernet packet
     */
    private void sendPacket(Ethernet reply) {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(reply.serialize());
        InboundPacket inPacket = new DefaultInboundPacket(connectPoint("1", 1),
                reply,
                byteBuffer);

        PacketContext context = new TestPacketContext(127L, inPacket, null, false);
        packetProcessor.process(context);
    }

    /**
     * Constructs an Ethernet packet containing a DHCP Payload.
     * @param packetType DHCP Message Type
     * @return Ethernet packet
     */
    private Ethernet constructDhcpPacket(DHCPPacketType packetType) {

        // Ethernet Frame.
        Ethernet ethReply = new Ethernet();
        ethReply.setSourceMACAddress(CLIENT1_HOST.mac());
        ethReply.setDestinationMACAddress(MacAddress.BROADCAST);
        ethReply.setEtherType(Ethernet.TYPE_IPV4);
        ethReply.setVlanID((short) 2);

        // IP Packet
        IPv4 ipv4Reply = new IPv4();
        ipv4Reply.setSourceAddress(0);
        ipv4Reply.setDestinationAddress(BROADCAST.toInt());
        ipv4Reply.setTtl((byte) 127);

        // UDP Datagram.
        UDP udpReply = new UDP();
        udpReply.setSourcePort((byte) UDP.DHCP_CLIENT_PORT);
        udpReply.setDestinationPort((byte) UDP.DHCP_SERVER_PORT);

        // DHCP Payload.
        DHCP dhcpReply = new DHCP();
        dhcpReply.setOpCode(DHCP.OPCODE_REQUEST);

        dhcpReply.setYourIPAddress(0);
        dhcpReply.setServerIPAddress(0);

        dhcpReply.setTransactionId(TRANSACTION_ID);
        dhcpReply.setClientHardwareAddress(CLIENT1_HOST.mac().toBytes());
        dhcpReply.setHardwareType(DHCP.HWTYPE_ETHERNET);
        dhcpReply.setHardwareAddressLength((byte) 6);

        // DHCP Options.
        DHCPOption option = new DHCPOption();
        List<DHCPOption> optionList = new ArrayList<>();

        // DHCP Message Type.
        option.setCode(DHCP.DHCPOptionCode.OptionCode_MessageType.getValue());
        option.setLength((byte) 1);
        byte[] optionData = {(byte) packetType.getValue()};
        option.setData(optionData);
        optionList.add(option);

        // DHCP Requested IP.
        option = new DHCPOption();
        option.setCode(DHCP.DHCPOptionCode.OptionCode_RequestedIP.getValue());
        option.setLength((byte) 4);
        optionData = Ip4Address.valueOf(EXPECTED_IP).toOctets();
        option.setData(optionData);
        optionList.add(option);

        // End Option.
        option = new DHCPOption();
        option.setCode(DHCP.DHCPOptionCode.OptionCode_END.getValue());
        option.setLength((byte) 1);
        optionList.add(option);

        dhcpReply.setOptions(optionList);

        udpReply.setPayload(dhcpReply);
        ipv4Reply.setPayload(udpReply);
        ethReply.setPayload(ipv4Reply);

        return ethReply;
    }

    /**
     * Validates the contents of the packet sent by the DHCP Manager.
     * @param packet Ethernet packet received
     */
    private void validatePacket(Ethernet packet) {
        DHCP dhcpPacket = (DHCP) packet.getPayload().getPayload().getPayload();
        assertEquals(MacAddress.valueOf(dhcpPacket.getClientHardwareAddress()), CLIENT1_HOST.mac());
        assertEquals(Ip4Address.valueOf(dhcpPacket.getYourIPAddress()), Ip4Address.valueOf(EXPECTED_IP));
        assertEquals(dhcpPacket.getTransactionId(), TRANSACTION_ID);
    }

    /**
     * Mocks the DHCPStore.
     */
    private final class TestDhcpStore implements DhcpStore {


        public void populateIPPoolfromRange(Ip4Address startIP, Ip4Address endIP) {
        }

        public Ip4Address suggestIP(HostId hostId, Ip4Address requestedIP) {
            return Ip4Address.valueOf(EXPECTED_IP);
        }

        public boolean assignIP(HostId hostId, IpAssignment ipAssignment) {
            return true;
        }

        public void setDefaultTimeoutForPurge(int timeInSeconds) {
        }

        public Ip4Address releaseIP(HostId hostId) {
            return null;
        }

        public Map<HostId, IpAssignment> listAssignedMapping() {
            return listAllMapping();
        }

        public Map<HostId, IpAssignment> listAllMapping() {
            Map<HostId, IpAssignment> map = new HashMap<>();
            IpAssignment assignment = IpAssignment.builder()
                                        .ipAddress(Ip4Address.valueOf(EXPECTED_IP))
                                        .assignmentStatus(IpAssignment.AssignmentStatus.Option_Assigned)
                                        .leasePeriod(300)
                                        .timestamp(new Date())
                                        .build();
            map.put(CLIENT1_HOST, assignment);
            return map;
        }

        public boolean assignStaticIP(MacAddress macID, IpAssignment ipAssignment) {
            return true;
        }

        public boolean removeStaticIP(MacAddress macID) {
            return true;
        }

        public Iterable<Ip4Address> getAvailableIPs() {
            List<Ip4Address> ipList = new ArrayList<>();
            ipList.add(Ip4Address.valueOf(EXPECTED_IP));
            return ImmutableSet.copyOf(ipList);
        }
        public IpAssignment getIpAssignmentFromAllocationMap(HostId hostId) {
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
     * Mocks the CoreService.
     */
    private class TestCoreService extends CoreServiceAdapter {

    }

    /**
     * Mocks the NetworkConfigRegistry.
     */
    private class TestNetworkConfigRegistry extends NetworkConfigRegistryAdapter {

    }

    /**
     * Mocks the ComponentConfigRegistry.
     */
    private class TestComponentConfig extends ComponentConfigAdapter {

    }

    /**
     * Mocks the HostProviderService.
     */
    private class TestHostProviderService extends AbstractProviderService<HostProvider>
            implements HostProviderService {

        protected TestHostProviderService(HostProvider provider) {
            super(provider);
        }

        @Override
        public void hostDetected(HostId hostId, HostDescription hostDescription, boolean replaceIps) {

        }

        @Override
        public void hostVanished(HostId hostId) {
        }

        @Override
        public void removeIpFromHost(HostId hostId, IpAddress ipAddress) {

        }

        @Override
        public void removeLocationFromHost(HostId hostId, HostLocation location) {

        }
    }

    /**
     * Mocks the HostProvider.
     */
    private static class TestHostProvider extends AbstractProvider
            implements HostProvider {

        protected TestHostProvider() {
            super(PID);
        }

        @Override
        public ProviderId id() {
            return PID;
        }

        @Override
        public void triggerProbe(Host host) {
        }

    }

    /**
     * Mocks the HostProviderRegistry.
     */
    private class TestHostRegistry implements HostProviderRegistry {

        @Override
        public HostProviderService register(HostProvider provider) {
            return hostProviderService;
        }

        @Override
        public void unregister(HostProvider provider) {
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }

    }

}
