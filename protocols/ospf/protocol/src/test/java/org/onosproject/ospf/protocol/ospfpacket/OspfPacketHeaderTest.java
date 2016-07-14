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
package org.onosproject.ospf.protocol.ospfpacket;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for OspfPacketHeader.
 */
public class OspfPacketHeaderTest {

    private final byte[] packet = {0, 0, 0, 0};
    private OspfPacketHeader ospfPacketHeader;
    private ChannelBuffer channelBuffer;
    private byte[] result2;
    private int result;
    private Ip4Address result1;

    @Before
    public void setUp() throws Exception {
        ospfPacketHeader = new OspfPacketHeader();
    }

    @After
    public void tearDown() throws Exception {
        ospfPacketHeader = null;
        ospfPacketHeader = null;
        channelBuffer = null;
        result2 = null;
        result1 = null;
    }

    /**
     * Tests sourceIp() getter method.
     */
    @Test
    public void testGetSourceIP() throws Exception {
        ospfPacketHeader.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfPacketHeader.sourceIp(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests sourceIp() setter method.
     */
    @Test
    public void testSetSourceIP() throws Exception {
        ospfPacketHeader.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        assertThat(result, is(notNullValue()));
        assertThat(ospfPacketHeader.sourceIp(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests ospfMessageType() getter method.
     */
    @Test
    public void testGetOspfMessageType() throws Exception {
        assertThat(ospfPacketHeader.ospfMessageType(), nullValue());
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        ospfPacketHeader.readFrom(channelBuffer);
        assertThat(ospfPacketHeader, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result2 = ospfPacketHeader.asBytes();
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests ospfVersion() getter method.
     */
    @Test
    public void testGetOspfVer() throws Exception {
        ospfPacketHeader.setOspfVer(2);
        result = ospfPacketHeader.ospfVersion();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(2));
    }

    /**
     * Tests ospfVersion() setter method.
     */
    @Test
    public void testSetOspfVer() throws Exception {
        ospfPacketHeader.setOspfVer(2);
        result = ospfPacketHeader.ospfVersion();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(2));
    }

    /**
     * Tests ospfType() getter method.
     */
    @Test
    public void testGetOspfType() throws Exception {
        ospfPacketHeader.setOspftype(3);
        result = ospfPacketHeader.ospfType();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(3));
    }

    /**
     * Tests ospfType() setter method.
     */
    @Test
    public void testSetOspfType() throws Exception {
        ospfPacketHeader.setOspftype(3);
        result = ospfPacketHeader.ospfType();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(3));
    }

    /**
     * Tests ospfPacLength() getter method.
     */
    @Test
    public void testGetOspfPacLength() throws Exception {
        ospfPacketHeader.setOspfPacLength(3);
        result = ospfPacketHeader.ospfPacLength();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(3));
    }

    /**
     * Tests ospfPacLength() setter method.
     */
    @Test
    public void testSetOspfPacLength() throws Exception {
        ospfPacketHeader.setOspfPacLength(3);
        int result = ospfPacketHeader.ospfPacLength();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(3));
    }

    /**
     * Tests routerId()getter method.
     */
    @Test
    public void testGetRouterId() throws Exception {

        ospfPacketHeader.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        result1 = ospfPacketHeader.routerId();
        assertThat(result1, is(notNullValue()));
        assertThat(result1, is(Ip4Address.valueOf("1.1.1.1")));

    }

    /**
     * Tests routerId() setter method.
     */
    @Test
    public void testSetRouterId() throws Exception {
        ospfPacketHeader.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        result1 = ospfPacketHeader.routerId();
        assertThat(result1, is(notNullValue()));
        assertThat(result1, is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests areaId() getter method.
     */
    @Test
    public void testGetAreaId() throws Exception {
        ospfPacketHeader.setAreaId(Ip4Address.valueOf("1.1.1.1"));
        result1 = ospfPacketHeader.areaId();
        assertThat(result1, is(notNullValue()));
        assertThat(result1, is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests areaId() setter method.
     */
    @Test
    public void testSetAreaId() throws Exception {
        ospfPacketHeader.setAreaId(Ip4Address.valueOf("1.1.1.1"));
        result1 = ospfPacketHeader.areaId();
        assertThat(result1, is(notNullValue()));
        assertThat(result1, is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests checksum() getter method.
     */
    @Test
    public void testGetChecksum() throws Exception {
        ospfPacketHeader.setChecksum(3);
        result = ospfPacketHeader.checksum();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(3));
    }

    /**
     * Tests checksum() setter method.
     */
    @Test
    public void testSetChecksum() throws Exception {
        ospfPacketHeader.setChecksum(3);
        result = ospfPacketHeader.checksum();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(3));
    }

    /**
     * Tests authType() getter method.
     */
    @Test
    public void testGetAutype() throws Exception {
        ospfPacketHeader.setAuthType(3);
        result = ospfPacketHeader.authType();
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result);
    }

    /**
     * Tests authType() setter method.
     */
    @Test
    public void testSetAutype() throws Exception {
        ospfPacketHeader.setAuthType(3);
        result = ospfPacketHeader.authType();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(3));
    }

    /**
     * Tests authentication() getter method.
     */
    @Test
    public void testGetAuthentication() throws Exception {
        ospfPacketHeader.setAuthentication(3);
        result = ospfPacketHeader.authentication();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(3));
    }

    /**
     * Tests authentication() setter method.
     */
    @Test
    public void testSetAuthentication() throws Exception {
        ospfPacketHeader.setAuthentication(3);
        result = ospfPacketHeader.authentication();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(3));
    }

    /**
     * Tests destinationIp() getter method.
     */
    @Test
    public void testGetDestinationIP() throws Exception {
        ospfPacketHeader.setDestinationIp(Ip4Address.valueOf("1.1.1.1"));
        result1 = ospfPacketHeader.destinationIp();
        assertThat(result1, is(notNullValue()));
        assertThat(result1, is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests destinationIp() setter method.
     */
    @Test
    public void testSetDestinationIP() throws Exception {
        ospfPacketHeader.setDestinationIp(Ip4Address.valueOf("1.1.1.1"));
        result1 = ospfPacketHeader.destinationIp();
        assertThat(result1, is(notNullValue()));
        assertThat(result1, is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(ospfPacketHeader.toString(), is(notNullValue()));
    }

    /**
     * Tests populateHeader() method.
     */
    @Test
    public void testPopulateHeader() throws Exception {
        ospfPacketHeader.populateHeader(new OspfPacketHeader());
        assertThat(ospfPacketHeader, is(notNullValue()));
    }

}