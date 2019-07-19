/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.segmentrouting.xconnect.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.MockCodecContext;
import org.onosproject.net.DeviceId;

import java.io.InputStream;
import java.util.Set;

import static org.junit.Assert.*;

public class XconnectCodecTest {
    private static final DeviceId DEVICE_ID = DeviceId.deviceId("of:1");
    private static final VlanId VLAN_VID = VlanId.vlanId((short) 10);
    private static final XconnectKey KEY = new XconnectKey(DEVICE_ID, VLAN_VID);
    private static final XconnectEndpoint EP1 = XconnectEndpoint.fromString("1");
    private static final XconnectEndpoint EP2 = XconnectEndpoint.fromString("2");
    private static final XconnectEndpoint EP3 = XconnectEndpoint.fromString("LB:5");

    private CodecContext context;
    private JsonCodec<XconnectDesc> codec;

    @Before
    public void setUp() throws Exception {
        context = new MockCodecContext();
        codec = new XconnectCodec();
    }

    @Test
    public void testEncodePort() throws Exception {
        Set<XconnectEndpoint> endpoints1 = Sets.newHashSet(EP1, EP2);
        XconnectDesc desc1 = new XconnectDesc(KEY, endpoints1);

        ObjectMapper mapper = new ObjectMapper();
        InputStream jsonStream1 = XconnectCodecTest.class.getResourceAsStream("/xconnect1.json");
        JsonNode expected = mapper.readTree(jsonStream1);

        JsonNode actual = codec.encode(desc1, context);

        assertEquals(expected.get(XconnectCodec.DEVICE_ID), actual.get(XconnectCodec.DEVICE_ID));
        assertEquals(expected.get(XconnectCodec.VLAN_ID).asInt(), actual.get(XconnectCodec.VLAN_ID).asInt());
        assertEquals(expected.get(XconnectCodec.ENDPOINTS), actual.get(XconnectCodec.ENDPOINTS));
    }

    @Test
    public void testDecodePort() throws Exception {
        Set<XconnectEndpoint> endpoints1 = Sets.newHashSet(EP1, EP2);
        XconnectDesc expected = new XconnectDesc(KEY, endpoints1);

        ObjectMapper mapper = new ObjectMapper();
        InputStream jsonStream1 = XconnectCodecTest.class.getResourceAsStream("/xconnect1.json");
        ObjectNode objectNode = mapper.readTree(jsonStream1).deepCopy();

        XconnectDesc actual = codec.decode(objectNode, context);

        assertEquals(expected, actual);
    }

    @Test
    public void testEncodeLb() throws Exception {
        Set<XconnectEndpoint> endpoints1 = Sets.newHashSet(EP1, EP3);
        XconnectDesc desc1 = new XconnectDesc(KEY, endpoints1);

        ObjectMapper mapper = new ObjectMapper();
        InputStream jsonStream1 = XconnectCodecTest.class.getResourceAsStream("/xconnect2.json");
        JsonNode expected = mapper.readTree(jsonStream1);

        JsonNode actual = codec.encode(desc1, context);

        assertEquals(expected.get(XconnectCodec.DEVICE_ID), actual.get(XconnectCodec.DEVICE_ID));
        assertEquals(expected.get(XconnectCodec.VLAN_ID).asInt(), actual.get(XconnectCodec.VLAN_ID).asInt());
        assertEquals(expected.get(XconnectCodec.ENDPOINTS), actual.get(XconnectCodec.ENDPOINTS));
    }

    @Test
    public void testDecodeLb() throws Exception {
        Set<XconnectEndpoint> endpoints1 = Sets.newHashSet(EP1, EP3);
        XconnectDesc expected = new XconnectDesc(KEY, endpoints1);

        ObjectMapper mapper = new ObjectMapper();
        InputStream jsonStream1 = XconnectCodecTest.class.getResourceAsStream("/xconnect2.json");
        ObjectNode objectNode = mapper.readTree(jsonStream1).deepCopy();

        XconnectDesc actual = codec.decode(objectNode, context);

        assertEquals(expected, actual);
    }
}