/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onlab.packet.dhcp;

import com.google.common.io.Resources;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;
import org.onlab.packet.DHCP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.PacketTestUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 * Unit tests for DHCP class.
 */
public class DhcpTest {
    private static final String DISCOVER = "dhcp_discover.bin";
    private static final String OFFER = "dhcp_offer.bin";
    private static final String REQUEST = "dhcp_request.bin";
    private static final String ACK = "dhcp_ack.bin";
    private static final String LONG_OPT = "dhcp_long_opt.bin";
    private static final String EMPTY = "";
    private static final byte HW_TYPE = 1;
    private static final byte HW_ADDR_LEN = 6;
    private static final byte HOPS = 1;
    private static final int XID = 0x8f5a186c;
    private static final short SECS = 0;
    private static final short FLAGS = 0;
    private static final int NO_IP = 0;
    private static final Ip4Address GW_IP = Ip4Address.valueOf("10.0.4.254");
    private static final Ip4Address SERVER_IP = Ip4Address.valueOf("10.0.99.3");
    private static final MacAddress CLIENT_HW_ADDR = MacAddress.valueOf("00:aa:00:00:00:01");
    private static final Ip4Address CLIENT_IP = Ip4Address.valueOf("10.0.4.1");
    private static final Ip4Address DNS_1 = Ip4Address.valueOf("8.8.8.8");
    private static final Ip4Address DNS_2 = Ip4Address.valueOf("8.8.4.4");
    private static final Ip4Address SUBNET_MASK = Ip4Address.valueOf("255.255.255.0");
    private static final String HOSTNAME = "charlie-n";
    private static final String CIRCUIT_ID = "relay-eth0";
    private static final String DOMAIN_NAME = "trellis.local";

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(DHCP.deserializer());
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        byte[] byteHeader = ByteBuffer.allocate(241)
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
                .put(MacAddress.valueOf("1a:1a:1a:1a:1a:1a").toBytes()) // client hardware address
                .put(new byte[10]) // pad
                .put(new byte[64]) // server name
                .put(new byte[128]) // boot file name
                .putInt(0x63825363) // magic cookie
                .put((byte) 0xff) // end of options
                .array();
        PacketTestUtils.testDeserializeTruncated(DHCP.deserializer(), byteHeader);
    }

    /**
     * Tests deserialize discover packet.
     */
    @Test
    public void testDeserializeDiscover() throws Exception {
        byte[] data = Resources.toByteArray(Dhcp6RelayTest.class.getResource(DISCOVER));
        Ethernet eth = Ethernet.deserializer().deserialize(data, 0, data.length);
        DHCP dhcp = (DHCP) eth.getPayload().getPayload().getPayload();

        assertEquals(DHCP.OPCODE_REQUEST, dhcp.getOpCode());
        assertEquals(HW_TYPE, dhcp.getHardwareType());
        assertEquals(HW_ADDR_LEN, dhcp.getHardwareAddressLength());
        assertEquals(HOPS, dhcp.getHops());
        assertEquals(XID, dhcp.getTransactionId());
        assertEquals(SECS, dhcp.getSeconds());
        assertEquals(FLAGS, dhcp.getFlags());
        assertEquals(NO_IP, dhcp.getClientIPAddress());
        assertEquals(NO_IP, dhcp.getYourIPAddress());
        assertEquals(NO_IP, dhcp.getServerIPAddress());
        assertEquals(GW_IP.toInt(), dhcp.getGatewayIPAddress());
        assertTrue(Arrays.equals(CLIENT_HW_ADDR.toBytes(), dhcp.getClientHardwareAddress()));
        assertEquals(EMPTY, dhcp.getServerName());
        assertEquals(EMPTY, dhcp.getBootFileName());
        assertEquals(6, dhcp.getOptions().size());

        DhcpOption option = dhcp.getOptions().get(0);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_MessageType.getValue(), option.code);
        assertEquals(1, option.length);
        assertEquals(DHCP.MsgType.DHCPDISCOVER.getValue(), (int) option.getData()[0]);

        option = dhcp.getOptions().get(1);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_RequestedIP.getValue(), option.code);
        assertEquals(4, option.length);
        assertArrayEquals(CLIENT_IP.toOctets(), option.getData());

        option = dhcp.getOptions().get(2);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_HostName.getValue(), option.code);
        assertEquals(9, option.length);
        assertArrayEquals(HOSTNAME.getBytes(Charsets.US_ASCII), option.getData());

        option = dhcp.getOptions().get(3);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_RequestedParameters.getValue(),
                     option.code);
        assertEquals(13, option.length);
        assertArrayEquals(new byte[]{1, 28, 2, 3, 15, 6, 119, 12, 44, 47, 26, 121, 42},
                     option.getData());

        option = dhcp.getOptions().get(4);
        assertTrue(option instanceof DhcpRelayAgentOption);
        DhcpRelayAgentOption relayAgentOption = (DhcpRelayAgentOption) option;
        assertEquals(DHCP.DHCPOptionCode.OptionCode_CircuitID.getValue(), relayAgentOption.code);
        assertEquals(12, relayAgentOption.length);
        DhcpOption subOption = relayAgentOption
                .getSubOption(DhcpRelayAgentOption.RelayAgentInfoOptions.CIRCUIT_ID.getValue());
        assertEquals(10, subOption.getLength());
        assertArrayEquals(CIRCUIT_ID.getBytes(Charsets.US_ASCII), subOption.getData());

        option = dhcp.getOptions().get(5);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_END.getValue(), option.code);
        assertEquals(0, option.length);
    }

    /**
     * Tests deserialize discover packet.
     */
    @Test
    public void testDeserializeOffer() throws Exception {
        byte[] data = Resources.toByteArray(Dhcp6RelayTest.class.getResource(OFFER));
        Ethernet eth = Ethernet.deserializer().deserialize(data, 0, data.length);
        DHCP dhcp = (DHCP) eth.getPayload().getPayload().getPayload();

        assertEquals(DHCP.OPCODE_REPLY, dhcp.getOpCode());
        assertEquals(HW_TYPE, dhcp.getHardwareType());
        assertEquals(HW_ADDR_LEN, dhcp.getHardwareAddressLength());
        assertEquals(HOPS, dhcp.getHops());
        assertEquals(XID, dhcp.getTransactionId());
        assertEquals(SECS, dhcp.getSeconds());
        assertEquals(FLAGS, dhcp.getFlags());
        assertEquals(NO_IP, dhcp.getClientIPAddress());
        assertEquals(CLIENT_IP.toInt(), dhcp.getYourIPAddress());
        assertEquals(SERVER_IP.toInt(), dhcp.getServerIPAddress());
        assertEquals(GW_IP.toInt(), dhcp.getGatewayIPAddress());
        assertTrue(Arrays.equals(CLIENT_HW_ADDR.toBytes(), dhcp.getClientHardwareAddress()));
        assertEquals(EMPTY, dhcp.getServerName());
        assertEquals(EMPTY, dhcp.getBootFileName());
        assertEquals(9, dhcp.getOptions().size());

        DhcpOption option = dhcp.getOptions().get(0);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_MessageType.getValue(), option.code);
        assertEquals(1, option.length);
        assertEquals(DHCP.MsgType.DHCPOFFER.getValue(), (int) option.getData()[0]);

        option = dhcp.getOptions().get(1);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_DHCPServerIp.getValue(), option.code);
        assertEquals(4, option.length);
        assertArrayEquals(SERVER_IP.toOctets(), option.getData());

        option = dhcp.getOptions().get(2);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_LeaseTime.getValue(), option.code);
        assertEquals(4, option.length);
        assertArrayEquals(new byte[]{0, 0, 2, 88}, option.getData());

        option = dhcp.getOptions().get(3);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_SubnetMask.getValue(), option.code);
        assertEquals(4, option.length);
        assertArrayEquals(SUBNET_MASK.toOctets(), option.getData());

        option = dhcp.getOptions().get(4);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_RouterAddress.getValue(), option.code);
        assertEquals(4, option.length);
        assertArrayEquals(GW_IP.toOctets(), option.getData());

        option = dhcp.getOptions().get(5);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_DomainName.getValue(), option.code);
        assertEquals(13, option.length);
        assertArrayEquals(DOMAIN_NAME.getBytes(Charsets.US_ASCII), option.getData());

        option = dhcp.getOptions().get(6);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_DomainServer.getValue(), option.code);
        assertEquals(8, option.length);
        assertArrayEquals(ArrayUtils.addAll(DNS_1.toOctets(), DNS_2.toOctets()),
                          option.getData());

        option = dhcp.getOptions().get(7);
        assertTrue(option instanceof DhcpRelayAgentOption);
        DhcpRelayAgentOption relayAgentOption = (DhcpRelayAgentOption) option;
        assertEquals(DHCP.DHCPOptionCode.OptionCode_CircuitID.getValue(), relayAgentOption.code);
        assertEquals(12, relayAgentOption.length);
        DhcpOption subOption = relayAgentOption
                .getSubOption(DhcpRelayAgentOption.RelayAgentInfoOptions.CIRCUIT_ID.getValue());
        assertEquals(10, subOption.getLength());
        assertArrayEquals(CIRCUIT_ID.getBytes(Charsets.US_ASCII), subOption.getData());

        option = dhcp.getOptions().get(8);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_END.getValue(), option.code);
        assertEquals(0, option.length);
    }

    /**
     * Tests deserialize discover packet.
     */
    @Test
    public void testDeserializeRequest() throws Exception {
        byte[] data = Resources.toByteArray(Dhcp6RelayTest.class.getResource(REQUEST));
        Ethernet eth = Ethernet.deserializer().deserialize(data, 0, data.length);
        DHCP dhcp = (DHCP) eth.getPayload().getPayload().getPayload();

        assertEquals(DHCP.OPCODE_REQUEST, dhcp.getOpCode());
        assertEquals(HW_TYPE, dhcp.getHardwareType());
        assertEquals(HW_ADDR_LEN, dhcp.getHardwareAddressLength());
        assertEquals(HOPS, dhcp.getHops());
        assertEquals(XID, dhcp.getTransactionId());
        assertEquals(SECS, dhcp.getSeconds());
        assertEquals(FLAGS, dhcp.getFlags());
        assertEquals(NO_IP, dhcp.getClientIPAddress());
        assertEquals(NO_IP, dhcp.getYourIPAddress());
        assertEquals(NO_IP, dhcp.getServerIPAddress());
        assertEquals(GW_IP.toInt(), dhcp.getGatewayIPAddress());
        assertTrue(Arrays.equals(CLIENT_HW_ADDR.toBytes(), dhcp.getClientHardwareAddress()));
        assertEquals(EMPTY, dhcp.getServerName());
        assertEquals(EMPTY, dhcp.getBootFileName());
        assertEquals(7, dhcp.getOptions().size());

        DhcpOption option = dhcp.getOptions().get(0);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_MessageType.getValue(), option.code);
        assertEquals(1, option.length);
        assertEquals(DHCP.MsgType.DHCPREQUEST.getValue(), (int) option.getData()[0]);

        option = dhcp.getOptions().get(1);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_DHCPServerIp.getValue(), option.code);
        assertEquals(4, option.length);
        assertArrayEquals(SERVER_IP.toOctets(), option.getData());

        option = dhcp.getOptions().get(2);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_RequestedIP.getValue(), option.code);
        assertEquals(4, option.length);
        assertArrayEquals(CLIENT_IP.toOctets(), option.getData());

        option = dhcp.getOptions().get(3);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_HostName.getValue(), option.code);
        assertEquals(9, option.length);
        assertArrayEquals(HOSTNAME.getBytes(Charsets.US_ASCII), option.getData());

        option = dhcp.getOptions().get(4);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_RequestedParameters.getValue(),
                     option.code);
        assertEquals(13, option.length);
        assertArrayEquals(new byte[]{1, 28, 2, 3, 15, 6, 119, 12, 44, 47, 26, 121, 42},
                          option.getData());

        option = dhcp.getOptions().get(5);
        assertTrue(option instanceof DhcpRelayAgentOption);
        DhcpRelayAgentOption relayAgentOption = (DhcpRelayAgentOption) option;
        assertEquals(DHCP.DHCPOptionCode.OptionCode_CircuitID.getValue(), relayAgentOption.code);
        assertEquals(12, relayAgentOption.length);
        DhcpOption subOption = relayAgentOption
                .getSubOption(DhcpRelayAgentOption.RelayAgentInfoOptions.CIRCUIT_ID.getValue());
        assertEquals(10, subOption.getLength());
        assertArrayEquals(CIRCUIT_ID.getBytes(Charsets.US_ASCII), subOption.getData());

        option = dhcp.getOptions().get(6);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_END.getValue(), option.code);
        assertEquals(0, option.length);
    }

    /**
     * Tests deserialize discover packet.
     */
    @Test
    public void testDeserializeAck() throws Exception {
        byte[] data = Resources.toByteArray(Dhcp6RelayTest.class.getResource(ACK));
        Ethernet eth = Ethernet.deserializer().deserialize(data, 0, data.length);
        DHCP dhcp = (DHCP) eth.getPayload().getPayload().getPayload();

        assertEquals(DHCP.OPCODE_REPLY, dhcp.getOpCode());
        assertEquals(HW_TYPE, dhcp.getHardwareType());
        assertEquals(HW_ADDR_LEN, dhcp.getHardwareAddressLength());
        assertEquals(HOPS, dhcp.getHops());
        assertEquals(XID, dhcp.getTransactionId());
        assertEquals(SECS, dhcp.getSeconds());
        assertEquals(FLAGS, dhcp.getFlags());
        assertEquals(NO_IP, dhcp.getClientIPAddress());
        assertEquals(CLIENT_IP.toInt(), dhcp.getYourIPAddress());
        assertEquals(SERVER_IP.toInt(), dhcp.getServerIPAddress());
        assertEquals(GW_IP.toInt(), dhcp.getGatewayIPAddress());
        assertTrue(Arrays.equals(CLIENT_HW_ADDR.toBytes(), dhcp.getClientHardwareAddress()));
        assertEquals(EMPTY, dhcp.getServerName());
        assertEquals(EMPTY, dhcp.getBootFileName());
        assertEquals(9, dhcp.getOptions().size());

        DhcpOption option = dhcp.getOptions().get(0);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_MessageType.getValue(), option.code);
        assertEquals(1, option.length);
        assertEquals(DHCP.MsgType.DHCPACK.getValue(), (int) option.getData()[0]);

        option = dhcp.getOptions().get(1);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_DHCPServerIp.getValue(), option.code);
        assertEquals(4, option.length);
        assertArrayEquals(SERVER_IP.toOctets(), option.getData());

        option = dhcp.getOptions().get(2);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_LeaseTime.getValue(), option.code);
        assertEquals(4, option.length);
        assertArrayEquals(new byte[]{0, 0, 2, 88}, option.getData());

        option = dhcp.getOptions().get(3);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_SubnetMask.getValue(), option.code);
        assertEquals(4, option.length);
        assertArrayEquals(SUBNET_MASK.toOctets(), option.getData());

        option = dhcp.getOptions().get(4);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_RouterAddress.getValue(), option.code);
        assertEquals(4, option.length);
        assertArrayEquals(GW_IP.toOctets(), option.getData());

        option = dhcp.getOptions().get(5);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_DomainName.getValue(), option.code);
        assertEquals(13, option.length);
        assertArrayEquals(DOMAIN_NAME.getBytes(Charsets.US_ASCII), option.getData());

        option = dhcp.getOptions().get(6);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_DomainServer.getValue(), option.code);
        assertEquals(8, option.length);
        assertArrayEquals(ArrayUtils.addAll(DNS_1.toOctets(), DNS_2.toOctets()),
                          option.getData());

        option = dhcp.getOptions().get(7);
        assertTrue(option instanceof DhcpRelayAgentOption);
        DhcpRelayAgentOption relayAgentOption = (DhcpRelayAgentOption) option;
        assertEquals(DHCP.DHCPOptionCode.OptionCode_CircuitID.getValue(), relayAgentOption.code);
        assertEquals(12, relayAgentOption.length);
        DhcpOption subOption = relayAgentOption
                .getSubOption(DhcpRelayAgentOption.RelayAgentInfoOptions.CIRCUIT_ID.getValue());
        assertEquals(10, subOption.getLength());
        assertArrayEquals(CIRCUIT_ID.getBytes(Charsets.US_ASCII), subOption.getData());

        option = dhcp.getOptions().get(8);
        assertEquals(DHCP.DHCPOptionCode.OptionCode_END.getValue(), option.code);
        assertEquals(0, option.length);
    }

    /**
     * Test option with option length > 128.
     */
    @Test
    public void longOptionTest() throws Exception {
        byte[] data = Resources.toByteArray(Dhcp6RelayTest.class.getResource(LONG_OPT));
        DHCP dhcp = DHCP.deserializer().deserialize(data, 0, data.length);
        assertEquals(2, dhcp.getOptions().size());
        DhcpOption hostnameOption = dhcp.getOption(DHCP.DHCPOptionCode.OptionCode_HostName);
        DhcpOption endOption = dhcp.getOption(DHCP.DHCPOptionCode.OptionCode_END);
        assertNotNull(hostnameOption);
        assertNotNull(endOption);

        // Host name contains 200 "A"
        StringBuilder hostnameBuilder = new StringBuilder();
        IntStream.range(0, 200).forEach(i -> hostnameBuilder.append("A"));
        String hostname = hostnameBuilder.toString();

        assertEquals((byte) 200, hostnameOption.getLength());
        assertArrayEquals(hostname.getBytes(Charsets.US_ASCII), hostnameOption.getData());
    }
}
