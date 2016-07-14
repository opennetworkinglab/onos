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

import java.util.Vector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for NetworkLsa.
 */
public class NetworkLsaTest {

    private static final Ip4Address LOCAL_ADDRESS = Ip4Address.valueOf("127.0.0.1");

    private Vector<String> attachedRouters = new Vector();
    private NetworkLsa networkLsa;
    private Ip4Address result;
    private Ip4Address inetAddres;
    private byte[] inputByteArray;
    private LsaHeader lsaHeader;
    private ChannelBuffer channelBuffer;
    private byte[] result1;
    private OspfLsaType ospflsaType;
    private int result2;

    @Before
    public void setUp() throws Exception {
        networkLsa = new NetworkLsa();
    }

    @After
    public void tearDown() throws Exception {
        networkLsa = null;
        attachedRouters = null;
        result = null;
        inetAddres = null;
        inputByteArray = null;
        lsaHeader = null;
        channelBuffer = null;
        result1 = null;
        ospflsaType = null;
    }

    /**
     * Tests networkMask() getter method.
     */
    @Test
    public void testGetNetworkMask() throws Exception {
        networkLsa.setNetworkMask(Ip4Address.valueOf("10.226.165.164"));
        result = networkLsa.networkMask();
        assertThat(result, is(Ip4Address.valueOf("10.226.165.164")));
    }

    /**
     * Tests networkMask() setter method.
     */
    @Test
    public void testSetNetworkMask() throws Exception {
        networkLsa.setNetworkMask(Ip4Address.valueOf("10.226.165.165"));
        result = networkLsa.networkMask();
        result = networkLsa.networkMask();
        assertThat(result, is(Ip4Address.valueOf("10.226.165.165")));
    }

    /**
     * Tests addAttachedRouter() getter method.
     */
    @Test
    public void testGetAttachedRouters() throws Exception {
        attachedRouters.add("1.1.1.1");
        networkLsa.addAttachedRouter(Ip4Address.valueOf("1.1.1.1"));
        assertThat(attachedRouters, is(notNullValue()));
    }

    /**
     * Tests addAttachedRouter() setter method.
     */
    @Test
    public void testSetAttachedRouters() throws Exception {
        attachedRouters.add("1.1.1.1");
        networkLsa.addAttachedRouter(Ip4Address.valueOf("1.1.1.1"));
        assertThat(attachedRouters, is(notNullValue()));
    }

    /**
     * Tests addAttachedRouter() method.
     */
    @Test
    public void testAddAttachedRouter() throws Exception {
        networkLsa.addAttachedRouter(LOCAL_ADDRESS);
        networkLsa.addAttachedRouter(LOCAL_ADDRESS);
        assertThat(networkLsa, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */

    @Test
    public void testReadFrom() throws Exception {
        inputByteArray = createByteForNetworkLsa();
        lsaHeader = createLsaHeader();
        networkLsa = new NetworkLsa(lsaHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(inputByteArray);
        networkLsa.readFrom(channelBuffer);
        assertThat(networkLsa, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test(expected = Exception.class)
    public void testReadFrom1() throws Exception {
        byte[] temp = {0, 0, 0};
        inputByteArray = temp;
        lsaHeader = createLsaHeader();
        networkLsa = new NetworkLsa(lsaHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(inputByteArray);
        networkLsa.readFrom(channelBuffer);
        assertThat(networkLsa, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test(expected = Exception.class)
    public void testAsBytes() throws Exception {
        result1 = networkLsa.asBytes();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests getLsaBodyAsByteArray() method.
     */
    @Test(expected = Exception.class)
    public void testGetLsaBodyAsByteArray() throws Exception {
        networkLsa.addAttachedRouter(Ip4Address.valueOf("1.1.1.1"));
        networkLsa.addAttachedRouter(Ip4Address.valueOf("2.2.2.2"));
        networkLsa.addAttachedRouter(Ip4Address.valueOf("3.3.3.3"));
        result1 = networkLsa.getLsaBodyAsByteArray();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests getLsaBodyAsByteArray() method.
     */
    @Test
    public void testGetLsaBodyAsByteArray1() throws Exception {
        networkLsa.setNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        networkLsa.addAttachedRouter(Ip4Address.valueOf("1.1.1.1"));
        networkLsa.addAttachedRouter(Ip4Address.valueOf("2.2.2.2"));
        networkLsa.addAttachedRouter(Ip4Address.valueOf("3.3.3.3"));
        result1 = networkLsa.getLsaBodyAsByteArray();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests getOspfLsaType() getter method.
     */
    @Test
    public void testGetOspfLsaType() throws Exception {
        networkLsa.setLsType(2);
        ospflsaType = networkLsa.getOspfLsaType();
        assertThat(ospflsaType, is(OspfLsaType.NETWORK));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashcode() throws Exception {

        result2 = networkLsa.hashCode();
        assertThat(result2, is(notNullValue()));

    }

    /**
     * Utility method used by junit methods.
     */
    private byte[] createByteForNetworkLsa() {
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
        lsaHeader.setLsType(2);
        lsaHeader.setLsPacketLen(48);
        lsaHeader.setLsCheckSum(10);
        lsaHeader.setAge(4);
        lsaHeader.setLinkStateId("10.226.165.164");
        lsaHeader.setLsSequenceNo(250);
        lsaHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        lsaHeader.setOptions(2);
        return lsaHeader;
    }
}
