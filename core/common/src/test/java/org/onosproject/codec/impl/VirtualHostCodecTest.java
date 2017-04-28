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
package org.onosproject.codec.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.virtual.DefaultVirtualHost;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualHost;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.NetTestTools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests VirtualHostCodec class.
 */

public class VirtualHostCodecTest {

    private static final String TEST_IP1 = "1.1.1.1";
    private static final String TEST_IP2 = "2.2.2.2";
    private static final String TEST_HOST_ID = "12:34:56:78:90:11/1";
    private static final String TEST_MAC_ADDRESS = "11:11:22:22:33:33";
    private static final long TEST_NETWORK_ID = 44L;
    private static final short TEST_VLAN_ID = (short) 12;
    private static final ConnectPoint CONNECT_POINT =
            NetTestTools.connectPoint("d1", 1);

    @Test
    public void testEncode() {
        MockCodecContext context = new MockCodecContext();
        NetworkId networkId = NetworkId.networkId(TEST_NETWORK_ID);
        HostId id = NetTestTools.hid(TEST_HOST_ID);
        MacAddress mac = MacAddress.valueOf(TEST_MAC_ADDRESS);
        VlanId vlan = VlanId.vlanId(TEST_VLAN_ID);
        HostLocation location =
                new HostLocation(CONNECT_POINT, 0L);
        Set<IpAddress> ips = ImmutableSet.of(IpAddress.valueOf(TEST_IP1),
                                             IpAddress.valueOf(TEST_IP2));
        VirtualHost host =
                new DefaultVirtualHost(networkId, id, mac, vlan, location, ips);
        JsonCodec<VirtualHost> codec = context.codec(VirtualHost.class);
        ObjectNode node = codec.encode(host, context);

        assertThat(node.get(VirtualHostCodec.NETWORK_ID).asLong(),
                   is(TEST_NETWORK_ID));
        assertThat(node.get(VirtualHostCodec.HOST_ID).asText(),
                   is(TEST_HOST_ID));
        assertThat(node.get(VirtualHostCodec.MAC_ADDRESS).asText(),
                   is(TEST_MAC_ADDRESS));
        assertThat(node.get(VirtualHostCodec.VLAN).asInt(),
                   is((int) TEST_VLAN_ID));
        assertThat(node.get(VirtualHostCodec.HOST_LOCATION).get(0).get("elementId").asText(),
                   is(location.deviceId().toString()));
        assertThat(node.get(VirtualHostCodec.HOST_LOCATION).get(0).get("port").asLong(),
                   is(location.port().toLong()));

        JsonNode jsonIps = node.get(VirtualHostCodec.IP_ADDRESSES);
        assertThat(jsonIps, notNullValue());
        assertThat(jsonIps.isArray(), is(true));
        assertThat(jsonIps.size(), is(ips.size()));

        IntStream.of(0, 1).forEach(index ->
            assertThat(jsonIps.get(index).asText(),
                       isOneOf(TEST_IP1, TEST_IP2)));
    }

    @Test
    public void testDecode() throws IOException {
        MockCodecContext context = new MockCodecContext();
        InputStream jsonStream =
                VirtualHostCodecTest.class.getResourceAsStream("VirtualHost.json");
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        JsonCodec<VirtualHost> codec = context.codec(VirtualHost.class);
        VirtualHost virtualHost = codec.decode((ObjectNode) json, context);
        assertThat(virtualHost, notNullValue());

        assertThat(virtualHost.networkId().id(),
                   is(TEST_NETWORK_ID));
        assertThat(virtualHost.id().toString(),
                   is(NetTestTools.hid(TEST_MAC_ADDRESS + "/12").toString()));
        assertThat(virtualHost.mac().toString(),
                   is(TEST_MAC_ADDRESS));
        assertThat(virtualHost.vlan().id(),
                   is((short) TEST_VLAN_ID));
        assertThat(virtualHost.location().deviceId(),
                   is(CONNECT_POINT.deviceId()));
        assertThat(virtualHost.location().port().toLong(),
                   is(CONNECT_POINT.port().toLong()));


        assertThat(virtualHost.ipAddresses().contains(IpAddress.valueOf(TEST_IP1)),
                   is(true));
        assertThat(virtualHost.ipAddresses().contains(IpAddress.valueOf(TEST_IP2)),
                   is(true));
    }
}
