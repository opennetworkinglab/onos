/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.google.common.base.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.dhcp.DhcpOption;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for DHCP class.
 */
public class DhcpTest {

    // For serialize test
    private static final int TRANSACTION_ID = 1000;
    private static final MacAddress CLIENT1_HOST_MAC = MacAddress.valueOf("1a:1a:1a:1a:1a:1a");
    private static final Ip4Address REQ_IP = Ip4Address.valueOf("10.2.0.2");
    private static final byte[] EXPECTED_SERIALIZED = ByteBuffer.allocate(300)
            .put((byte) 0x01) // op code
            .put((byte) 0x01) // hardware type
            .put((byte) 0x06) // hardware address len
            .put((byte) 0x00) // hops
            .putInt(0x3e8) // transaction id
            .putShort((short) 0x0) // seconds
            .putShort((short) 0x0) // flags
            .putInt(0) // client ip
            .putInt(0) // your ip
            .putInt(0) // server ip
            .putInt(0) // gateway ip
            .put(CLIENT1_HOST_MAC.toBytes()) // client hardware address
            .put(new byte[10]) // pad
            .put(new byte[64]) // server name
            .put(new byte[128]) // boot file name
            .putInt(0x63825363) // magic cookie
            .put(new byte[]{0x35, 0x1, 0x3}) // msg type
            .put(new byte[]{0x32, 0x4, 0xa, 0x2, 0x0, 0x2}) // requested ip
            .put((byte) 0xff) // end of options
            .put(new byte[50]) // pad
            .array();

    private Deserializer<DHCP> deserializer = DHCP.deserializer();

    private byte opCode = 1;
    private byte hardwareType = 1;
    private byte hardwareAddressLength = Ethernet.DATALAYER_ADDRESS_LENGTH;
    private byte hops = 0;
    private int transactionId = 0x2ed4eb50;
    private short seconds = 0;
    private short flags = 0;
    private int clientIpAddress = 1;
    private int yourIpAddress = 2;
    private int serverIpAddress = 3;
    private int gatewayIpAddress = 4;
    private byte[] clientHardwareAddress = MacAddress.valueOf(500).toBytes();
    private String serverName = "test-server";
    private String bootFileName = "test-file";

    private String hostName = "test-host";
    private DhcpOption hostNameOption = new DhcpOption();

    private byte[] byteHeader;

    @Before
    public void setUp() {
        hostNameOption.setCode((byte) 55);
        hostNameOption.setLength((byte) hostName.length());
        hostNameOption.setData(hostName.getBytes(Charsets.US_ASCII));

        // Packet length is the fixed DHCP header plus option length plus an
        // extra byte to indicate 'end of options'.
        ByteBuffer bb = ByteBuffer.allocate(DHCP.MIN_HEADER_LENGTH +
                                                    2 + hostNameOption.getLength()  + 1);

        bb.put(opCode);
        bb.put(hardwareType);
        bb.put(hardwareAddressLength);
        bb.put(hops);
        bb.putInt(transactionId);
        bb.putShort(seconds);
        bb.putShort(flags);
        bb.putInt(clientIpAddress);
        bb.putInt(yourIpAddress);
        bb.putInt(serverIpAddress);
        bb.putInt(gatewayIpAddress);
        bb.put(clientHardwareAddress);

        // need 16 bytes of zeros to pad out the client hardware address field
        bb.put(new byte[16 - hardwareAddressLength]);

        // Put server name and pad out to 64 bytes
        bb.put(serverName.getBytes(Charsets.US_ASCII));
        bb.put(new byte[64 - serverName.length()]);

        // Put boot file name and pad out to 128 bytes
        bb.put(bootFileName.getBytes(Charsets.US_ASCII));
        bb.put(new byte[128 - bootFileName.length()]);

        // Magic cookie
        bb.put("DHCP".getBytes(Charsets.US_ASCII));

        bb.put(hostNameOption.getCode());
        bb.put(hostNameOption.getLength());
        bb.put(hostNameOption.getData());

        // End of options marker
        bb.put((DHCP.DHCPOptionCode.OptionCode_END.getValue()));

        byteHeader = bb.array();
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(deserializer);
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        PacketTestUtils.testDeserializeTruncated(deserializer, byteHeader);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws Exception {
        DHCP dhcp = deserializer.deserialize(byteHeader, 0, byteHeader.length);

        assertEquals(opCode, dhcp.opCode);
        assertEquals(hardwareType, dhcp.hardwareType);
        assertEquals(hardwareAddressLength, dhcp.hardwareAddressLength);
        assertEquals(hops, dhcp.hops);
        assertEquals(transactionId, dhcp.transactionId);
        assertEquals(seconds, dhcp.seconds);
        assertEquals(flags, dhcp.flags);
        assertEquals(clientIpAddress, dhcp.clientIPAddress);
        assertEquals(yourIpAddress, dhcp.yourIPAddress);
        assertEquals(serverIpAddress, dhcp.serverIPAddress);
        assertEquals(gatewayIpAddress, dhcp.gatewayIPAddress);
        assertTrue(Arrays.equals(clientHardwareAddress, dhcp.clientHardwareAddress));

        assertEquals(serverName, dhcp.serverName);
        assertEquals(bootFileName, dhcp.bootFileName);
        assertEquals(2, dhcp.options.size());
        assertEquals(hostNameOption, dhcp.options.get(0));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringDhcp() throws Exception {
        DHCP dhcp = deserializer.deserialize(byteHeader, 0, byteHeader.length);
        String str = dhcp.toString();

        assertTrue(StringUtils.contains(str, "opCode=" + opCode));
        assertTrue(StringUtils.contains(str, "hardwareType=" + hardwareType));
        assertTrue(StringUtils.contains(str, "hardwareAddressLength=" + hardwareAddressLength));
        assertTrue(StringUtils.contains(str, "hops=" + hops));
        assertTrue(StringUtils.contains(str, "transactionId=" + transactionId));
        assertTrue(StringUtils.contains(str, "seconds=" + seconds));
        assertTrue(StringUtils.contains(str, "flags=" + flags));
        assertTrue(StringUtils.contains(str, "clientIPAddress=" + clientIpAddress));
        assertTrue(StringUtils.contains(str, "yourIPAddress=" + yourIpAddress));
        assertTrue(StringUtils.contains(str, "serverIPAddress=" + serverIpAddress));
        assertTrue(StringUtils.contains(str, "gatewayIPAddress=" + gatewayIpAddress));
        assertTrue(StringUtils.contains(str, "clientHardwareAddress=" + Arrays.toString(clientHardwareAddress)));
        assertTrue(StringUtils.contains(str, "serverName=" + serverName));
        assertTrue(StringUtils.contains(str, "bootFileName=" + bootFileName));
        // TODO: add option unit test
    }



    @Test
    public void testSerialize() throws Exception {
        DHCP dhcpReply = new DHCP();
        dhcpReply.setOpCode(DHCP.OPCODE_REQUEST);

        dhcpReply.setYourIPAddress(0);
        dhcpReply.setServerIPAddress(0);

        dhcpReply.setTransactionId(TRANSACTION_ID);
        dhcpReply.setClientHardwareAddress(CLIENT1_HOST_MAC.toBytes());
        dhcpReply.setHardwareType(DHCP.HWTYPE_ETHERNET);
        dhcpReply.setHardwareAddressLength((byte) 6);

        // DHCP Options.
        DhcpOption option = new DhcpOption();
        List<DhcpOption> optionList = new ArrayList<>();

        // DHCP Message Type.
        option.setCode(DHCP.DHCPOptionCode.OptionCode_MessageType.getValue());
        option.setLength((byte) 1);
        byte[] optionData = {(byte) DHCP.MsgType.DHCPREQUEST.getValue()};
        option.setData(optionData);
        optionList.add(option);

        // DHCP Requested IP.
        option = new DhcpOption();
        option.setCode(DHCP.DHCPOptionCode.OptionCode_RequestedIP.getValue());
        option.setLength((byte) 4);
        optionData = REQ_IP.toOctets();
        option.setData(optionData);
        optionList.add(option);

        // End Option.
        option = new DhcpOption();
        option.setCode(DHCP.DHCPOptionCode.OptionCode_END.getValue());
        option.setLength((byte) 1);
        optionList.add(option);

        dhcpReply.setOptions(optionList);

        assertArrayEquals(EXPECTED_SERIALIZED, dhcpReply.serialize());
    }
}
