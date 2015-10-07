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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.BasePacket;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.EAP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.RADIUS;
import org.onlab.packet.RADIUSAttribute;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;

import com.google.common.base.Charsets;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Set of tests of the ONOS application component.
 */
public class AAATest extends AAATestBase {

    static final String BAD_IP_ADDRESS = "198.51.100.0";

    private AAA aaa;

    class AAAWithoutRadiusServer extends AAA {
        protected void sendRADIUSPacket(RADIUS radiusPacket) {
            savePacket(radiusPacket);
        }
    }

    /**
     * Mocks the AAAConfig class to force usage of an unroutable address for the
     * RADIUS server.
     */
    static class MockAAAConfig extends AAAConfig {
        @Override
        public InetAddress radiusIp() {
            try {
                return InetAddress.getByName(BAD_IP_ADDRESS);
            } catch (UnknownHostException ex) {
                // can't happen
                throw new IllegalStateException(ex);
            }
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
            AAAConfig aaaConfig = new MockAAAConfig();
            return (C) aaaConfig;
        }
    }

    /**
     * Constructs an Ethernet packet containing a RADIUS challenge
     * packet.
     *
     * @param challengeCode code to use in challenge packet
     * @param challengeType type to use in challenge packet
     * @return Ethernet packet
     */
    private RADIUS constructRADIUSCodeAccessChallengePacket(byte challengeCode, byte challengeType) {

        String challenge = "12345678901234567";

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

        return radius;
    }

    /**
     * Sets up the services required by the AAA application.
     */
    @Before
    public void setUp() {
        aaa = new AAAWithoutRadiusServer();
        aaa.netCfgService = new TestNetworkConfigRegistry();
        aaa.coreService = new CoreServiceAdapter();
        aaa.packetService = new MockPacketService();
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
     * @param radius RADIUS packet sent by the supplicant
     * @throws DeserializationException if deserialization of the packet contents
     *         fails.
     */
    private void checkRADIUSPacketFromSupplicant(RADIUS radius)
            throws DeserializationException {
        assertThat(radius, notNullValue());

        EAP eap = radius.decapsulateMessage();
        assertThat(eap, notNullValue());
    }

    /**
     * Fetches the sent packet at the given index. The requested packet
     * must be the last packet on the list.
     *
     * @param index index into sent packets array
     * @return packet
     */
    private BasePacket fetchPacket(int index) {
        BasePacket packet = savedPackets.get(index);
        assertThat(packet, notNullValue());
        return packet;
    }

    /**
     * Tests the authentication path through the AAA application.
     *
     * @throws DeserializationException if packed deserialization fails.
     */
    @Test
    public void testAuthentication()  throws Exception {

        //  (1) Supplicant start up

        Ethernet startPacket = constructSupplicantStartPacket();
        sendPacket(startPacket);

        Ethernet responsePacket = (Ethernet) fetchPacket(0);
        checkRadiusPacket(aaa, responsePacket, EAP.ATTR_IDENTITY);

        //  (2) Supplicant identify

        Ethernet identifyPacket = constructSupplicantIdentifyPacket(null, EAP.ATTR_IDENTITY, (byte) 1, null);
        sendPacket(identifyPacket);

        RADIUS radiusIdentifyPacket = (RADIUS) fetchPacket(1);

        checkRADIUSPacketFromSupplicant(radiusIdentifyPacket);

        assertThat(radiusIdentifyPacket.getCode(), is(RADIUS.RADIUS_CODE_ACCESS_REQUEST));
        assertThat(new String(radiusIdentifyPacket.getAttribute(RADIUSAttribute.RADIUS_ATTR_USERNAME).getValue()),
                   is("testuser"));

        IpAddress nasIp =
                IpAddress.valueOf(IpAddress.Version.INET,
                                  radiusIdentifyPacket.getAttribute(RADIUSAttribute.RADIUS_ATTR_NAS_IP)
                                          .getValue());
        assertThat(nasIp.toString(), is(aaa.nasIpAddress.getHostAddress()));

        //  State machine should have been created by now

        StateMachine stateMachine =
                StateMachine.lookupStateMachineBySessionId(SESSION_ID);
        assertThat(stateMachine, notNullValue());
        assertThat(stateMachine.state(), is(StateMachine.STATE_PENDING));

        // (3) RADIUS MD5 challenge

        RADIUS radiusCodeAccessChallengePacket =
                constructRADIUSCodeAccessChallengePacket(RADIUS.RADIUS_CODE_ACCESS_CHALLENGE, EAP.ATTR_MD5);
        aaa.radiusListener.handleRadiusPacket(radiusCodeAccessChallengePacket);

        Ethernet radiusChallengeMD5Packet = (Ethernet) fetchPacket(2);
        checkRadiusPacket(aaa, radiusChallengeMD5Packet, EAP.ATTR_MD5);

        // (4) Supplicant MD5 response

        Ethernet md5RadiusPacket =
                constructSupplicantIdentifyPacket(stateMachine,
                                                  EAP.ATTR_MD5,
                                                  stateMachine.challengeIdentifier(),
                                                  radiusChallengeMD5Packet);
        sendPacket(md5RadiusPacket);

        RADIUS responseMd5RadiusPacket = (RADIUS) fetchPacket(3);

        checkRADIUSPacketFromSupplicant(responseMd5RadiusPacket);
        assertThat(responseMd5RadiusPacket.getIdentifier(), is((byte) 0));
        assertThat(responseMd5RadiusPacket.getCode(), is(RADIUS.RADIUS_CODE_ACCESS_REQUEST));

        //  State machine should be in pending state

        assertThat(stateMachine, notNullValue());
        assertThat(stateMachine.state(), is(StateMachine.STATE_PENDING));

        // (5) RADIUS Success

        RADIUS successPacket =
                constructRADIUSCodeAccessChallengePacket(RADIUS.RADIUS_CODE_ACCESS_ACCEPT, EAP.SUCCESS);
        aaa.radiusListener.handleRadiusPacket((successPacket));
        Ethernet supplicantSuccessPacket = (Ethernet) fetchPacket(4);

        checkRadiusPacket(aaa, supplicantSuccessPacket, EAP.SUCCESS);

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
        assertThat(aaa.radiusIpAddress.getHostAddress(), is(BAD_IP_ADDRESS));
        assertThat(aaa.radiusMacAddress, is(AAAConfig.DEFAULT_RADIUS_MAC));
    }
}
