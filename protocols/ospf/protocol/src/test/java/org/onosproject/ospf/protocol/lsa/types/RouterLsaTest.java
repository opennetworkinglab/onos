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
package org.onosproject.ospf.protocol.lsa.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.subtypes.OspfLsaLink;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit test class for RouterLsa.
 */
public class RouterLsaTest {

    private RouterLsa routerLsa;
    private int result1;
    private OspfLsaLink ospflsaLink;
    private byte[] inputArray;
    private LsaHeader lsaHeader;
    private ChannelBuffer channelBuffer;
    private byte[] result2;
    private OspfLsaType result3;

    @Before
    public void setUp() throws Exception {
        routerLsa = new RouterLsa();
    }

    @After
    public void tearDown() throws Exception {
        routerLsa = null;
        ospflsaLink = null;
        inputArray = null;
        lsaHeader = null;
        channelBuffer = null;
        result2 = null;
        result3 = null;
    }


    /**
     * Tests virtualEndPoint() setter method.
     */
    @Test
    public void testSetVirtualEndPoint() throws Exception {
        routerLsa.setVirtualEndPoint(true);
        assertThat(routerLsa, is(notNullValue()));
    }


    /**
     * Tests isAsBoundaryRouter() setter method.
     */
    @Test
    public void testSetAsBoundaryRouter() throws Exception {
        routerLsa.setAsBoundaryRouter(true);
        assertThat(routerLsa, is(notNullValue()));
    }

    /**
     * Tests areaBorderRouter() setter method.
     */
    @Test
    public void testSetAreaBorderRouter() throws Exception {
        routerLsa.setAreaBorderRouter(true);
        assertThat(routerLsa, is(notNullValue()));
    }

    /**
     * Tests noLink() getter method.
     */
    @Test
    public void testGetNoLink() throws Exception {
        routerLsa.setNoLink(10);
        result1 = routerLsa.noLink();
        assertThat(result1, is(10));
    }

    /**
     * Tests noLink() setter method.
     */
    @Test
    public void testSetNoLink() throws Exception {
        routerLsa.setNoLink(10);
        result1 = routerLsa.noLink();
        assertThat(result1, is(10));
    }

    /**
     * Tests addRouterLink() method.
     */
    @Test
    public void testAddRouterLink() throws Exception {
        routerLsa.setNoLink(0);
        ospflsaLink = createOspfLsaLink();
        routerLsa.addRouterLink(ospflsaLink);
        routerLsa.incrementLinkNo();
        result1 = routerLsa.noLink();
        assertThat(result1, is(1));

    }


    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        ospflsaLink = createOspfLsaLink();
        routerLsa.addRouterLink(ospflsaLink);
        inputArray = createByteForRouterLsa();
        lsaHeader = createLsaHeader();
        routerLsa = new RouterLsa(lsaHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(inputArray);
        routerLsa.readFrom(channelBuffer);
        assertThat(routerLsa, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test(expected = Exception.class)
    public void testReadFrom1() throws Exception {
        byte[] temp = {0, 0, 0};
        ospflsaLink = createOspfLsaLink();
        routerLsa.addRouterLink(ospflsaLink);
        inputArray = temp;
        lsaHeader = createLsaHeader();
        routerLsa = new RouterLsa(lsaHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(inputArray);
        routerLsa.readFrom(channelBuffer);
        assertThat(routerLsa, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result2 = routerLsa.asBytes();
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests getLsaBodyAsByteArray() method.
     */
    @Test
    public void testGetLsaBodyAsByteArray() throws Exception {
        routerLsa.setAreaBorderRouter(true);
        routerLsa.setVirtualEndPoint(true);
        routerLsa.setAreaBorderRouter(true);
        ospflsaLink = createOspfLsaLink();
        routerLsa.addRouterLink(ospflsaLink);
        result2 = routerLsa.getLsaBodyAsByteArray();
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests getOspfLsaType() getter method.
     */
    @Test
    public void testGetOspfLsaType() throws Exception {
        routerLsa.setLsType(1);
        result3 = routerLsa.getOspfLsaType();
        assertThat(result3, is(OspfLsaType.ROUTER));
    }

    /**
     * Tests incrementLinkNo() method.
     */
    @Test
    public void testIncrementLinkNo() throws Exception {
        routerLsa.setNoLink(1);
        routerLsa.incrementLinkNo();
        assertThat(routerLsa.noLink(), is(2));
    }

    /**
     * Tests lsaHeader() method.
     */
    @Test
    public void testGetLsaHeader() throws Exception {
        lsaHeader = (LsaHeader) routerLsa.lsaHeader();
        assertThat(lsaHeader, instanceOf(RouterLsa.class));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(routerLsa.toString(), is(notNullValue()));

    }

    /**
     * Utility method used by junit methods.
     */
    private OspfLsaLink createOspfLsaLink() {
        ospflsaLink = new OspfLsaLink();
        ospflsaLink.setLinkId("10.226.165.164");
        ospflsaLink.setMetric(10);
        ospflsaLink.setTos(50);
        ospflsaLink.setLinkType(2);
        ospflsaLink.setLinkData("10.226.165.170");
        return ospflsaLink;
    }

    /**
     * Utility method used by junit methods.
     */
    private byte[] createByteForRouterLsa() {
        byte[] packet = {2, 1, 1, 52, -64, -88, 56, 1, -64, -88, 56, 1, 0, 100, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0, -64,
                -88, 56, 1, 0, 10, 1, 1, 0, 0, 0, 40, -64, -88, 56, 1, -64, -88, 56, 1, -64, -88, 56, 1, -64, -88, 56,
                1};
        return packet;
    }

    /**
     * Utility method used by junit methods.
     */
    private LsaHeader createLsaHeader() {
        lsaHeader = new LsaHeader();
        lsaHeader.setLsType(1);
        lsaHeader.setLsPacketLen(48);
        lsaHeader.setLsCheckSum(10);
        lsaHeader.setAge(4);
        lsaHeader.setLinkStateId("10.226.165.164");
        lsaHeader.setLsSequenceNo(250);
        lsaHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        lsaHeader.setOptions(2);
        return lsaHeader;
    }

    /**
     * Tests hashcode() method.
     */
    @Test
    public void testHashcode() throws Exception {

        result1 = routerLsa.hashCode();
        assertThat(result1, is(notNullValue()));

    }
}
