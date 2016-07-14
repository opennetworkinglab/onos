/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.isis.controller.impl;

import org.easymock.EasyMock;
import org.jboss.netty.channel.Channel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.isis.controller.IsisNetworkType;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for IsisHelloPduSender.
 */
public class IsisHelloPduSenderTest {
    private final String systemId = "1234.1234.1234";
    private final String areaId = "490001";
    private final String circuitId = "0";
    private final String lanId = "0000.0000.0000.00";
    private Channel channel;
    private DefaultIsisInterface isisInterface;
    private DefaultIsisInterface isisInterface1;
    private IsisHelloPduSender isisHelloPduSender;
    private IsisHelloPduSender isisHelloPduSender1;
    private Ip4Address interfaceAddress = Ip4Address.valueOf("10.10.10.10");

    @Before
    public void setUp() throws Exception {
        channel = EasyMock.createNiceMock(Channel.class);
        isisInterface = new DefaultIsisInterface();
        isisInterface1 = new DefaultIsisInterface();
    }

    @After
    public void tearDown() throws Exception {
        channel = null;
        isisInterface = null;
    }

    /**
     * Tests run() method.
     */
    @Test(expected = Exception.class)
    public void testRun() throws Exception {
        isisInterface.setNetworkType(IsisNetworkType.P2P);
        isisInterface.setCircuitId(circuitId);
        isisInterface.setSystemId(systemId);
        isisInterface.setAreaAddress("490001");
        isisInterface.setInterfaceIpAddress(interfaceAddress);
        isisHelloPduSender = new IsisHelloPduSender(channel, isisInterface);
        isisHelloPduSender.run();
        assertThat(isisHelloPduSender, is(notNullValue()));

        isisInterface1.setNetworkType(IsisNetworkType.BROADCAST);
        isisInterface1.setCircuitId(circuitId);
        isisInterface1.setSystemId(systemId);
        isisInterface1.setAreaAddress(areaId);
        isisInterface1.setInterfaceIpAddress(interfaceAddress);
        isisInterface1.setReservedPacketCircuitType(1);
        isisInterface1.setL1LanId(lanId);
        isisHelloPduSender1 = new IsisHelloPduSender(channel, isisInterface1);
        isisHelloPduSender1.run();
        assertThat(isisHelloPduSender1, is(notNullValue()));

        isisInterface1.setNetworkType(IsisNetworkType.BROADCAST);
        isisInterface1.setCircuitId(circuitId);
        isisInterface1.setSystemId(systemId);
        isisInterface1.setAreaAddress(areaId);
        isisInterface1.setInterfaceIpAddress(interfaceAddress);
        isisInterface1.setReservedPacketCircuitType(2);
        isisInterface1.setL2LanId(lanId);
        isisHelloPduSender1 = new IsisHelloPduSender(channel, isisInterface1);
        isisHelloPduSender1.run();
        assertThat(isisHelloPduSender1, is(notNullValue()));

        isisInterface1.setNetworkType(IsisNetworkType.BROADCAST);
        isisInterface1.setCircuitId(circuitId);
        isisInterface1.setSystemId(systemId);
        isisInterface1.setAreaAddress(areaId);
        isisInterface1.setInterfaceIpAddress(interfaceAddress);
        isisInterface1.setReservedPacketCircuitType(3);
        isisInterface1.setL1LanId(lanId);
        isisInterface1.setL2LanId(lanId);
        isisHelloPduSender1 = new IsisHelloPduSender(channel, isisInterface1);
        isisHelloPduSender1.run();
        assertThat(isisHelloPduSender1, is(notNullValue()));
    }
}