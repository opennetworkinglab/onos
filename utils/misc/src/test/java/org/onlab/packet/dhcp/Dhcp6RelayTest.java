/*
 * Copyright 2017-present Open Networking Foundation
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
 *
 */

package org.onlab.packet.dhcp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import org.junit.Test;
import org.onlab.packet.DHCP6;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.UDP;

import java.util.List;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.*;

/**
 * Test serializing/deserializing DHCPv6 relay message.
 */
public class Dhcp6RelayTest {
    private static final String SOLICIT = "dhcp6_relay_solicit.bin";
    private static final String ADVERTISE = "dhcp6_relay_advertise.bin";
    private static final String REQUEST = "dhcp6_relay_request.bin";
    private static final String REPLY = "dhcp6_relay_reply.bin";

    private static final int HOP_COUNT = 0;
    private static final Ip6Address LINK_ADDRESS = Ip6Address.valueOf("2000::2ff");
    private static final Ip6Address PEER_ADDRESS = Ip6Address.valueOf("fe80::2bb:ff:fe00:1");
    private static final int XID_1 = 8135067;
    private static final int XID_2 = 14742082;
    private static final int IA_ID = 1;
    private static final int T1_CLIENT = 3600;
    private static final int T2_CLIENT = 5400;
    private static final int T1_SERVER = 0;
    private static final int T2_SERVER = 0;
    private static final Ip6Address IA_ADDRESS = Ip6Address.valueOf("2000::201");
    private static final int PREFFERRED_LT_SERVER = 375;
    private static final int VALID_LT_SERVER = 600;
    private static final int PREFFERRED_LT_REQ = 7200;
    private static final int VALID_LT_REQ = 10800;
    private static final int VALID_LT_REQ_2 = 7500;
    private static final MacAddress DOWNSTREAM_MAC = MacAddress.valueOf("4a:c0:c2:78:92:34");
    private static final MacAddress CLIENT_MAC = MacAddress.valueOf("00:bb:00:00:00:01");
    private static final int CLIENT_DUID_TIME = 555636143;
    private static final MacAddress IPV6_MCAST = MacAddress.valueOf("33:33:00:01:00:03");
    private static final Ip6Address DOWNSTREAM_LL = Ip6Address.valueOf("fe80::48c0:c2ff:fe78:9234");
    private static final Ip6Address DHCP6_BRC = Ip6Address.valueOf("ff05::1:3");
    private static final Ip6Address SERVER_IP = Ip6Address.valueOf("2000::9903");
    private static final MacAddress SERVER_MAC = MacAddress.valueOf("00:99:66:00:00:01");
    private static final Ip6Address SERVER_LL = Ip6Address.valueOf("fe80::299:66ff:fe00:1");


    /**
     * Test deserialize relay message with solicit message.
     *
     * @throws Exception exception while deserialize the DHCPv6 payload
     */
    @Test
    public void deserializeSolicit() throws Exception {
        byte[] data = Resources.toByteArray(Dhcp6RelayTest.class.getResource(SOLICIT));
        Ethernet eth = Ethernet.deserializer().deserialize(data, 0, data.length);
        DHCP6 relayMsg = (DHCP6) eth.getPayload().getPayload().getPayload();
        assertEquals(relayMsg.getMsgType(), DHCP6.MsgType.RELAY_FORW.value());
        assertEquals(relayMsg.getHopCount(), HOP_COUNT);
        assertEquals(relayMsg.getIp6LinkAddress(), LINK_ADDRESS);
        assertEquals(relayMsg.getIp6PeerAddress(), PEER_ADDRESS);

        assertEquals(relayMsg.getOptions().size(), 2);
        Dhcp6Option option = relayMsg.getOptions().get(0);
        assertEquals(option.getCode(), DHCP6.OptionCode.SUBSCRIBER_ID.value());
        assertEquals(option.getLength(), 10);
        assertArrayEquals(option.getData(), SERVER_IP.toString().getBytes(US_ASCII));

        option = relayMsg.getOptions().get(1);
        assertEquals(option.getCode(), DHCP6.OptionCode.RELAY_MSG.value());
        assertEquals(option.getLength(), 84);
        assertTrue(option.getPayload() instanceof DHCP6);

        DHCP6 relaiedDhcp6 = (DHCP6) option.getPayload();
        assertEquals(relaiedDhcp6.getMsgType(), DHCP6.MsgType.SOLICIT.value());
        assertEquals(relaiedDhcp6.getTransactionId(), XID_1);
        assertEquals(relaiedDhcp6.getOptions().size(), 4);

        // Client ID
        option = relaiedDhcp6.getOptions().get(0);
        assertTrue(option instanceof Dhcp6ClientIdOption);
        Dhcp6ClientIdOption clientIdOption = (Dhcp6ClientIdOption) option;
        assertEquals(clientIdOption.getCode(), DHCP6.OptionCode.CLIENTID.value());
        assertEquals(clientIdOption.getLength(), 14);
        assertEquals(clientIdOption.getDuid().getDuidType(), Dhcp6Duid.DuidType.DUID_LLT);
        assertEquals(clientIdOption.getDuid().getHardwareType(), 1);
        assertEquals(clientIdOption.getDuid().getDuidTime(), CLIENT_DUID_TIME);
        assertArrayEquals(clientIdOption.getDuid().getLinkLayerAddress(), CLIENT_MAC.toBytes());

        // ORO
        option = relaiedDhcp6.getOptions().get(1);
        assertEquals(option.getCode(), DHCP6.OptionCode.ORO.value());
        assertEquals(option.getLength(), 8);
        assertArrayEquals(option.getData(),
                          new byte[]{0, 23, 0, 24, 0, 39, 0, 31});

        // ELAPSED_TIME
        option = relaiedDhcp6.getOptions().get(2);
        assertEquals(option.getCode(), DHCP6.OptionCode.ELAPSED_TIME.value());
        assertEquals(option.getLength(), 2);
        assertArrayEquals(option.getData(),
                          new byte[]{0, 0});

        // IA NA
        option = relaiedDhcp6.getOptions().get(3);
        assertTrue(option instanceof Dhcp6IaNaOption);
        Dhcp6IaNaOption iaNaOption = (Dhcp6IaNaOption) option;
        assertEquals(iaNaOption.getCode(), DHCP6.OptionCode.IA_NA.value());
        assertEquals(iaNaOption.getLength(), 40);
        assertEquals(iaNaOption.getIaId(), IA_ID);
        assertEquals(iaNaOption.getT1(), T1_CLIENT);
        assertEquals(iaNaOption.getT2(), T2_CLIENT);
        assertEquals(iaNaOption.getOptions().size(), 1);
        Dhcp6IaAddressOption subOption = (Dhcp6IaAddressOption) iaNaOption.getOptions().get(0);
        assertEquals(subOption.getIp6Address(), IA_ADDRESS);
        assertEquals(subOption.getPreferredLifetime(), PREFFERRED_LT_REQ);
        assertEquals(subOption.getValidLifetime(), VALID_LT_REQ);

        assertArrayEquals(data, eth.serialize());
    }

    /**
     * Test serialize relay message with solicit message.
     *
     * @throws Exception exception while serialize the DHCPv6 payload
     */
    @Test
    public void serializeSolicit() throws Exception {
        DHCP6 relayMsg = new DHCP6();
        relayMsg.setMsgType(DHCP6.MsgType.RELAY_FORW.value());
        relayMsg.setHopCount((byte) HOP_COUNT);
        relayMsg.setLinkAddress(LINK_ADDRESS.toOctets());
        relayMsg.setPeerAddress(PEER_ADDRESS.toOctets());

        DHCP6 relaiedDhcp6 = new DHCP6();
        relaiedDhcp6.setMsgType(DHCP6.MsgType.SOLICIT.value());
        relaiedDhcp6.setTransactionId(XID_1);
        List<Dhcp6Option> options = Lists.newArrayList();

        // Client ID
        Dhcp6Duid duid = new Dhcp6Duid();
        duid.setDuidType(Dhcp6Duid.DuidType.DUID_LLT);
        duid.setHardwareType((short) 1);
        duid.setDuidTime(CLIENT_DUID_TIME);
        duid.setLinkLayerAddress(CLIENT_MAC.toBytes());
        Dhcp6ClientIdOption clientIdOption = new Dhcp6ClientIdOption();
        clientIdOption.setDuid(duid);
        options.add(clientIdOption);

        // Option request
        Dhcp6Option option = new Dhcp6Option();
        option.setCode(DHCP6.OptionCode.ORO.value());
        option.setLength((short) 8);
        option.setData(new byte[]{0, 23, 0, 24, 0, 39, 0, 31});
        options.add(option);

        // Elapsed Time
        option = new Dhcp6Option();
        option.setCode(DHCP6.OptionCode.ELAPSED_TIME.value());
        option.setLength((short) 2);
        option.setData(new byte[]{0, 0});
        options.add(option);

        // IA NA
        Dhcp6IaNaOption iaNaOption = new Dhcp6IaNaOption();
        iaNaOption.setIaId(IA_ID);
        iaNaOption.setT1(T1_CLIENT);
        iaNaOption.setT2(T2_CLIENT);
        Dhcp6IaAddressOption iaAddressOption = new Dhcp6IaAddressOption();
        iaAddressOption.setIp6Address(IA_ADDRESS);
        iaAddressOption.setPreferredLifetime(PREFFERRED_LT_REQ);
        iaAddressOption.setValidLifetime(VALID_LT_REQ);
        iaNaOption.setOptions(ImmutableList.of(iaAddressOption));
        options.add(iaNaOption);
        relaiedDhcp6.setOptions(options);

        Dhcp6RelayOption relayOption = new Dhcp6RelayOption();
        relayOption.setPayload(relaiedDhcp6);

        Dhcp6Option subscriberId = new Dhcp6Option();
        subscriberId.setCode(DHCP6.OptionCode.SUBSCRIBER_ID.value());
        subscriberId.setLength((short) 10);
        subscriberId.setData(SERVER_IP.toString().getBytes(US_ASCII));

        relayMsg.setOptions(ImmutableList.of(subscriberId, relayOption));

        UDP udp = new UDP();
        udp.setSourcePort(UDP.DHCP_V6_SERVER_PORT);
        udp.setDestinationPort(UDP.DHCP_V6_SERVER_PORT);
        udp.setPayload(relayMsg);
        udp.setChecksum((short) 0x9a99);

        IPv6 ipv6 = new IPv6();
        ipv6.setHopLimit((byte) 32);
        ipv6.setSourceAddress(DOWNSTREAM_LL.toOctets());
        ipv6.setDestinationAddress(DHCP6_BRC.toOctets());
        ipv6.setNextHeader(IPv6.PROTOCOL_UDP);
        ipv6.setTrafficClass((byte) 0);
        ipv6.setFlowLabel(0x000cbf64);
        ipv6.setPayload(udp);

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(IPV6_MCAST);
        eth.setSourceMACAddress(DOWNSTREAM_MAC);
        eth.setEtherType(Ethernet.TYPE_IPV6);
        eth.setPayload(ipv6);

        assertArrayEquals(Resources.toByteArray(Dhcp6RelayTest.class.getResource(SOLICIT)),
                          eth.serialize());
    }

    /**
     * Test deserialize relay message with advertise message.
     *
     * @throws Exception exception while deserialize the DHCPv6 payload
     */
    @Test
    public void deserializeAdvertise() throws Exception {
        byte[] data = Resources.toByteArray(getClass().getResource(ADVERTISE));
        Ethernet eth = Ethernet.deserializer().deserialize(data, 0, data.length);
        DHCP6 relayMsg = (DHCP6) eth.getPayload().getPayload().getPayload();
        assertEquals(relayMsg.getMsgType(), DHCP6.MsgType.RELAY_REPL.value());
        assertEquals(relayMsg.getHopCount(), HOP_COUNT);
        assertEquals(relayMsg.getIp6LinkAddress(), LINK_ADDRESS);
        assertEquals(relayMsg.getIp6PeerAddress(), PEER_ADDRESS);

        assertEquals(relayMsg.getOptions().size(), 1);
        Dhcp6Option option = relayMsg.getOptions().get(0);
        assertEquals(option.getCode(), DHCP6.OptionCode.RELAY_MSG.value());
        assertEquals(option.getLength(), 84);
        assertTrue(option.getPayload() instanceof DHCP6);

        DHCP6 relaiedDhcp6 = (DHCP6) option.getPayload();
        assertEquals(relaiedDhcp6.getMsgType(), DHCP6.MsgType.ADVERTISE.value());
        assertEquals(relaiedDhcp6.getTransactionId(), XID_1);
        assertEquals(relaiedDhcp6.getOptions().size(), 3);

        // IA NA
        option = relaiedDhcp6.getOptions().get(0);
        assertTrue(option instanceof Dhcp6IaNaOption);
        Dhcp6IaNaOption iaNaOption = (Dhcp6IaNaOption) option;
        assertEquals(iaNaOption.getCode(), DHCP6.OptionCode.IA_NA.value());
        assertEquals(iaNaOption.getLength(), 40);
        assertEquals(iaNaOption.getIaId(), IA_ID);
        assertEquals(iaNaOption.getT1(), T1_SERVER);
        assertEquals(iaNaOption.getT2(), T2_SERVER);
        assertEquals(iaNaOption.getOptions().size(), 1);

        // IA Address (in IA NA)
        assertTrue(iaNaOption.getOptions().get(0) instanceof Dhcp6IaAddressOption);
        Dhcp6IaAddressOption iaAddressOption =
                (Dhcp6IaAddressOption) iaNaOption.getOptions().get(0);
        assertEquals(iaAddressOption.getIp6Address(), IA_ADDRESS);
        assertEquals(iaAddressOption.getPreferredLifetime(), PREFFERRED_LT_SERVER);
        assertEquals(iaAddressOption.getValidLifetime(), VALID_LT_SERVER);
        assertNull(iaAddressOption.getOptions());

        // Client ID
        option = relaiedDhcp6.getOptions().get(1);
        assertTrue(option instanceof Dhcp6ClientIdOption);
        Dhcp6ClientIdOption clientIdOption = (Dhcp6ClientIdOption) option;
        assertEquals(clientIdOption.getCode(), DHCP6.OptionCode.CLIENTID.value());
        assertEquals(clientIdOption.getLength(), 14);
        assertEquals(clientIdOption.getDuid().getDuidType(), Dhcp6Duid.DuidType.DUID_LLT);
        assertEquals(clientIdOption.getDuid().getHardwareType(), 1);
        assertEquals(clientIdOption.getDuid().getDuidTime(), CLIENT_DUID_TIME);
        assertArrayEquals(clientIdOption.getDuid().getLinkLayerAddress(), CLIENT_MAC.toBytes());

        // Server ID
        option = relaiedDhcp6.getOptions().get(2);
        assertEquals(option.getCode(), DHCP6.OptionCode.SERVERID.value());
        assertEquals(option.getLength(), 14);
        Dhcp6Duid serverDuid =
                Dhcp6Duid.deserializer().deserialize(option.getData(), 0, option.getData().length);
        assertEquals(serverDuid.getDuidType(), Dhcp6Duid.DuidType.DUID_LLT);
        assertEquals(serverDuid.getDuidTime(), 0x211e5340);
        assertEquals(serverDuid.getHardwareType(), 1);
        assertArrayEquals(serverDuid.getLinkLayerAddress(), SERVER_MAC.toBytes());
        assertArrayEquals(data, eth.serialize());
    }

    /**
     * Test serialize relay message with advertise message.
     *
     * @throws Exception exception while serialize the DHCPv6 payload
     */
    @Test
    public void serializeAdvertise() throws Exception {
        DHCP6 relayMsg = new DHCP6();
        relayMsg.setMsgType(DHCP6.MsgType.RELAY_REPL.value());
        relayMsg.setHopCount((byte) HOP_COUNT);
        relayMsg.setLinkAddress(LINK_ADDRESS.toOctets());
        relayMsg.setPeerAddress(PEER_ADDRESS.toOctets());

        DHCP6 relaiedDhcp6 = new DHCP6();
        relaiedDhcp6.setMsgType(DHCP6.MsgType.ADVERTISE.value());
        relaiedDhcp6.setTransactionId(XID_1);
        List<Dhcp6Option> options = Lists.newArrayList();

        // IA address
        Dhcp6IaAddressOption iaAddressOption = new Dhcp6IaAddressOption();
        iaAddressOption.setIp6Address(IA_ADDRESS);
        iaAddressOption.setPreferredLifetime(PREFFERRED_LT_SERVER);
        iaAddressOption.setValidLifetime(VALID_LT_SERVER);

        // IA NA
        Dhcp6IaNaOption iaNaOption = new Dhcp6IaNaOption();
        iaNaOption.setIaId(IA_ID);
        iaNaOption.setT1(T1_SERVER);
        iaNaOption.setT2(T2_SERVER);
        iaNaOption.setOptions(ImmutableList.of(iaAddressOption));
        options.add(iaNaOption);

        // Client ID
        Dhcp6Duid duid = new Dhcp6Duid();
        duid.setDuidType(Dhcp6Duid.DuidType.DUID_LLT);
        duid.setHardwareType((short) 1);
        duid.setDuidTime(CLIENT_DUID_TIME);
        duid.setLinkLayerAddress(CLIENT_MAC.toBytes());
        Dhcp6ClientIdOption clientIdOption = new Dhcp6ClientIdOption();
        clientIdOption.setDuid(duid);
        options.add(clientIdOption);

        // Server ID
        Dhcp6Option option = new Dhcp6Option();
        option.setCode(DHCP6.OptionCode.SERVERID.value());
        option.setLength((short) 14);
        Dhcp6Duid serverDuid = new Dhcp6Duid();
        serverDuid.setDuidType(Dhcp6Duid.DuidType.DUID_LLT);
        serverDuid.setLinkLayerAddress(SERVER_MAC.toBytes());
        serverDuid.setHardwareType((short) 1);
        serverDuid.setDuidTime(0x211e5340);
        option.setData(serverDuid.serialize());
        options.add(option);

        relaiedDhcp6.setOptions(options);

        Dhcp6RelayOption relayOption = new Dhcp6RelayOption();
        relayOption.setPayload(relaiedDhcp6);

        relayMsg.setOptions(ImmutableList.of(relayOption));

        UDP udp = new UDP();
        udp.setSourcePort(UDP.DHCP_V6_SERVER_PORT);
        udp.setDestinationPort(UDP.DHCP_V6_SERVER_PORT);
        udp.setPayload(relayMsg);
        udp.setChecksum((short) 0x0000019d);

        IPv6 ipv6 = new IPv6();
        ipv6.setHopLimit((byte) 64);
        ipv6.setSourceAddress(SERVER_LL.toOctets());
        ipv6.setDestinationAddress(DOWNSTREAM_LL.toOctets());
        ipv6.setNextHeader(IPv6.PROTOCOL_UDP);
        ipv6.setTrafficClass((byte) 0);
        ipv6.setFlowLabel(0x000c72ef);
        ipv6.setPayload(udp);

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(DOWNSTREAM_MAC);
        eth.setSourceMACAddress(SERVER_MAC);
        eth.setEtherType(Ethernet.TYPE_IPV6);
        eth.setPayload(ipv6);
        assertArrayEquals(Resources.toByteArray(Dhcp6RelayTest.class.getResource(ADVERTISE)),
                          eth.serialize());
    }

    /**
     * Test deserialize relay message with request message.
     *
     * @throws Exception exception while deserialize the DHCPv6 payload
     */
    @Test
    public void deserializeRequest() throws Exception {
        byte[] data = Resources.toByteArray(getClass().getResource(REQUEST));
        Ethernet eth = Ethernet.deserializer().deserialize(data, 0, data.length);
        DHCP6 relayMsg = (DHCP6) eth.getPayload().getPayload().getPayload();
        assertEquals(relayMsg.getMsgType(), DHCP6.MsgType.RELAY_FORW.value());
        assertEquals(relayMsg.getHopCount(), HOP_COUNT);
        assertEquals(relayMsg.getIp6LinkAddress(), LINK_ADDRESS);
        assertEquals(relayMsg.getIp6PeerAddress(), PEER_ADDRESS);

        assertEquals(relayMsg.getOptions().size(), 2);
        Dhcp6Option option = relayMsg.getOptions().get(0);
        assertEquals(option.getCode(), DHCP6.OptionCode.SUBSCRIBER_ID.value());
        assertEquals(option.getLength(), 10);
        assertArrayEquals(option.getData(), SERVER_IP.toString().getBytes(US_ASCII));

        option = relayMsg.getOptions().get(1);
        assertEquals(option.getCode(), DHCP6.OptionCode.RELAY_MSG.value());
        assertEquals(option.getLength(), 102);
        assertTrue(option.getPayload() instanceof DHCP6);

        DHCP6 relaiedDhcp6 = (DHCP6) option.getPayload();
        assertEquals(relaiedDhcp6.getMsgType(), DHCP6.MsgType.REQUEST.value());
        assertEquals(relaiedDhcp6.getTransactionId(), XID_2);
        assertEquals(relaiedDhcp6.getOptions().size(), 5);

        // Client ID
        option = relaiedDhcp6.getOptions().get(0);
        assertTrue(option instanceof Dhcp6ClientIdOption);
        Dhcp6ClientIdOption clientIdOption = (Dhcp6ClientIdOption) option;
        assertEquals(clientIdOption.getCode(), DHCP6.OptionCode.CLIENTID.value());
        assertEquals(clientIdOption.getLength(), 14);
        assertEquals(clientIdOption.getDuid().getDuidType(), Dhcp6Duid.DuidType.DUID_LLT);
        assertEquals(clientIdOption.getDuid().getHardwareType(), 1);
        assertEquals(clientIdOption.getDuid().getDuidTime(), CLIENT_DUID_TIME);
        assertArrayEquals(clientIdOption.getDuid().getLinkLayerAddress(), CLIENT_MAC.toBytes());

        // Server ID
        option = relaiedDhcp6.getOptions().get(1);
        assertEquals(option.getCode(), DHCP6.OptionCode.SERVERID.value());
        assertEquals(option.getLength(), 14);
        Dhcp6Duid serverDuid =
                Dhcp6Duid.deserializer().deserialize(option.getData(), 0, option.getData().length);
        assertEquals(serverDuid.getDuidType(), Dhcp6Duid.DuidType.DUID_LLT);
        assertEquals(serverDuid.getDuidTime(), 0x211e5340);
        assertEquals(serverDuid.getHardwareType(), 1);
        assertArrayEquals(serverDuid.getLinkLayerAddress(), SERVER_MAC.toBytes());

        // Option Request
        option = relaiedDhcp6.getOptions().get(2);
        assertEquals(option.getCode(), DHCP6.OptionCode.ORO.value());
        assertEquals(option.getLength(), 8);
        assertArrayEquals(option.getData(), new byte[]{0, 23, 0, 24, 0, 39, 0, 31});

        // ELAPSED_TIME
        option = relaiedDhcp6.getOptions().get(3);
        assertEquals(option.getCode(), DHCP6.OptionCode.ELAPSED_TIME.value());
        assertEquals(option.getLength(), 2);
        assertArrayEquals(option.getData(), new byte[]{0, 0});

        // IA NA
        option = relaiedDhcp6.getOptions().get(4);
        assertTrue(option instanceof Dhcp6IaNaOption);
        Dhcp6IaNaOption iaNaOption = (Dhcp6IaNaOption) option;
        assertEquals(iaNaOption.getCode(), DHCP6.OptionCode.IA_NA.value());
        assertEquals(iaNaOption.getLength(), 40);
        assertEquals(iaNaOption.getIaId(), IA_ID);
        assertEquals(iaNaOption.getT1(), T1_CLIENT);
        assertEquals(iaNaOption.getT2(), T2_CLIENT);
        assertEquals(iaNaOption.getOptions().size(), 1);

        // IA Address (in IA NA)
        assertTrue(iaNaOption.getOptions().get(0) instanceof Dhcp6IaAddressOption);
        Dhcp6IaAddressOption iaAddressOption =
                (Dhcp6IaAddressOption) iaNaOption.getOptions().get(0);
        assertEquals(iaAddressOption.getIp6Address(), IA_ADDRESS);
        assertEquals(iaAddressOption.getPreferredLifetime(), PREFFERRED_LT_REQ);
        assertEquals(iaAddressOption.getValidLifetime(), VALID_LT_REQ_2);
        assertNull(iaAddressOption.getOptions());

        assertArrayEquals(data, eth.serialize());
    }

    /**
     * Test serialize relay message with request message.
     *
     * @throws Exception exception while serialize the DHCPv6 payload
     */
    @Test
    public void serializeRequest() throws Exception {
        DHCP6 relayMsg = new DHCP6();
        relayMsg.setMsgType(DHCP6.MsgType.RELAY_FORW.value());
        relayMsg.setHopCount((byte) HOP_COUNT);
        relayMsg.setLinkAddress(LINK_ADDRESS.toOctets());
        relayMsg.setPeerAddress(PEER_ADDRESS.toOctets());

        DHCP6 relaiedDhcp6 = new DHCP6();
        relaiedDhcp6.setMsgType(DHCP6.MsgType.REQUEST.value());
        relaiedDhcp6.setTransactionId(XID_2);
        List<Dhcp6Option> options = Lists.newArrayList();

        // Client ID
        Dhcp6Duid duid = new Dhcp6Duid();
        duid.setDuidType(Dhcp6Duid.DuidType.DUID_LLT);
        duid.setHardwareType((short) 1);
        duid.setDuidTime(CLIENT_DUID_TIME);
        duid.setLinkLayerAddress(CLIENT_MAC.toBytes());
        Dhcp6ClientIdOption clientIdOption = new Dhcp6ClientIdOption();
        clientIdOption.setDuid(duid);
        options.add(clientIdOption);

        // Server ID
        Dhcp6Option option = new Dhcp6Option();
        option.setCode(DHCP6.OptionCode.SERVERID.value());
        option.setLength((short) 14);
        Dhcp6Duid serverDuid = new Dhcp6Duid();
        serverDuid.setDuidType(Dhcp6Duid.DuidType.DUID_LLT);
        serverDuid.setLinkLayerAddress(SERVER_MAC.toBytes());
        serverDuid.setHardwareType((short) 1);
        serverDuid.setDuidTime(0x211e5340);
        option.setData(serverDuid.serialize());
        options.add(option);

        // Option request
        option = new Dhcp6Option();
        option.setCode(DHCP6.OptionCode.ORO.value());
        option.setLength((short) 8);
        option.setData(new byte[]{0, 23, 0, 24, 0, 39, 0, 31});
        options.add(option);

        // Elapsed Time
        option = new Dhcp6Option();
        option.setCode(DHCP6.OptionCode.ELAPSED_TIME.value());
        option.setLength((short) 2);
        option.setData(new byte[]{0, 0});
        options.add(option);

        // IA address
        Dhcp6IaAddressOption iaAddressOption = new Dhcp6IaAddressOption();
        iaAddressOption.setIp6Address(IA_ADDRESS);
        iaAddressOption.setPreferredLifetime(PREFFERRED_LT_REQ);
        iaAddressOption.setValidLifetime(VALID_LT_REQ_2);

        // IA NA
        Dhcp6IaNaOption iaNaOption = new Dhcp6IaNaOption();
        iaNaOption.setIaId(IA_ID);
        iaNaOption.setT1(T1_CLIENT);
        iaNaOption.setT2(T2_CLIENT);
        iaNaOption.setOptions(ImmutableList.of(iaAddressOption));
        options.add(iaNaOption);

        relaiedDhcp6.setOptions(options);

        Dhcp6Option subscriberId = new Dhcp6Option();
        subscriberId.setCode(DHCP6.OptionCode.SUBSCRIBER_ID.value());
        subscriberId.setLength((short) 10);
        subscriberId.setData(SERVER_IP.toString().getBytes(US_ASCII));

        Dhcp6RelayOption relayOption = new Dhcp6RelayOption();
        relayOption.setPayload(relaiedDhcp6);

        relayMsg.setOptions(ImmutableList.of(subscriberId, relayOption));

        UDP udp = new UDP();
        udp.setSourcePort(UDP.DHCP_V6_SERVER_PORT);
        udp.setDestinationPort(UDP.DHCP_V6_SERVER_PORT);
        udp.setPayload(relayMsg);
        udp.setChecksum((short) 0x9aab);

        IPv6 ipv6 = new IPv6();
        ipv6.setHopLimit((byte) 32);
        ipv6.setSourceAddress(DOWNSTREAM_LL.toOctets());
        ipv6.setDestinationAddress(DHCP6_BRC.toOctets());
        ipv6.setNextHeader(IPv6.PROTOCOL_UDP);
        ipv6.setTrafficClass((byte) 0);
        ipv6.setFlowLabel(0x000cbf64);
        ipv6.setPayload(udp);

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(IPV6_MCAST);
        eth.setSourceMACAddress(DOWNSTREAM_MAC);
        eth.setEtherType(Ethernet.TYPE_IPV6);
        eth.setPayload(ipv6);

        assertArrayEquals(Resources.toByteArray(Dhcp6RelayTest.class.getResource(REQUEST)),
                          eth.serialize());
    }

    /**
     * Test deserialize relay message with reply message.
     *
     * @throws Exception exception while deserialize the DHCPv6 payload
     */
    @Test
    public void deserializeReply() throws Exception {
        byte[] data = Resources.toByteArray(getClass().getResource(REPLY));
        Ethernet eth = Ethernet.deserializer().deserialize(data, 0, data.length);
        DHCP6 relayMsg = (DHCP6) eth.getPayload().getPayload().getPayload();
        assertEquals(relayMsg.getMsgType(), DHCP6.MsgType.RELAY_REPL.value());
        assertEquals(relayMsg.getHopCount(), HOP_COUNT);
        assertEquals(relayMsg.getIp6LinkAddress(), LINK_ADDRESS);
        assertEquals(relayMsg.getIp6PeerAddress(), PEER_ADDRESS);

        assertEquals(relayMsg.getOptions().size(), 1);
        Dhcp6Option option = relayMsg.getOptions().get(0);
        assertEquals(option.getCode(), DHCP6.OptionCode.RELAY_MSG.value());
        assertEquals(option.getLength(), 84);
        assertTrue(option.getPayload() instanceof DHCP6);

        DHCP6 relaiedDhcp6 = (DHCP6) option.getPayload();
        assertEquals(relaiedDhcp6.getMsgType(), DHCP6.MsgType.REPLY.value());
        assertEquals(relaiedDhcp6.getTransactionId(), XID_2);
        assertEquals(relaiedDhcp6.getOptions().size(), 3);

        // IA NA
        option = relaiedDhcp6.getOptions().get(0);
        assertTrue(option instanceof Dhcp6IaNaOption);
        Dhcp6IaNaOption iaNaOption = (Dhcp6IaNaOption) option;
        assertEquals(iaNaOption.getCode(), DHCP6.OptionCode.IA_NA.value());
        assertEquals(iaNaOption.getLength(), 40);
        assertEquals(iaNaOption.getIaId(), IA_ID);
        assertEquals(iaNaOption.getT1(), T1_SERVER);
        assertEquals(iaNaOption.getT2(), T2_SERVER);
        assertEquals(iaNaOption.getOptions().size(), 1);

        // IA Address (in IA NA)
        assertTrue(iaNaOption.getOptions().get(0) instanceof Dhcp6IaAddressOption);
        Dhcp6IaAddressOption iaAddressOption =
                (Dhcp6IaAddressOption) iaNaOption.getOptions().get(0);
        assertEquals(iaAddressOption.getIp6Address(), IA_ADDRESS);
        assertEquals(iaAddressOption.getPreferredLifetime(), PREFFERRED_LT_SERVER);
        assertEquals(iaAddressOption.getValidLifetime(), VALID_LT_SERVER);
        assertNull(iaAddressOption.getOptions());

        // Client ID
        option = relaiedDhcp6.getOptions().get(1);
        assertTrue(option instanceof Dhcp6ClientIdOption);
        Dhcp6ClientIdOption clientIdOption = (Dhcp6ClientIdOption) option;
        assertEquals(clientIdOption.getCode(), DHCP6.OptionCode.CLIENTID.value());
        assertEquals(clientIdOption.getLength(), 14);
        assertEquals(clientIdOption.getDuid().getDuidType(), Dhcp6Duid.DuidType.DUID_LLT);
        assertEquals(clientIdOption.getDuid().getHardwareType(), 1);
        assertEquals(clientIdOption.getDuid().getDuidTime(), CLIENT_DUID_TIME);
        assertArrayEquals(clientIdOption.getDuid().getLinkLayerAddress(), CLIENT_MAC.toBytes());

        // Server ID
        option = relaiedDhcp6.getOptions().get(2);
        assertEquals(option.getCode(), DHCP6.OptionCode.SERVERID.value());
        assertEquals(option.getLength(), 14);
        Dhcp6Duid serverDuid =
                Dhcp6Duid.deserializer().deserialize(option.getData(), 0, option.getData().length);
        assertEquals(serverDuid.getDuidType(), Dhcp6Duid.DuidType.DUID_LLT);
        assertEquals(serverDuid.getDuidTime(), 0x211e5340);
        assertEquals(serverDuid.getHardwareType(), 1);
        assertArrayEquals(serverDuid.getLinkLayerAddress(), SERVER_MAC.toBytes());

        assertArrayEquals(data, eth.serialize());
    }

    @Test
    public void serializeReply() throws Exception {
        DHCP6 relayMsg = new DHCP6();
        relayMsg.setMsgType(DHCP6.MsgType.RELAY_REPL.value());
        relayMsg.setHopCount((byte) HOP_COUNT);
        relayMsg.setLinkAddress(LINK_ADDRESS.toOctets());
        relayMsg.setPeerAddress(PEER_ADDRESS.toOctets());

        DHCP6 relaiedDhcp6 = new DHCP6();
        relaiedDhcp6.setMsgType(DHCP6.MsgType.REPLY.value());
        relaiedDhcp6.setTransactionId(XID_2);
        List<Dhcp6Option> options = Lists.newArrayList();

        // IA address
        Dhcp6IaAddressOption iaAddressOption = new Dhcp6IaAddressOption();
        iaAddressOption.setIp6Address(IA_ADDRESS);
        iaAddressOption.setPreferredLifetime(PREFFERRED_LT_SERVER);
        iaAddressOption.setValidLifetime(VALID_LT_SERVER);

        // IA NA
        Dhcp6IaNaOption iaNaOption = new Dhcp6IaNaOption();
        iaNaOption.setIaId(IA_ID);
        iaNaOption.setT1(T1_SERVER);
        iaNaOption.setT2(T2_SERVER);
        iaNaOption.setOptions(ImmutableList.of(iaAddressOption));
        options.add(iaNaOption);

        // Client ID
        Dhcp6Duid duid = new Dhcp6Duid();
        duid.setDuidType(Dhcp6Duid.DuidType.DUID_LLT);
        duid.setHardwareType((short) 1);
        duid.setDuidTime(CLIENT_DUID_TIME);
        duid.setLinkLayerAddress(CLIENT_MAC.toBytes());
        Dhcp6ClientIdOption clientIdOption = new Dhcp6ClientIdOption();
        clientIdOption.setDuid(duid);
        options.add(clientIdOption);

        // Server ID
        Dhcp6Option option = new Dhcp6Option();
        option.setCode(DHCP6.OptionCode.SERVERID.value());
        option.setLength((short) 14);
        Dhcp6Duid serverDuid = new Dhcp6Duid();
        serverDuid.setDuidType(Dhcp6Duid.DuidType.DUID_LLT);
        serverDuid.setLinkLayerAddress(SERVER_MAC.toBytes());
        serverDuid.setHardwareType((short) 1);
        serverDuid.setDuidTime(0x211e5340);
        option.setData(serverDuid.serialize());
        options.add(option);

        relaiedDhcp6.setOptions(options);

        Dhcp6RelayOption relayOption = new Dhcp6RelayOption();
        relayOption.setPayload(relaiedDhcp6);

        relayMsg.setOptions(ImmutableList.of(relayOption));

        UDP udp = new UDP();
        udp.setSourcePort(UDP.DHCP_V6_SERVER_PORT);
        udp.setDestinationPort(UDP.DHCP_V6_SERVER_PORT);
        udp.setPayload(relayMsg);
        udp.setChecksum((short) 0x019d);

        IPv6 ipv6 = new IPv6();
        ipv6.setHopLimit((byte) 64);
        ipv6.setSourceAddress(SERVER_LL.toOctets());
        ipv6.setDestinationAddress(DOWNSTREAM_LL.toOctets());
        ipv6.setNextHeader(IPv6.PROTOCOL_UDP);
        ipv6.setTrafficClass((byte) 0);
        ipv6.setFlowLabel(0x000c72ef);
        ipv6.setPayload(udp);

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(DOWNSTREAM_MAC);
        eth.setSourceMACAddress(SERVER_MAC);
        eth.setEtherType(Ethernet.TYPE_IPV6);
        eth.setPayload(ipv6);

        assertArrayEquals(Resources.toByteArray(Dhcp6RelayTest.class.getResource(REPLY)),
                          eth.serialize());
    }
}
