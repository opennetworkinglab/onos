/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onlab.packet;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

public class Dhcp6Test {
    private static final int OPT_CLIENT_ID = 0xBEEFBEEF;
    private static final byte[] OPT_CLIENT_ID_BYTE_ARR =
            {(byte) 0xBE, (byte) 0xEF, (byte) 0xBE, (byte) 0xEF};
    private static final short OPT_CLIENT_ID_SIZE = 4;
    private static final int OPT_AUTH = 0xBA11BA11;
    private static final byte[] OPT_AUTH_BYTE_AR =
            {(byte) 0xBA, 0x11, (byte) 0xBA, 0x11};
    private static final short OPT_AUTH_SIZE = 4;
    private static final int TRANSACTION_ID = 0xC0FFEE;
    private static final byte[] TRANSACTION_ID_BYTE_ARR =
            {(byte) 0xC0, (byte) 0xFF, (byte) 0xEE};
    private static final byte HOP_COUNT = 3;
    private static final Ip6Address LINK_ADDRESS = Ip6Address.valueOf("1111:2222::8888");
    private static final Ip6Address PEER_ADDRESS = Ip6Address.valueOf("3333:4444::9999");
    Deserializer<DHCP6> deserializer = DHCP6.deserializer();
    private byte[] byteHeader;

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(deserializer);
    }

    /**
     * Truncated a simple DHCPv6 payload.
     */
    @Test
    public void testDeserializeTruncated() throws Exception {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.put(DHCP6.MsgType.REQUEST.value());
        bb.put(TRANSACTION_ID_BYTE_ARR);
        byteHeader = bb.array();

        PacketTestUtils.testDeserializeTruncated(deserializer, byteHeader);
    }

    /**
     * Basic DHCPv6 header with one msg type and one transaction id.
     */
    @Test
    public void testDeserializeDefaultPayload() throws Exception {
        ByteBuffer bb = ByteBuffer.allocate(12);
        bb.put(DHCP6.MsgType.REQUEST.value());
        bb.put(TRANSACTION_ID_BYTE_ARR);

        // put a simple client id (4 bytes)
        bb.putShort(DHCP6.OptionCode.CLIENTID.value());
        bb.putShort(OPT_CLIENT_ID_SIZE);
        bb.putInt(OPT_CLIENT_ID);
        byteHeader = bb.array();

        DHCP6 dhcp6 = deserializer.deserialize(byteHeader, 0, byteHeader.length);
        assertEquals(dhcp6.getMsgType(), DHCP6.MsgType.REQUEST.value());
        assertEquals(dhcp6.getTransactionId(), TRANSACTION_ID);
        assertEquals(dhcp6.getOptions().size(), 1);

        DHCP6Option clientIdOption = dhcp6.getOptions().get(0);
        assertEquals(clientIdOption.getCode(), DHCP6.OptionCode.CLIENTID.value());
        assertArrayEquals(clientIdOption.getData(), OPT_CLIENT_ID_BYTE_ARR);
    }

    /**
     * DHCPv6 header with relay agent information.
     */
    @Test
    public void testDeserializeRelayAgent() throws Exception {
        ByteBuffer bb = ByteBuffer.allocate(42);
        bb.put(DHCP6.MsgType.RELAY_FORW.value());
        bb.put(HOP_COUNT);

        bb.put(LINK_ADDRESS.toOctets());
        bb.put(PEER_ADDRESS.toOctets());

        // put a simple client id (4 bytes)
        bb.putShort(DHCP6.OptionCode.CLIENTID.value());
        bb.putShort(OPT_CLIENT_ID_SIZE);
        bb.putInt(OPT_CLIENT_ID);
        byteHeader = bb.array();

        DHCP6 dhcp6 = deserializer.deserialize(byteHeader, 0, byteHeader.length);
        assertEquals(dhcp6.getMsgType(), DHCP6.MsgType.RELAY_FORW.value());
        assertEquals(dhcp6.getHopCount(), HOP_COUNT);
        assertArrayEquals(dhcp6.getLinkAddress(), LINK_ADDRESS.toOctets());
        assertArrayEquals(dhcp6.getPeerAddress(), PEER_ADDRESS.toOctets());
        assertEquals(dhcp6.getOptions().size(), 1);

        DHCP6Option clientIdOption = dhcp6.getOptions().get(0);
        assertEquals(clientIdOption.getCode(), DHCP6.OptionCode.CLIENTID.value());
        assertArrayEquals(clientIdOption.getData(), OPT_CLIENT_ID_BYTE_ARR);
    }

    /**
     * Serialize DHCPv6 header with default payload and options.
     */
    @Test
    public void testSerializeDefaultPayload() throws Exception {
        DHCP6 dhcp6 = new DHCP6();
        dhcp6.setMsgType(DHCP6.MsgType.REQUEST.value());
        dhcp6.setTransactionId(TRANSACTION_ID);

        DHCP6Option opt1 = new DHCP6Option();
        opt1.setCode(DHCP6.OptionCode.CLIENTID.value());
        opt1.setLength(OPT_CLIENT_ID_SIZE);
        opt1.setData(OPT_CLIENT_ID_BYTE_ARR);


        DHCP6Option opt2 = new DHCP6Option();
        opt2.setCode(DHCP6.OptionCode.AUTH.value());
        opt2.setLength(OPT_AUTH_SIZE);
        opt2.setData(OPT_AUTH_BYTE_AR);

        dhcp6.setOptions(ImmutableList.of(opt1, opt2));

        byte[] serialized = dhcp6.serialize();
        ByteBuffer expected = ByteBuffer.allocate(20)
                .put(DHCP6.MsgType.REQUEST.value())
                .put(TRANSACTION_ID_BYTE_ARR)
                .putShort(DHCP6.OptionCode.CLIENTID.value())
                .putShort(OPT_CLIENT_ID_SIZE)
                .putInt(OPT_CLIENT_ID)
                .putShort(DHCP6.OptionCode.AUTH.value())
                .putShort(OPT_AUTH_SIZE)
                .putInt(OPT_AUTH);

        assertArrayEquals(serialized, expected.array());
    }

    /**
     * Serialize DHCPv6 header with relay agent payload and options.
     */
    @Test
    public void testSerializeRelayAgent() throws Exception {
        DHCP6 dhcp6 = new DHCP6();
        dhcp6.setMsgType(DHCP6.MsgType.RELAY_FORW.value());
        dhcp6.setHopCount(HOP_COUNT);
        dhcp6.setLinkAddress(LINK_ADDRESS.toOctets());
        dhcp6.setPeerAddress(PEER_ADDRESS.toOctets());

        DHCP6Option opt1 = new DHCP6Option();
        opt1.setCode(DHCP6.OptionCode.CLIENTID.value());
        opt1.setLength(OPT_CLIENT_ID_SIZE);
        opt1.setData(OPT_CLIENT_ID_BYTE_ARR);


        DHCP6Option opt2 = new DHCP6Option();
        opt2.setCode(DHCP6.OptionCode.AUTH.value());
        opt2.setLength(OPT_AUTH_SIZE);
        opt2.setData(OPT_AUTH_BYTE_AR);

        dhcp6.setOptions(ImmutableList.of(opt1, opt2));

        byte[] serialized = dhcp6.serialize();
        ByteBuffer expected = ByteBuffer.allocate(50)
                .put(DHCP6.MsgType.RELAY_FORW.value())
                .put(HOP_COUNT)
                .put(LINK_ADDRESS.toOctets())
                .put(PEER_ADDRESS.toOctets())
                .putShort(DHCP6.OptionCode.CLIENTID.value())
                .putShort(OPT_CLIENT_ID_SIZE)
                .putInt(OPT_CLIENT_ID)
                .putShort(DHCP6.OptionCode.AUTH.value())
                .putShort(OPT_AUTH_SIZE)
                .putInt(OPT_AUTH);

        assertArrayEquals(serialized, expected.array());
    }
}
