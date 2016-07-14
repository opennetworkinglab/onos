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
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for OpaqueLsa9.
 */
public class OpaqueLsa9Test {

    private final byte[] packet = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0};
    private byte[] result;
    private String result1;
    private OpaqueLsaHeader opqueHeader;
    private OpaqueLsa9 opaqueLsa9;
    private ChannelBuffer channelBuffer;
    private OspfLsaType ospflsaType;
    private int result2;

    @Before
    public void setUp() throws Exception {
        opaqueLsa9 = new OpaqueLsa9(new OpaqueLsaHeader());

    }

    @After
    public void tearDown() throws Exception {
        opaqueLsa9 = null;
        opqueHeader = null;
        channelBuffer = null;
        result = null;
        ospflsaType = null;
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        opqueHeader = new OpaqueLsaHeader();
        opqueHeader.setLsType(1);
        opqueHeader.setLsPacketLen(48);
        opqueHeader.setLsCheckSum(10);
        opqueHeader.setAge(4);
        opqueHeader.setOpaqueId(9);
        opqueHeader.setOpaqueType(9);
        opqueHeader.setLsSequenceNo(250);
        opqueHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader.setOptions(2);
        opaqueLsa9 = new OpaqueLsa9(opqueHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        opaqueLsa9.readFrom(channelBuffer);
        assertThat(opaqueLsa9, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        opqueHeader = new OpaqueLsaHeader();
        opqueHeader.setLsType(9);
        opqueHeader.setLsPacketLen(48);
        opqueHeader.setLsCheckSum(10);
        opqueHeader.setAge(4);
        opqueHeader.setOpaqueId(9);
        opqueHeader.setOpaqueType(9);
        opqueHeader.setLsSequenceNo(250);
        opqueHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader.setOptions(2);
        opaqueLsa9 = new OpaqueLsa9(opqueHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        opaqueLsa9.readFrom(channelBuffer);
        result = opaqueLsa9.asBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests getLsaBodyAsByteArray() method.
     */
    @Test
    public void testGetLsaBodyAsByteArray() throws Exception {
        opqueHeader = new OpaqueLsaHeader();
        opqueHeader.setLsType(9);
        opqueHeader.setLsPacketLen(48);
        opqueHeader.setLsCheckSum(10);
        opqueHeader.setAge(4);
        opqueHeader.setOpaqueId(9);
        opqueHeader.setOpaqueType(9);
        opqueHeader.setLsSequenceNo(250);
        opqueHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader.setOptions(2);
        opaqueLsa9 = new OpaqueLsa9(opqueHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        opaqueLsa9.readFrom(channelBuffer);
        result = opaqueLsa9.getLsaBodyAsByteArray();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashcode() throws Exception {

        result2 = opaqueLsa9.hashCode();
        assertThat(result2, is(notNullValue()));

    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {

        result1 = opaqueLsa9.toString();
        assertThat(result1, is(notNullValue()));

    }

    /**
     * Tests to getOspfLsaType() getter method.
     */
    @Test
    public void testGetOspfLsaType() throws Exception {
        opaqueLsa9.setLsType(9);
        ospflsaType = opaqueLsa9.getOspfLsaType();
        assertThat(ospflsaType, is(notNullValue()));
        assertThat(ospflsaType, is(OspfLsaType.LINK_LOCAL_OPAQUE_LSA));
    }
}
