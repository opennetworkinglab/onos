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

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for DHCP class.
 */
public class DhcpTest {

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
    private DHCPOption hostNameOption = new DHCPOption();

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
        bb.put((byte) (0xff & 255));

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
}
