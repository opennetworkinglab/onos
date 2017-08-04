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
package org.onosproject.drivers.lisp.extensions.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpPrefix;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.drivers.lisp.extensions.LispListAddress;
import org.onosproject.drivers.lisp.extensions.LispMappingExtensionCodecRegistrator;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.mapping.codec.MappingAddressJsonMatcher;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for LispListAddressCodec.
 */
public class LispListAddressCodecTest {

    private static final IpPrefix IPV4_PREFIX = IpPrefix.valueOf("10.1.1.0/24");
    private static final IpPrefix IPV6_PREFIX = IpPrefix.valueOf("fe80::/64");

    private CodecContext context;
    private JsonCodec<LispListAddress> listAddressCodec;
    private LispMappingExtensionCodecRegistrator registrator;

    /**
     * Sets up for each test.
     * Creates a context and fetches the LispListAddress codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new LispMappingExtensionCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new LispMappingExtensionCodecContextAdapter(registrator.codecService);
        listAddressCodec = context.codec(LispListAddress.class);
        assertThat("List address codec should not be null",
                listAddressCodec, notNullValue());
    }

    /**
     * Deactivates the codec registrator.
     */
    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests encoding of a LispListAddress object.
     */
    @Test
    public void testLispListAddressEncode() {
        LispListAddress address = new LispListAddress.Builder()
                .withIpv4(MappingAddresses.ipv4MappingAddress(IPV4_PREFIX))
                .withIpv6(MappingAddresses.ipv6MappingAddress(IPV6_PREFIX))
                .build();
        ObjectNode addressJson = listAddressCodec.encode(address, context);
        assertThat("errors in encoding List address JSON",
                addressJson, LispListAddressJsonMatcher.matchesListAddress(address));
    }

    /**
     * Tests decoding of a LispListAddress JSON object.
     */
    @Test
    public void testLispListAddressDecode() throws IOException {
        LispListAddress address = getLispListAddress("LispListAddress.json");

        assertThat("incorrect IPv4 address", address.getIpv4(),
                is(MappingAddresses.ipv4MappingAddress(IPV4_PREFIX)));
        assertThat("incorrect IPv6 address", address.getIpv6(),
                is(MappingAddresses.ipv6MappingAddress(IPV6_PREFIX)));
    }

    /**
     * Hamcrest matcher for LispListAddress.
     */
    public static final class LispListAddressJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final LispListAddress address;

        /**
         * Default constructor.
         *
         * @param address LispListAddress object
         */
        private LispListAddressJsonMatcher(LispListAddress address) {
            this.address = address;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check ipv4
            MappingAddressJsonMatcher ipv4Matcher =
                    MappingAddressJsonMatcher.matchesMappingAddress(address.getIpv4());

            // check ipv6
            MappingAddressJsonMatcher ipv6Matcher =
                    MappingAddressJsonMatcher.matchesMappingAddress(address.getIpv6());

            return ipv4Matcher.matches(jsonNode.get(LispListAddressCodec.IPV4)) ||
                    ipv6Matcher.matches(jsonNode.get(LispListAddressCodec.IPV6));

        }

        @Override
        public void describeTo(Description description) {
            description.appendText(address.toString());
        }

        /**
         * Factory to allocate a LispListAddress matcher.
         *
         * @param address LispListAddress object we are looking for
         * @return matcher
         */
        public static LispListAddressJsonMatcher matchesListAddress(LispListAddress address) {
            return new LispListAddressJsonMatcher(address);
        }
    }

    /**
     * Reads in a LispListAddress from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded LispListAddress
     * @throws IOException if processing the resource fails
     */
    private LispListAddress getLispListAddress(String resourceName) throws IOException {
        InputStream jsonStream = LispListAddressCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat("JSON string should not be null", json, notNullValue());
        LispListAddress listAddress = listAddressCodec.decode((ObjectNode) json, context);
        assertThat("decoded address should not be null", listAddress, notNullValue());
        return listAddress;
    }
}
