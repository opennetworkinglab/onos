/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.aaa;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Data;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.EAP;
import org.onlab.packet.EAPOL;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.RADIUS;
import org.onlab.packet.RADIUSAttribute;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.net.Annotations;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.net.provider.ProviderId;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.onosproject.net.NetTestTools.connectPoint;

/**
 * Set of tests of the ONOS application component.
 */
public class AAATest {

    MacAddress clientMac = MacAddress.valueOf("1a:1a:1a:1a:1a:1a");
    MacAddress serverMac = MacAddress.valueOf("2a:2a:2a:2a:2a:2a");

    PacketProcessor packetProcessor;
    private AAA aaa;
    List<Ethernet> savedPackets = new LinkedList<>();

    /**
     * Saves the given packet onto the saved packets list.
     *
     * @param eth packet to save
     */
    private void savePacket(Ethernet eth) {
        savedPackets.add(eth);
    }

    /**
     * Keeps a reference to the PacketProcessor and saves the OutboundPackets.
     */
    private class MockPacketService extends PacketServiceAdapter {

        @Override
        public void addProcessor(PacketProcessor processor, int priority) {
            packetProcessor = processor;
        }

        @Override
        public void emit(OutboundPacket packet) {
            try {
                Ethernet eth = Ethernet.deserializer().deserialize(packet.data().array(),
                                                                   0, packet.data().array().length);
                savePacket(eth);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
    }

    /**
     * Mocks the DefaultPacketContext.
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
     * Mocks a host to allow locating the Radius server.
     */
    private static final class MockHost implements Host {
        @Override
        public HostId id() {
            return null;
        }

        @Override
        public MacAddress mac() {
            return null;
        }

        @Override
        public VlanId vlan() {
            return VlanId.vlanId(VlanId.UNTAGGED);
        }

        @Override
        public Set<IpAddress> ipAddresses() {
            return null;
        }

        @Override
        public HostLocation location() {
            return null;
        }

        @Override
        public Annotations annotations() {
            return null;
        }

        @Override
        public ProviderId providerId() {
            return null;
        }
    }

    /**
     * Mocks the Host service.
     */
    private static final class MockHostService extends HostServiceAdapter {
        @Override
        public Set<Host> getHostsByIp(IpAddress ip) {
            return ImmutableSet.of(new MockHost());
        }
    }

    /**
     * Mocks the network config registry.
     */
    @SuppressWarnings("unchecked")
    private static final class TestNetworkConfigRegistry
            extends NetworkConfigRegistryAdapter {
        @Override
        public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
            return (C) new AAAConfig();
        }
    }

    /**
     * Sends an Ethernet packet to the process method of the Packet Processor.
     *
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
     * Constructs an Ethernet packet containing a EAPOL_START Payload.
     *
     * @return Ethernet packet
     */
    private Ethernet constructSupplicantStartPacket() {
        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(clientMac.toBytes());
        eth.setSourceMACAddress(serverMac.toBytes());
        eth.setEtherType(EthType.EtherType.EAPOL.ethType().toShort());
        eth.setVlanID((short) 2);

        EAP eap = new EAP(EAPOL.EAPOL_START, (byte) 1, EAPOL.EAPOL_START, null);

        //eapol header
        EAPOL eapol = new EAPOL();
        eapol.setEapolType(EAPOL.EAPOL_START);
        eapol.setPacketLength(eap.getLength());

        //eap part
        eapol.setPayload(eap);

        eth.setPayload(eapol);
        eth.setPad(true);
        return eth;
    }

    /**
     * Constructs an Ethernet packet containing identification payload.
     *
     * @return Ethernet packet
     */
    private Ethernet constructSupplicantIdentifyPacket(byte type) {
        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(clientMac.toBytes());
        eth.setSourceMACAddress(serverMac.toBytes());
        eth.setEtherType(EthType.EtherType.EAPOL.ethType().toShort());
        eth.setVlanID((short) 2);

        String username = "user";
        EAP eap = new EAP(EAP.REQUEST, (byte) 1, type,
                          username.getBytes(Charsets.US_ASCII));
        eap.setIdentifier((byte) 1);

        // eapol header
        EAPOL eapol = new EAPOL();
        eapol.setEapolType(EAPOL.EAPOL_PACKET);
        eapol.setPacketLength(eap.getLength());

        // eap part
        eapol.setPayload(eap);

        eth.setPayload(eapol);
        eth.setPad(true);
        return eth;
    }

    /**
     * Constructs an Ethernet packet containing a RADIUS challenge
     * packet.
     *
     * @param challengeCode code to use in challenge packet
     * @param challengeType type to use in challenge packet
     * @return Ethernet packet
     */
    private Ethernet constructRADIUSCodeAccessChallengePacket(byte challengeCode, byte challengeType) {
        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(clientMac.toBytes());
        eth.setSourceMACAddress(serverMac.toBytes());
        eth.setEtherType(EthType.EtherType.IPV4.ethType().toShort());
        eth.setVlanID((short) 2);

        IPv4 ipv4 = new IPv4();
        ipv4.setProtocol(IPv4.PROTOCOL_UDP);
        ipv4.setSourceAddress(aaa.radiusIpAddress.getHostAddress());

        String challenge = "1234";

        EAP eap = new EAP(challengeType, (byte) 1, challengeType,
                          challenge.getBytes(Charsets.US_ASCII));
        eap.setIdentifier((byte) 1);

        RADIUS radius = new RADIUS();
        radius.setCode(challengeCode);

        radius.setAttribute(RADIUSAttribute.RADIUS_ATTR_STATE,
                            challenge.getBytes(Charsets.US_ASCII));

        radius.setPayload(eap);
        radius.setAttribute(RADIUSAttribute.RADIUS_ATTR_EAP_MESSAGE,
                            eap.serialize());

        UDP udp = new UDP();
        udp.setPayload(radius);
        ipv4.setPayload(udp);

        eth.setPayload(ipv4);
        eth.setPad(true);
        return eth;
    }

    /**
     * Sets up the services required by the AAA application.
     */
    @Before
    public void setUp() {
        aaa = new AAA();
        aaa.netCfgService = new TestNetworkConfigRegistry();
        aaa.coreService = new CoreServiceAdapter();
        aaa.packetService = new MockPacketService();
        aaa.hostService = new MockHostService();
        aaa.activate();
    }

    /**
     * Tears down the AAA application.
     */
    @After
    public void tearDown() {
        aaa.deactivate();
    }

    /**
     * Extracts the RADIUS packet from a packet sent by the supplicant.
     *
     * @param supplicantPacket packet sent by the supplicant
     * @return RADIUS packet
     * @throws DeserializationException if deserialization of the packet contents
     *         fails.
     */
    private RADIUS checkAndFetchRADIUSPacketFromSupplicant(Ethernet supplicantPacket)
            throws DeserializationException {
        assertThat(supplicantPacket, notNullValue());
        assertThat(supplicantPacket.getVlanID(), is(VlanId.UNTAGGED));
        assertThat(supplicantPacket.getSourceMAC().toString(), is(aaa.nasMacAddress));
        assertThat(supplicantPacket.getDestinationMAC().toString(), is(aaa.radiusMacAddress));

        assertThat(supplicantPacket.getPayload(), instanceOf(IPv4.class));
        IPv4 ipv4 = (IPv4) supplicantPacket.getPayload();
        assertThat(ipv4, notNullValue());
        assertThat(IpAddress.valueOf(ipv4.getSourceAddress()).toString(),
                   is(aaa.nasIpAddress.getHostAddress()));
        assertThat(IpAddress.valueOf(ipv4.getDestinationAddress()).toString(),
                   is(aaa.radiusIpAddress.getHostAddress()));

        assertThat(ipv4.getPayload(), instanceOf(UDP.class));
        UDP udp = (UDP) ipv4.getPayload();
        assertThat(udp, notNullValue());

        assertThat(udp.getPayload(), instanceOf(Data.class));
        Data data = (Data) udp.getPayload();
        RADIUS radius = RADIUS.deserializer()
                .deserialize(data.getData(), 0, data.getData().length);
        assertThat(radius, notNullValue());
        return radius;
    }

    /**
     * Checks the contents of a RADIUS packet being sent to the RADIUS server.
     *
     * @param radiusPacket packet to check
     * @param code expected code
     */
    private void checkRadiusPacket(Ethernet radiusPacket, byte code) {
        assertThat(radiusPacket.getVlanID(), is((short) 2));

        // TODO: These address values seem wrong, but are produced by the current AAA implementation
        assertThat(radiusPacket.getSourceMAC(), is(MacAddress.valueOf(1L)));
        assertThat(radiusPacket.getDestinationMAC(), is(serverMac));

        assertThat(radiusPacket.getPayload(), instanceOf(EAPOL.class));
        EAPOL eapol = (EAPOL) radiusPacket.getPayload();
        assertThat(eapol, notNullValue());

        assertThat(eapol.getEapolType(), is(EAPOL.EAPOL_PACKET));
        assertThat(eapol.getPayload(), instanceOf(EAP.class));
        EAP eap = (EAP) eapol.getPayload();
        assertThat(eap, notNullValue());
        assertThat(eap.getCode(), is(code));
    }

    /**
     * Fetches the sent packet at the given index. The requested packet
     * must be the last packet on the list.
     *
     * @param index index into sent packets array
     * @return packet
     */
    private Ethernet fetchPacket(int index) {
        assertThat(savedPackets.size(), is(index + 1));
        Ethernet eth = savedPackets.get(index);
        assertThat(eth, notNullValue());
        return eth;
    }

    /**
     * Tests the authentication path through the AAA application.
     *
     * @throws DeserializationException if packed deserialization fails.
     */
    @Test
    public void testAuthentication()  throws DeserializationException {

        // Our session id will be the device ID ("of:1") with the port ("1") concatenated
        String sessionId = "of:11";

        //  (1) Supplicant start up

        Ethernet startPacket = constructSupplicantStartPacket();
        sendPacket(startPacket);

        Ethernet responsePacket = fetchPacket(0);
        checkRadiusPacket(responsePacket, EAP.ATTR_IDENTITY);

        //  (2) Supplicant identify

        Ethernet identifyPacket = constructSupplicantIdentifyPacket(EAP.ATTR_IDENTITY);
        sendPacket(identifyPacket);

        Ethernet radiusIdentifyPacket = fetchPacket(1);

        RADIUS radiusAccessRequest = checkAndFetchRADIUSPacketFromSupplicant(radiusIdentifyPacket);
        assertThat(radiusAccessRequest, notNullValue());
        assertThat(radiusAccessRequest.getCode(), is(RADIUS.RADIUS_CODE_ACCESS_REQUEST));
        assertThat(new String(radiusAccessRequest.getAttribute(RADIUSAttribute.RADIUS_ATTR_USERNAME).getValue()),
                   is("user"));

        IpAddress nasIp =
                IpAddress.valueOf(IpAddress.Version.INET,
                                  radiusAccessRequest.getAttribute(RADIUSAttribute.RADIUS_ATTR_NAS_IP)
                                          .getValue());
        assertThat(nasIp.toString(), is(aaa.nasIpAddress.getHostAddress()));

        //  State machine should have been created by now

        StateMachine stateMachine =
                StateMachine.lookupStateMachineBySessionId(sessionId);
        assertThat(stateMachine, notNullValue());
        assertThat(stateMachine.state(), is(StateMachine.STATE_PENDING));

        // (3) RADIUS MD5 challenge

        Ethernet radiusCodeAccessChallengePacket =
                constructRADIUSCodeAccessChallengePacket(RADIUS.RADIUS_CODE_ACCESS_CHALLENGE, EAP.ATTR_MD5);
        sendPacket(radiusCodeAccessChallengePacket);

        Ethernet radiusChallengeMD5Packet = fetchPacket(2);
        checkRadiusPacket(radiusChallengeMD5Packet, EAP.ATTR_MD5);

        // (4) Supplicant MD5 response

        Ethernet md5RadiusPacket = constructSupplicantIdentifyPacket(EAP.ATTR_MD5);
        sendPacket(md5RadiusPacket);
        Ethernet supplicantMD5ResponsePacket = fetchPacket(3);
        RADIUS responseMd5RadiusPacket = checkAndFetchRADIUSPacketFromSupplicant(supplicantMD5ResponsePacket);
        assertThat(responseMd5RadiusPacket.getIdentifier(), is((byte) 1));
        assertThat(responseMd5RadiusPacket.getCode(), is(RADIUS.RADIUS_CODE_ACCESS_REQUEST));

        //  State machine should be in pending state

        assertThat(stateMachine, notNullValue());
        assertThat(stateMachine.state(), is(StateMachine.STATE_PENDING));

        // (5) RADIUS TLS Challenge

        Ethernet radiusCodeAccessChallengeTLSPacket =
                constructRADIUSCodeAccessChallengePacket(RADIUS.RADIUS_CODE_ACCESS_CHALLENGE, EAP.ATTR_TLS);
        sendPacket(radiusCodeAccessChallengeTLSPacket);

        Ethernet radiusChallengeTLSPacket = fetchPacket(4);
        checkRadiusPacket(radiusChallengeTLSPacket, EAP.ATTR_TLS);

        // (6) Supplicant TLS response

        Ethernet tlsRadiusPacket = constructSupplicantIdentifyPacket(EAP.ATTR_TLS);
        sendPacket(tlsRadiusPacket);
        Ethernet supplicantTLSResponsePacket = fetchPacket(5);
        RADIUS responseTLSRadiusPacket = checkAndFetchRADIUSPacketFromSupplicant(supplicantTLSResponsePacket);
        assertThat(responseTLSRadiusPacket.getIdentifier(), is((byte) 0));
        assertThat(responseTLSRadiusPacket.getCode(), is(RADIUS.RADIUS_CODE_ACCESS_REQUEST));

        // (7) RADIUS Success

        Ethernet successPacket =
                constructRADIUSCodeAccessChallengePacket(RADIUS.RADIUS_CODE_ACCESS_ACCEPT, EAP.SUCCESS);
        sendPacket(successPacket);
        Ethernet supplicantSuccessPacket = fetchPacket(6);

        checkRadiusPacket(supplicantSuccessPacket, EAP.SUCCESS);

        //  State machine should be in authorized state

        assertThat(stateMachine, notNullValue());
        assertThat(stateMachine.state(), is(StateMachine.STATE_AUTHORIZED));
    }

    /**
     * Tests the default configuration.
     */
    @Test
    public void testConfig() {
        assertThat(aaa.nasIpAddress.getHostAddress(), is(AAAConfig.DEFAULT_NAS_IP));
        assertThat(aaa.nasMacAddress, is(AAAConfig.DEFAULT_NAS_MAC));
        assertThat(aaa.radiusIpAddress.getHostAddress(), is(AAAConfig.DEFAULT_RADIUS_IP));
        assertThat(aaa.radiusMacAddress, is(AAAConfig.DEFAULT_RADIUS_MAC));
    }
}
