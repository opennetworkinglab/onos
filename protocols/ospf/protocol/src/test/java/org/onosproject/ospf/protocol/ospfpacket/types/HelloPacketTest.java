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
package org.onosproject.ospf.protocol.ospfpacket.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfPacketType;
import org.onosproject.ospf.protocol.ospfpacket.OspfPacketHeader;

import java.util.Vector;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for HelloPacket.
 */
public class HelloPacketTest {

    private boolean result1;
    private OspfPacketType ospfPacketType;
    private OspfPacketHeader ospfPacketHeader;
    private HelloPacket helloPacket;
    private Vector<String> neighborAddress = new Vector();
    private Ip4Address result;
    private int result2;
    private byte[] packet;
    private ChannelBuffer channelBuffer;
    private byte[] result3;

    @Before
    public void setUp() throws Exception {
        helloPacket = new HelloPacket();
        helloPacket.setAuthType(1);
        helloPacket.setOspftype(2);
        helloPacket.setRouterId(Ip4Address.valueOf("10.226.165.164"));
        helloPacket.setAreaId(Ip4Address.valueOf("10.226.165.100"));
        helloPacket.setChecksum(201);
        helloPacket.setAuthentication(2);
        helloPacket.setOspfPacLength(48);
        helloPacket.setOspfVer(2);

    }

    @After
    public void tearDown() throws Exception {
        helloPacket = null;
        result = null;
        ospfPacketType = null;
        ospfPacketHeader = null;
        packet = null;
        channelBuffer = null;
        result3 = null;
    }

    /**
     * Tests networkMask() getter method.
     */
    @Test
    public void testGetNetworkMask() throws Exception {
        helloPacket.setNetworkMask(Ip4Address.valueOf("10.226.165.164"));
        result = helloPacket.networkMask();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(Ip4Address.valueOf("10.226.165.164")));
    }

    /**
     * Tests networkMask() setter method.
     */
    @Test
    public void testSetNetworkMask() throws Exception {
        helloPacket.setNetworkMask(Ip4Address.valueOf("10.226.165.164"));
        result = helloPacket.networkMask();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(Ip4Address.valueOf("10.226.165.164")));
    }

    /**
     * Tests bdr() setter method.
     */
    @Test
    public void testSetBdr() throws Exception {
        helloPacket.setBdr(Ip4Address.valueOf("10.226.165.166"));
        result = helloPacket.bdr();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(Ip4Address.valueOf("10.226.165.166")));
    }

    /**
     * Tests dr() getter method.
     */
    @Test
    public void testGetDr() throws Exception {
        helloPacket.setDr(Ip4Address.valueOf("10.226.165.167"));
        result = helloPacket.dr();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(Ip4Address.valueOf("10.226.165.167")));
    }

    /**
     * Tests dr() setter method.
     */
    @Test
    public void testSetDr() throws Exception {
        helloPacket.setDr(Ip4Address.valueOf("10.226.165.167"));
        result = helloPacket.dr();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(Ip4Address.valueOf("10.226.165.167")));
    }

    /**
     * Tests addNeighbor() method.
     */
    @Test
    public void testAddNeighbor() throws Exception {
        helloPacket.addNeighbor(Ip4Address.valueOf("10.226.165.170"));
        result1 = helloPacket.containsNeighbour(Ip4Address.valueOf("10.226.165.170"));
        assertThat(result1, is(true));
    }

    /**
     * Tests containsNeighbour() method.
     */
    @Test
    public void testContainsNeighbour() throws Exception {
        helloPacket.addNeighbor(Ip4Address.valueOf("10.226.165.200"));
        result1 = helloPacket.containsNeighbour(Ip4Address.valueOf("10.226.165.200"));
        assertThat(result1, is(true));
    }


    /**
     * Tests options() getter  method.
     */
    @Test
    public void testGetOptions() throws Exception {
        helloPacket.setOptions(10);
        result2 = helloPacket.options();
        assertThat(result2, is(notNullValue()));
        assertThat(result2, is(10));
    }

    /**
     * Tests options() setter  method.
     */
    @Test
    public void testSetOptions() throws Exception {
        helloPacket.setOptions(11);
        result2 = helloPacket.options();
        assertThat(result2, is(notNullValue()));
        assertThat(result2, is(11));
    }

    /**
     * Tests routerPriority() getter  method.
     */
    @Test
    public void testGetRouterPriority() throws Exception {
        helloPacket.setRouterPriority(1);
        result2 = helloPacket.routerPriority();
        assertThat(result2, is(notNullValue()));
        assertThat(result2, is(1));
    }

    /**
     * Tests routerPriority() setter  method.
     */
    @Test
    public void testSetRouterPriority() throws Exception {
        helloPacket.setRouterPriority(2);
        result2 = helloPacket.routerPriority();
        assertThat(result2, is(notNullValue()));
        assertThat(result2, is(2));
    }

    /**
     * Tests helloInterval() getter  method.
     */
    @Test
    public void testGetHelloInterval() throws Exception {
        helloPacket.setHelloInterval(10);
        result2 = helloPacket.helloInterval();
        assertThat(result2, is(notNullValue()));
        assertThat(result2, is(10));
    }

    /**
     * Tests helloInterval() setter  method.
     */
    @Test
    public void testSetHelloInterval() throws Exception {
        helloPacket.setHelloInterval(10);
        result2 = helloPacket.helloInterval();
        assertThat(result2, is(notNullValue()));
        assertThat(result2, is(10));
    }

    /**
     * Tests routerDeadInterval() getter  method.
     */
    @Test
    public void testGetRouterDeadInterval() throws Exception {
        helloPacket.setRouterDeadInterval(50);
        result2 = helloPacket.routerDeadInterval();
        assertThat(result2, is(notNullValue()));
        assertThat(result2, is(50));
    }

    /**
     * Tests routerDeadInterval() setter  method.
     */
    @Test
    public void testSetRouterDeadInterval() throws Exception {
        helloPacket.setRouterDeadInterval(50);
        result2 = helloPacket.routerDeadInterval();
        assertThat(result2, is(notNullValue()));
        assertThat(result2, is(50));
    }

    /**
     * Tests ospfMessageType() getter  method.
     */
    @Test
    public void testGetOspfMessageType() throws Exception {
        ospfPacketType = helloPacket.ospfMessageType();
        assertThat(ospfPacketType, is(notNullValue()));
        assertThat(ospfPacketType, is(OspfPacketType.HELLO));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        ospfPacketHeader = new OspfPacketHeader();
        ospfPacketHeader.setAreaId(Ip4Address.valueOf("1.1.1.1"));
        ospfPacketHeader.setAuthentication(0);
        ospfPacketHeader.setAuthType(0);
        ospfPacketHeader.setChecksum(12345);
        ospfPacketHeader.setDestinationIp(Ip4Address.valueOf("10.10.10.10"));
        ospfPacketHeader.setOspfPacLength(56);
        ospfPacketHeader.setOspftype(1);
        ospfPacketHeader.setOspfVer(2);
        ospfPacketHeader.setRouterId(Ip4Address.valueOf("2.2.2.2"));
        ospfPacketHeader.setSourceIp(Ip4Address.valueOf("3.3.3.3"));
        packet = createByteForHelloPacket();
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        helloPacket.readFrom(channelBuffer);
        assertThat(helloPacket, is(notNullValue()));
        assertThat(helloPacket.ospfMessageType(), is(OspfPacketType.HELLO));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result3 = helloPacket.asBytes();
        assertThat(result3, is(notNullValue()));
    }

    /**
     * Tests getHelloHeaderAsByteArray() method.
     */
    @Test
    public void testGetHelloHeaderAsByteArray() throws Exception {
        result3 = helloPacket.getHelloHeaderAsByteArray();
        assertThat(result3, is(notNullValue()));
    }

    /**
     * Tests getHelloBodyAsByteArray() method.
     */
    @Test
    public void testGetHelloBodyAsByteArray() throws Exception {
        neighborAddress.add("10.226.165.100");
        result3 = helloPacket.getHelloBodyAsByteArray();
        assertThat(result3, is(notNullValue()));
    }

    /**
     * Tests getHelloBodyAsByteArray() method.
     */
    @Test
    public void testReadHelloBody() throws Exception {
        helloPacket.getHelloBodyAsByteArray();
        assertThat(helloPacket, is(notNullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(helloPacket.toString(), is(notNullValue()));
    }

    /**
     * Utility method used by junit methods.
     */
    private byte[] createByteForHelloPacket() {
        byte[] helloPacket = {2, 1, 0, 44, -64, -88, -86, 8, 0, 0, 0, 1, 39, 59, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, -1, -1, -1, 0, 0, 10, 2, 1, 0, 0, 0, 40, -64, -88, -86, 8, 0, 0, 0, 0};

        return helloPacket;
    }
}