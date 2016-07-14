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
 * Unit test class for OpaqueLsa11.
 */
public class OpaqueLsa11Test {

    private final byte[] packet = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private OpaqueLsa11 opaqueLsa11;
    private OpaqueLsaHeader opqueHeader;
    private ChannelBuffer channelBuffer;
    private byte[] result;
    private int result1;
    private String result2;
    private OspfLsaType ospflsaType;

    @Before
    public void setUp() throws Exception {
        opaqueLsa11 = new OpaqueLsa11(new OpaqueLsaHeader());
    }

    @After
    public void tearDown() throws Exception {
        opaqueLsa11 = null;
        opqueHeader = null;
        channelBuffer = null;
        result = null;
        ospflsaType = null;
    }

    /**
     * Tests readFrom()  method.
     */
    @Test
    public void testReadFrom() throws Exception {
        opqueHeader = new OpaqueLsaHeader();
        opqueHeader.setLsType(11);
        opqueHeader.setLsPacketLen(48);
        opqueHeader.setLsCheckSum(10);
        opqueHeader.setAge(4);
        opqueHeader.setOpaqueId(1);
        opqueHeader.setOpaqueType(11);
        opqueHeader.setLsSequenceNo(250);
        opqueHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader.setOptions(2);
        opaqueLsa11 = new OpaqueLsa11(opqueHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        opaqueLsa11.readFrom(channelBuffer);
        assertThat(opaqueLsa11, is(notNullValue()));
    }

    /**
     * Tests asBytes()  method.
     */
    @Test
    public void testAsBytes() throws Exception {
        opqueHeader = new OpaqueLsaHeader();
        opqueHeader.setLsType(11);
        opqueHeader.setLsPacketLen(48);
        opqueHeader.setLsCheckSum(10);
        opqueHeader.setAge(4);
        opqueHeader.setOpaqueId(1);
        opqueHeader.setOpaqueType(11);
        opqueHeader.setLsSequenceNo(250);
        opqueHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader.setOptions(2);
        opaqueLsa11 = new OpaqueLsa11(opqueHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        opaqueLsa11.readFrom(channelBuffer);
        result = opaqueLsa11.asBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests getLsaBodyAsByteArray()  method.
     */
    @Test
    public void testGetLsaBodyAsByteArray() throws Exception {
        opqueHeader = new OpaqueLsaHeader();
        opqueHeader.setLsType(11);
        opqueHeader.setLsPacketLen(48);
        opqueHeader.setLsCheckSum(10);
        opqueHeader.setAge(4);
        opqueHeader.setOpaqueId(1);
        opqueHeader.setOpaqueType(11);
        opqueHeader.setLsSequenceNo(250);
        opqueHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader.setOptions(2);
        opaqueLsa11 = new OpaqueLsa11(opqueHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        opaqueLsa11.readFrom(channelBuffer);
        result = opaqueLsa11.getLsaBodyAsByteArray();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests getOspfLsaType()  method.
     */
    @Test
    public void testGetOspfLsaType() throws Exception {
        opaqueLsa11.setLsType(11);
        ospflsaType = opaqueLsa11.getOspfLsaType();
        assertThat(ospflsaType, is(notNullValue()));
        assertThat(ospflsaType, is(OspfLsaType.AS_OPAQUE_LSA));
    }

    /**
     * Tests hashCode()  method.
     */
    @Test
    public void testHashcode() throws Exception {

        result1 = opaqueLsa11.hashCode();
        assertThat(result1, is(notNullValue()));

    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {

        result2 = opaqueLsa11.toString();
        assertThat(result2, is(notNullValue()));

    }
}
