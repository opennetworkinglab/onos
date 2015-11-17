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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.packet.EAP;
import org.onlab.packet.EAPOL;
import org.onlab.packet.Ethernet;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Set of tests of the ONOS application component. These use an existing RADIUS
 * server and sends live packets over the network to it.
 */
@Ignore ("This should not be run as part of the standard build")
public class AaaIntegrationTest extends AaaTestBase {

    private AaaManager aaa;

    /**
     * Mocks the network config registry.
     */
    @SuppressWarnings("unchecked")
    static final class TestNetworkConfigRegistry
            extends NetworkConfigRegistryAdapter {
        @Override
        public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
            return (C) new AaaConfig();
        }
    }

    /**
     * Sets up the services required by the AAA application.
     */
    @Before
    public void setUp() {
        aaa = new AaaManager();
        aaa.netCfgService = new TestNetworkConfigRegistry();
        aaa.coreService = new CoreServiceAdapter();
        aaa.packetService = new MockPacketService();
        aaa.activate();
    }

    /**
     * Fetches the sent packet at the given index. The requested packet
     * must be the last packet on the list.
     *
     * @param index index into sent packets array
     * @return packet
     */
    private Ethernet fetchPacket(int index) {
        for (int iteration = 0; iteration < 20; iteration++) {
            if (savedPackets.size() > index) {
                return (Ethernet) savedPackets.get(index);
            } else {
                try {
                    Thread.sleep(250);
                } catch (Exception ex) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Tests the authentication path through the AAA application by sending
     * packets to the RADIUS server and checking the state machine
     * transitions.
     *
     * @throws Exception when an unhandled error occurs
     */
    @Test
    public void testAuthentication()  throws Exception {

        //  (1) Supplicant start up

        Ethernet startPacket = constructSupplicantStartPacket();
        sendPacket(startPacket);

        Ethernet responsePacket = fetchPacket(0);
        assertThat(responsePacket, notNullValue());
        checkRadiusPacket(aaa, responsePacket, EAP.REQUEST);

        //  (2) Supplicant identify

        Ethernet identifyPacket = constructSupplicantIdentifyPacket(null, EAP.ATTR_IDENTITY, (byte) 1, null);
        sendPacket(identifyPacket);

        //  State machine should have been created by now

        StateMachine stateMachine =
                StateMachine.lookupStateMachineBySessionId(SESSION_ID);
        assertThat(stateMachine, notNullValue());
        assertThat(stateMachine.state(), is(StateMachine.STATE_PENDING));

        // (3) RADIUS MD5 challenge

        Ethernet radiusChallengeMD5Packet = fetchPacket(1);
        assertThat(radiusChallengeMD5Packet, notNullValue());
        checkRadiusPacket(aaa, radiusChallengeMD5Packet, EAP.REQUEST);


        // (4) Supplicant MD5 response

        Ethernet md5RadiusPacket =
                constructSupplicantIdentifyPacket(stateMachine,
                                                  EAP.ATTR_MD5,
                                                  stateMachine.challengeIdentifier(),
                                                  radiusChallengeMD5Packet);
        sendPacket(md5RadiusPacket);


        // (5) RADIUS Success

        Ethernet successRadiusPacket = fetchPacket(2);
        assertThat(successRadiusPacket, notNullValue());
        EAPOL successEapol = (EAPOL) successRadiusPacket.getPayload();
        EAP successEap = (EAP) successEapol.getPayload();
        assertThat(successEap.getCode(), is(EAP.SUCCESS));

        //  State machine should be in authorized state

        assertThat(stateMachine, notNullValue());
        assertThat(stateMachine.state(), is(StateMachine.STATE_AUTHORIZED));

    }

}

