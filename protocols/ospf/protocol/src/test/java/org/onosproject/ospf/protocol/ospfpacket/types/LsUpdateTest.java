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
import org.onosproject.ospf.controller.OspfLsa;
import org.onosproject.ospf.controller.OspfPacketType;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.lsa.types.AsbrSummaryLsa;
import org.onosproject.ospf.protocol.lsa.types.ExternalLsa;
import org.onosproject.ospf.protocol.lsa.types.NetworkLsa;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa10;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa11;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa9;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;
import org.onosproject.ospf.protocol.lsa.types.SummaryLsa;
import org.onosproject.ospf.protocol.ospfpacket.OspfPacketHeader;

import java.util.List;
import java.util.Vector;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for LsUpdate.
 */
public class LsUpdateTest {

    private final byte[] packet1 = {0, 0, 0, 2, 0, 10, 2, 1, 7, 7, 7, 7, 7, 7, 7, 7,
            -128, 0, 0, 2, 46, -126, 0, 48, 0, 0, 0, 2, 1, 1, 1, 1, 10, 10, 10, 7, 1,
            0, 0, 10, 10, 10, 10, 0, -1, -1, -1, 0, 3, 0, 0, 10, 0, 10, 66, 10, 1, 0,
            0, 1, 7, 7, 7, 7, -128, 0, 0, 1, -64, 79, 0, 116, 0, 1, 0, 4, 0, 0, 0, 0,
            0, 2, 0, 84, 0, 1, 0, 1, 1, 0, 0, 0, 0, 2, 0, 4, 10, 10, 10, 0, 0, 5, 0,
            4, 0, 0, 0, 0, 0, 6, 0, 4, 73, -104, -106, -128, 0, 7, 0, 4, 73, -104, -106,
            -128, 0, 8, 0, 32, 73, -104, -106, -128, 73, -104, -106, -128, 73, -104, -106,
            -128, 73, -104, -106, -128, 73, -104, -106, -128, 73, -104, -106, -128, 73,
            -104, -106, -128, 73, -104, -106, -128, 0, 9, 0, 4, 0, 0, 0, 0};
    private final byte[] packet3 = {0, 0, 0, 1, 0, 100, 2, 10, 1, 0, 0, 1, 9, 9, 9, 9,
            -128, 0, 0, 1, -7, 62, 0, -124, 0, 2, 0, 108, 0, 1, 0, 1, 2, 0, 0, 0, 0, 2,
            0, 4, -64, -88, 7, -91, 0, 3, 0, 4, -64, -88, 7, -91, 0, 4, 0, 4, 0, 0, 0,
            0, 0, 5, 0, 4, 0, 0, 0, 1, 0, 6, 0, 4, 0, 0, 0, 0, 0, 7, 0, 4, 0, 0, 0, 0,
            0, 8, 0, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 4, 0, 0, 0, 0, -128, 2, 0, 4, 0,
            0, 0, 1};
    private final byte[] packet2 = {0, 0,
            0, 1, 0, 1, 2, 2, -64, -88,
            -86, 8, -64, -88, -86, 8, -128, 0, 0, 1, 55, -73, 0, 32, -1, -1, -1, 0, -64,
            -88, -86, 3, -64, -88, -86, 8};
    private final byte[] packet4 = {0, 0, 0, 1, 0, 100, 2, 9, 1, 0, 0, 1, 9, 9, 9, 9, -128,
            0, 0, 1, -7, 62, 0, -124, 0, 2, 0, 108, 0, 1, 0, 1, 2, 0, 0, 0, 0, 2, 0, 4, -64,
            -88, 7, -91, 0, 3, 0, 4, -64, -88, 7, -91, 0, 4, 0, 4, 0, 0, 0, 0, 0, 5, 0, 4, 0,
            0, 0, 1, 0, 6, 0, 4, 0, 0, 0, 0, 0, 7, 0, 4, 0, 0, 0, 0, 0, 8, 0, 32, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 9, 0, 4, 0, 0, 0, 0, -128, 2, 0, 4, 0, 0, 0, 1};

    private final byte[] packet5 = {0, 0, 0, 1, 0, 100, 2, 11, 1, 0, 0, 1, 9, 9, 9, 9, -128, 0,
            0, 1, -7, 62, 0, -124, 0, 2, 0, 108, 0, 1, 0, 1, 2, 0, 0, 0, 0, 2, 0, 4, -64, -88,
            7, -91, 0, 3, 0, 4, -64, -88, 7, -91, 0, 4, 0, 4, 0, 0, 0, 0, 0, 5, 0, 4, 0, 0, 0,
            1, 0, 6, 0, 4, 0, 0, 0, 0, 0, 7, 0, 4, 0, 0, 0, 0, 0, 8, 0, 32, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            9, 0, 4, 0, 0, 0, 0, -128, 2, 0, 4, 0, 0, 0, 1};
    private final byte[] packet6 = {0, 0, 0, 1, 0, 100, 2, 3, 1, 0, 0, 1, 9, 9, 9, 9, -128,
            0, 0, 1, -7, 62, 0, -124, 0, 2, 0, 108, 0, 1, 0, 1, 2, 0, 0, 0, 0, 2, 0, 4, -64,
            -88, 7, -91, 0, 3, 0, 4, -64, -88, 7, -91, 0, 4, 0, 4, 0, 0, 0, 0, 0, 5, 0, 4,
            0, 0, 0, 1, 0, 6, 0, 4, 0, 0, 0, 0, 0, 7, 0, 4, 0, 0, 0, 0, 0, 8, 0, 32, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 9, 0, 4, 0, 0, 0, 0, -128, 2, 0, 4, 0, 0, 0, 1};
    private final byte[] packet7 = {0, 0, 0, 1, 0, 100, 2, 4, 1, 0, 0, 1, 9, 9, 9, 9, -128,
            0, 0, 1, -7, 62, 0, -124, 0, 2, 0, 108, 0, 1, 0, 1, 2, 0, 0, 0, 0, 2, 0, 4, -64,
            -88, 7, -91, 0, 3, 0, 4, -64, -88, 7, -91, 0, 4, 0, 4, 0, 0, 0, 0, 0, 5, 0, 4, 0,
            0, 0, 1, 0, 6, 0, 4, 0, 0, 0, 0, 0, 7, 0, 4, 0, 0, 0, 0, 0, 8, 0, 32, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 9, 0, 4, 0, 0, 0, 0, -128, 2, 0, 4, 0, 0, 0, 1};

    private final byte[] packet8 = {0, 0, 0, 2, 1, 4, 2, 1, 5, 5, 5, 5, 5, 5, 5, 5, -128, 0, 0,
            4, -39, -84, 0, 36, 1, 0, 0, 1, -64, -88, 7, 90, -64, -88, 7, 92, 2, 0, 0, 10, 1, 4,
            2, 4, -34, -34, -34, -34, 5, 5, 5, 5, -128, 0, 0, 1, 31, -93, 0, 28, 0, 0, 0, 0, 0,
            0, 0, 10};
    private LsUpdate lsUpdate;
    private RouterLsa ospflsa;
    private NetworkLsa ospflsa1;
    private SummaryLsa ospflsa2;
    private AsbrSummaryLsa ospflsa3;
    private ExternalLsa ospflsa4;
    private Vector<OspfLsa> listLsa = new Vector();
    private List lsa;
    private int result;
    private OspfPacketType ospfMessageType;
    private OspfPacketHeader ospfPacketHeader;
    private byte[] result1;
    private ChannelBuffer channelBuffer;
    private OpaqueLsa10 opaqueLsa10;
    private OpaqueLsa9 opaqueLsa9;
    private OpaqueLsa11 opaqueLsa11;

    @Before
    public void setUp() throws Exception {
        lsUpdate = new LsUpdate();
        ospflsa = new RouterLsa();
        lsUpdate.setAuthType(1);
        lsUpdate.setOspftype(2);
        lsUpdate.setRouterId(Ip4Address.valueOf("10.226.165.164"));
        lsUpdate.setAreaId(Ip4Address.valueOf("10.226.165.100"));
        lsUpdate.setChecksum(201);
        lsUpdate.setAuthentication(2);
        lsUpdate.setOspfPacLength(48);
        lsUpdate.setOspfVer(2);
        ospflsa.setLsType(1);
        lsUpdate.addLsa(ospflsa);
        ospflsa1 = new NetworkLsa();
        ospflsa1.setNetworkMask(Ip4Address.valueOf("10.226.165.164"));
        ospflsa1.setLsType(2);
        lsUpdate.addLsa(ospflsa1);
        ospflsa2 = new SummaryLsa(new LsaHeader());
        ospflsa2.setLsType(3);
        lsUpdate.addLsa(ospflsa2);
        ospflsa3 = new AsbrSummaryLsa(new LsaHeader());
        ospflsa3.setNetworkMask(Ip4Address.valueOf("10.226.165.164"));
        ospflsa3.setLsType(4);
        lsUpdate.addLsa(ospflsa3);
        ospflsa4 = new ExternalLsa(new LsaHeader());
        ospflsa4.setLsType(5);
        lsUpdate.addLsa(ospflsa4);
    }

    @After
    public void tearDown() throws Exception {
        lsUpdate = null;
        ospflsa = null;
        ospflsa1 = null;
        ospflsa2 = null;
        ospflsa3 = null;
        ospflsa4 = null;
        listLsa.clear();
        lsa = null;
        ospfMessageType = null;
        ospfPacketHeader = null;
        result1 = null;
        channelBuffer = null;
        opaqueLsa9 = null;
        opaqueLsa10 = null;
        opaqueLsa11 = null;
    }

    /**
     * Tests getLsaList() getter method.
     */
    @Test
    public void testGetListLsa() throws Exception {
        lsUpdate.addLsa(ospflsa);
        lsUpdate.addLsa(ospflsa);
        lsa = lsUpdate.getLsaList();
        assertThat(lsa, is(notNullValue()));
        assertThat(lsa.size(), is(5));
    }

    /**
     * Tests addLsa() method.
     */
    @Test
    public void testAddLsa() throws Exception {
        lsUpdate.addLsa(ospflsa);
        assertThat(lsUpdate, is(notNullValue()));
    }

    /**
     * Tests noLsa() getter  method.
     */
    @Test
    public void testGetNoLsa() throws Exception {
        lsUpdate.setNumberOfLsa(5);
        result = lsUpdate.noLsa();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(5));
    }

    /**
     * Tests noLsa() setter  method.
     */
    @Test
    public void testSetNoLsa() throws Exception {
        lsUpdate.setNumberOfLsa(5);
        result = lsUpdate.noLsa();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(5));
    }

    /**
     * Tests ospfMessageType() getter  method.
     */
    @Test
    public void testGetOspfMessageType() throws Exception {
        ospfMessageType = lsUpdate.ospfMessageType();
        assertThat(ospfMessageType, is(OspfPacketType.LSUPDATE));
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
        ospfPacketHeader.setOspftype(4);
        ospfPacketHeader.setOspfVer(2);
        ospfPacketHeader.setRouterId(Ip4Address.valueOf("2.2.2.2"));
        ospfPacketHeader.setSourceIp(Ip4Address.valueOf("3.3.3.3"));
        lsUpdate = new LsUpdate(ospfPacketHeader);
        result1 = createLsUpdatePacket();
        channelBuffer = ChannelBuffers.copiedBuffer(result1);
        lsUpdate.readFrom(channelBuffer);
        channelBuffer = ChannelBuffers.copiedBuffer(packet1);
        lsUpdate.readFrom(channelBuffer);
        channelBuffer = ChannelBuffers.copiedBuffer(packet2);
        lsUpdate.readFrom(channelBuffer);
        channelBuffer = ChannelBuffers.copiedBuffer(packet3);
        lsUpdate.readFrom(channelBuffer);
        channelBuffer = ChannelBuffers.copiedBuffer(packet4);
        lsUpdate.readFrom(channelBuffer);
        channelBuffer = ChannelBuffers.copiedBuffer(packet5);
        lsUpdate.readFrom(channelBuffer);
        channelBuffer = ChannelBuffers.copiedBuffer(packet6);
        lsUpdate.readFrom(channelBuffer);
        channelBuffer = ChannelBuffers.copiedBuffer(packet7);
        lsUpdate.readFrom(channelBuffer);
        channelBuffer = ChannelBuffers.copiedBuffer(packet8);
        lsUpdate.readFrom(channelBuffer);
        assertThat(lsUpdate, is(notNullValue()));
        assertThat(lsUpdate.ospfMessageType(), is(OspfPacketType.LSUPDATE));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result1 = lsUpdate.asBytes();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests getLsuHeaderAsByteArray() method.
     */
    @Test
    public void testGetLsuHeaderAsByteArray() throws Exception {
        result1 = lsUpdate.getLsuHeaderAsByteArray();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests getLsuBodyAsByteArray() method.
     */
    @Test
    public void testGetLsuBodyAsByteArray() throws Exception {
        lsUpdate.setNumberOfLsa(8);
        lsUpdate.addLsa(ospflsa3);
        opaqueLsa9 = new OpaqueLsa9(new OpaqueLsaHeader());
        opaqueLsa9.setLsType(9);
        lsUpdate.addLsa(opaqueLsa9);
        opaqueLsa10 = new OpaqueLsa10(new OpaqueLsaHeader());
        opaqueLsa10.setLsType(10);
        lsUpdate.addLsa(opaqueLsa10);
        opaqueLsa11 = new OpaqueLsa11(new OpaqueLsaHeader());
        opaqueLsa10.setLsType(11);
        lsUpdate.addLsa(opaqueLsa11);
        result1 = lsUpdate.getLsuBodyAsByteArray();
        assertThat(result1, is(notNullValue()));
    }

    @Test
    public void testGetLsuBodyAsByteArray1() throws Exception {
        lsUpdate.setNumberOfLsa(8);
        opaqueLsa10 = new OpaqueLsa10(new OpaqueLsaHeader());
        opaqueLsa10.setLsType(10);
        lsUpdate.addLsa(opaqueLsa10);
        assertThat(result1, is(nullValue()));
    }

    @Test
    public void testGetLsuBodyAsByteArray2() throws Exception {
        opaqueLsa11 = new OpaqueLsa11(new OpaqueLsaHeader());
        opaqueLsa11.setLsType(11);
        lsUpdate.addLsa(opaqueLsa11);
        result1 = lsUpdate.getLsuBodyAsByteArray();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(lsUpdate.toString(), is(notNullValue()));
    }

    /**
     * Utility method used by junit methods.
     */
    private byte[] createLsUpdatePacket() {
        byte[] lsUpdatePacket = {0, 0, 0, 7, 0, 2, 2,
                1, -64, -88, -86, 3, -64, -88, -86, 3, -128, 0, 0, 1, 58,
                -100, 0, 48, 2, 0, 0, 2, -64, -88, -86, 0, -1, -1, -1, 0,
                3, 0, 0, 10, -64, -88, -86, 0, -1, -1, -1, 0, 3, 0, 0, 10,
                0, 3, 2, 5, 80, -44, 16, 0, -64, -88, -86, 2, -128, 0, 0,
                1, 42, 73, 0, 36, -1, -1, -1, -1, -128, 0, 0, 20, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 3, 2, 5, -108, 121, -85, 0, -64, -88,
                -86, 2, -128, 0, 0, 1, 52, -91, 0, 36, -1, -1, -1, 0,
                -128, 0, 0, 20, -64, -88, -86, 1, 0, 0, 0, 0, 0, 3, 2, 5,
                -64, -126, 120, 0, -64, -88, -86, 2, -128, 0, 0, 1, -45,
                25, 0, 36, -1, -1, -1, 0, -128, 0, 0, 20, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 3, 2, 5, -64, -88, 0, 0, -64, -88, -86, 2,
                -128, 0, 0, 1, 55, 8, 0, 36, -1, -1, -1, 0, -128, 0, 0,
                20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 2, 5, -64, -88, 1, 0,
                -64, -88, -86, 2, -128, 0, 0, 1, 44, 18, 0, 36, -1, -1, -1, 0, -128, 0,
                0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 2, 5, -64, -88, -84, 0, -64, -88,
                -86, 2, -128, 0, 0, 1, 51, 65, 0, 36, -1, -1, -1, 0, -128, 0, 0, 20, -64,
                -88, -86, 10, 0, 0, 0, 0};
        return lsUpdatePacket;
    }
}