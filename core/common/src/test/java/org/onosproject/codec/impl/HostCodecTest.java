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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.onosproject.codec.impl.JsonCodecUtils.assertJsonEncodable;

/**
 * Unit test for HostCodec.
 */
public class HostCodecTest {

    // Make sure this Host and the corresponding JSON file have the same values
    private static final MacAddress MAC;
    private static final VlanId VLAN_ID;
    private static final HostId HOST_ID;
    private static final DeviceId DEVICE_ID;
    private static final PortNumber PORT_NUM;
    private static final Set<HostLocation> HOST_LOCATIONS = new HashSet<>();
    private static final PortNumber AUX_PORT_NUM;
    private static final Set<HostLocation> AUX_HOST_LOCATIONS = new HashSet<>();
    private static final Set<IpAddress> IPS;
    private static final VlanId INNER_VLAN_ID;
    private static final EthType OUTER_TPID;
    private static final Annotations ANNOTATIONS;
    private static final Host HOST;

    private MockCodecContext context = new MockCodecContext();
    private JsonCodec<Host> hostCodec = context.codec(Host.class);

    private static final String JSON_FILE = "simple-host.json";

    static {
        // Make sure these members have same values with the corresponding JSON fields
        MAC = MacAddress.valueOf("46:E4:3C:A4:17:C8");
        VLAN_ID = VlanId.vlanId("None");
        HOST_ID = HostId.hostId(MAC, VLAN_ID);
        DEVICE_ID = DeviceId.deviceId("of:0000000000000002");
        PORT_NUM = PortNumber.portNumber("3");
        HOST_LOCATIONS.add(new HostLocation(DEVICE_ID, PORT_NUM, 0));
        AUX_PORT_NUM = PortNumber.portNumber("4");
        AUX_HOST_LOCATIONS.add(new HostLocation(DEVICE_ID, AUX_PORT_NUM, 0));
        IPS = new HashSet<>();
        IPS.add(IpAddress.valueOf("127.0.0.1"));
        INNER_VLAN_ID = VlanId.vlanId("10");
        OUTER_TPID = EthType.EtherType.lookup((short) (Integer.decode("0x88a8") & 0xFFFF)).ethType();
        ANNOTATIONS = DefaultAnnotations.builder().set("key1", "val1").build();
        HOST = new DefaultHost(ProviderId.NONE, HOST_ID, MAC, VLAN_ID,
                               HOST_LOCATIONS, AUX_HOST_LOCATIONS, IPS, INNER_VLAN_ID,
                               OUTER_TPID, false, false, ANNOTATIONS);
    }

    @Test
    public void testCodec() {
        assertNotNull(hostCodec);
        assertJsonEncodable(context, hostCodec, HOST);
    }

    @Test
    public void testDecode() throws IOException {
        InputStream jsonStream = HostCodec.class.getResourceAsStream(JSON_FILE);
        JsonNode jsonString = context.mapper().readTree(jsonStream);

        Host expected = hostCodec.decode((ObjectNode) jsonString, context);
        assertEquals(expected, HOST);
    }

    @Test
    public void testEncode() throws IOException {
        InputStream jsonStream = HostCodec.class.getResourceAsStream(JSON_FILE);
        JsonNode jsonString = context.mapper().readTree(jsonStream);

        ObjectNode expected = hostCodec.encode(HOST, context);
        // Host ID is not a field in Host but rather derived from MAC + VLAN.
        // Derived information should not be part of the JSON really.
        // However, we keep it as is for backward compatibility.
        expected.remove(HostCodec.HOST_ID);

        assertEquals(expected, jsonString);
    }

}