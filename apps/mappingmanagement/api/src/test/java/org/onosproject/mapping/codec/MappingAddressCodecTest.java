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
 */
package org.onosproject.mapping.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.mapping.MappingCodecRegistrator;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.mapping.codec.MappingAddressJsonMatcher.matchesMappingAddress;

/**
 * Unit tests for MappingAddressCodec.
 */
public class MappingAddressCodecTest {

    private CodecContext context;
    private JsonCodec<MappingAddress> addressCodec;
    private MappingCodecRegistrator registrator;
    private static final String IPV4_STRING = "1.2.3.4";
    private static final String PORT_STRING = "32";
    private static final IpPrefix IPV4_PREFIX =
            IpPrefix.valueOf(IPV4_STRING + "/" + PORT_STRING);
    private static final IpPrefix IPV6_PREFIX = IpPrefix.valueOf("fe80::/64");
    private static final MacAddress MAC = MacAddress.valueOf("00:00:11:00:00:01");
    private static final String DN = "onos";
    private static final String AS = "AS1000";

    /**
     * Sets up for each test.
     * Creates a context and fetches the mapping address codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new MappingCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new MappingCodecContextAdapter(registrator.codecService);

        addressCodec = context.codec(MappingAddress.class);
        assertThat(addressCodec, notNullValue());
    }

    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests AS mapping address.
     */
    @Test
    public void asMappingAddressTest() {
        MappingAddress address = MappingAddresses.asMappingAddress(AS);
        ObjectNode result = addressCodec.encode(address, context);
        assertThat(result, matchesMappingAddress(address));
    }

    /**
     * Tests DN mapping address.
     */
    @Test
    public void dnMappingAddressTest() {
        MappingAddress address = MappingAddresses.dnMappingAddress(DN);
        ObjectNode result = addressCodec.encode(address, context);
        assertThat(result, matchesMappingAddress(address));
    }

    /**
     * Tests IPv4 mapping address.
     */
    @Test
    public void ipv4MappingAddressTest() {
        MappingAddress address = MappingAddresses.ipv4MappingAddress(IPV4_PREFIX);
        ObjectNode result = addressCodec.encode(address, context);
        assertThat(result, matchesMappingAddress(address));
    }

    /**
     * Tests IPv6 mapping address.
     */
    @Test
    public void ipv6MappingAddressTest() {
        MappingAddress address = MappingAddresses.ipv6MappingAddress(IPV6_PREFIX);
        ObjectNode result = addressCodec.encode(address, context);
        assertThat(result, matchesMappingAddress(address));
    }

    /**
     * Tests Ethernet mapping address.
     */
    @Test
    public void ethMappingAddressTest() {
        MappingAddress address = MappingAddresses.ethMappingAddress(MAC);
        ObjectNode result = addressCodec.encode(address, context);
        assertThat(result, matchesMappingAddress(address));
    }

    /**
     * Tests the decoding of mapping address from JSON object.
     *
     * @throws IOException if processing the resource fails
     */
    @Test
    public void testMappingAddressDecode() throws IOException {
        MappingAddress address = getAddress("MappingAddress.json");
        assertThat(address.toString(),
                is("IPV4:" + IPV4_STRING + "/" + PORT_STRING));
    }

    /**
     * Reads in a mapping address from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded mappingAddress
     * @throws IOException if processing the resource fails
     */
    private MappingAddress getAddress(String resourceName) throws IOException {
        InputStream jsonStream = MappingAddressCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        MappingAddress address = addressCodec.decode((ObjectNode) json, context);
        assertThat(address, notNullValue());
        return address;
    }
}
