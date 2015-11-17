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
package org.onosproject.aaa;

import org.onlab.packet.BasePacket;
import org.onlab.packet.EAP;
import org.onlab.packet.EAPOL;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketServiceAdapter;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.onosproject.net.NetTestTools.connectPoint;

/**
 * Common methods for AAA app testing.
 */
public class AaaTestBase {

    MacAddress clientMac = MacAddress.valueOf("1a:1a:1a:1a:1a:1a");
    MacAddress serverMac = MacAddress.valueOf("2a:2a:2a:2a:2a:2a");

    // Our session id will be the device ID ("of:1") with the port ("1") concatenated
    static final String SESSION_ID = "of:11";

    List<BasePacket> savedPackets = new LinkedList<>();
    PacketProcessor packetProcessor;

    /**
     * Saves the given packet onto the saved packets list.
     *
     * @param packet packet to save
     */
    void savePacket(BasePacket packet) {
        savedPackets.add(packet);
    }

    /**
     * Keeps a reference to the PacketProcessor and saves the OutboundPackets.
     */
    class MockPacketService extends PacketServiceAdapter {

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
    final class TestPacketContext extends DefaultPacketContext {

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
     * Sends an Ethernet packet to the process method of the Packet Processor.
     *
     * @param reply Ethernet packet
     */
    void sendPacket(Ethernet reply) {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(reply.serialize());
        InboundPacket inPacket = new DefaultInboundPacket(connectPoint("1", 1),
                                                          reply,
                                                          byteBuffer);

        PacketContext context = new TestPacketContext(127L, inPacket, null, false);
        packetProcessor.process(context);
    }

    /**
     * Constructs an Ethernet packet containing identification payload.
     *
     * @return Ethernet packet
     */
    Ethernet constructSupplicantIdentifyPacket(StateMachine stateMachine,
                                                       byte type,
                                                       byte id,
                                                       Ethernet radiusChallenge)
            throws Exception {
        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(clientMac.toBytes());
        eth.setSourceMACAddress(serverMac.toBytes());
        eth.setEtherType(EthType.EtherType.EAPOL.ethType().toShort());
        eth.setVlanID((short) 2);

        String username = "testuser";
        byte[] data = username.getBytes();


        if (type == EAP.ATTR_MD5) {
            String password = "testpassword";
            EAPOL eapol = (EAPOL) radiusChallenge.getPayload();
            EAP eap = (EAP) eapol.getPayload();

            byte[] identifier = new byte[password.length() + eap.getData().length];

            identifier[0] = stateMachine.challengeIdentifier();
            System.arraycopy(password.getBytes(), 0, identifier, 1, password.length());
            System.arraycopy(eap.getData(), 1, identifier, 1 + password.length(), 16);

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(identifier);
            data = new byte[17];
            data[0] = (byte) 16;
            System.arraycopy(hash, 0, data, 1, 16);
        }
        EAP eap = new EAP(EAP.RESPONSE, (byte) 1, type,
                          data);
        eap.setIdentifier(id);

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
     * Constructs an Ethernet packet containing a EAPOL_START Payload.
     *
     * @return Ethernet packet
     */
    Ethernet constructSupplicantStartPacket() {
        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(clientMac.toBytes());
        eth.setSourceMACAddress(serverMac.toBytes());
        eth.setEtherType(EthType.EtherType.EAPOL.ethType().toShort());
        eth.setVlanID((short) 2);

        EAP eap = new EAP(EAPOL.EAPOL_START, (byte) 2, EAPOL.EAPOL_START, null);

        // eapol header
        EAPOL eapol = new EAPOL();
        eapol.setEapolType(EAPOL.EAPOL_START);
        eapol.setPacketLength(eap.getLength());

        // eap part
        eapol.setPayload(eap);

        eth.setPayload(eapol);
        eth.setPad(true);
        return eth;
    }

    /**
     * Checks the contents of a RADIUS packet being sent to the RADIUS server.
     *
     * @param radiusPacket packet to check
     * @param code expected code
     */
    void checkRadiusPacket(AaaManager aaaManager, Ethernet radiusPacket, byte code) {

        assertThat(radiusPacket.getSourceMAC(),
                   is(MacAddress.valueOf(aaaManager.nasMacAddress)));
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
}
