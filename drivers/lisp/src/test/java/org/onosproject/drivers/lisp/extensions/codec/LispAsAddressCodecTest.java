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
import org.onosproject.drivers.lisp.extensions.LispAsAddress;
import org.onosproject.drivers.lisp.extensions.LispMappingExtensionCodecRegistrator;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.mapping.codec.MappingAddressJsonMatcher;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for LispAsAddressCodec.
 */
public class LispAsAddressCodecTest {

    private static final int AS_NUMBER = 1;
    private static final IpPrefix IPV4_PREFIX = IpPrefix.valueOf("10.1.1.0/24");

    private CodecContext context;
    private JsonCodec<LispAsAddress> asAddressCodec;
    private LispMappingExtensionCodecRegistrator registrator;

    /**
     * Sets up for each test.
     * Creates a context and fetches the LispAsAddress codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new LispMappingExtensionCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new LispMappingExtensionCodecContextAdapter(registrator.codecService);
        asAddressCodec = context.codec(LispAsAddress.class);
        assertThat("AS address codec should not be null", asAddressCodec, notNullValue());
    }

    /**
     * Deactivates the codec registrator.
     */
    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests encoding of a LispAsAddress object.
     */
    @Test
    public void testLispAsAddressEncode() {
        LispAsAddress address = new LispAsAddress.Builder()
                .withAsNumber(AS_NUMBER)
                .withAddress(MappingAddresses.ipv4MappingAddress(IPV4_PREFIX))
                .build();
        ObjectNode addressJson = asAddressCodec.encode(address, context);
        assertThat("errors in encoding AS address JSON",
                addressJson, LispAsAddressJsonMatcher.matchesAsAddress(address));
    }

    /**
     * Tests decoding of a LispAsAddress JSON object.
     */
    @Test
    public void testLispAsAddressDecode() throws IOException {
        LispAsAddress address = getLispAsAddress("LispAsAddress.json");

        assertThat("incorrect AS number", address.getAsNumber(), is(AS_NUMBER));
        assertThat("incorrect mapping address", address.getAddress(),
                    is(MappingAddresses.ipv4MappingAddress(IPV4_PREFIX)));
    }

    /**
     * Hamcrest matcher for LispAsAddress.
     */
    public static final class LispAsAddressJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final LispAsAddress address;

        /**
         * Default constructor.
         *
         * @param address LispAsAddress object
         */
        private LispAsAddressJsonMatcher(LispAsAddress address) {
            this.address = address;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check protocol
            int jsonAsNumber = jsonNode.get(LispAsAddressCodec.AS_NUMBER).asInt();
            int asNumber = address.getAsNumber();
            if (jsonAsNumber != asNumber) {
                description.appendText("AsNumber was " + jsonAsNumber);
                return false;
            }

            // check address
            MappingAddressJsonMatcher addressMatcher =
                    MappingAddressJsonMatcher.matchesMappingAddress(address.getAddress());
            return addressMatcher.matches(jsonNode.get(LispAppDataAddressCodec.ADDRESS));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(address.toString());
        }

        /**
         * Factory to allocate a LispAsAddress matcher.
         *
         * @param address LispAsAddress object we are looking for
         * @return matcher
         */
        public static LispAsAddressJsonMatcher matchesAsAddress(LispAsAddress address) {
            return new LispAsAddressJsonMatcher(address);
        }
    }

    /**
     * Reads in a LispAsAddress from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded LispAsAddress
     * @throws IOException if processing the resource fails
     */
    private LispAsAddress getLispAsAddress(String resourceName) throws IOException {
        InputStream jsonStream = LispAsAddressCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat("JSON string should not be null", json, notNullValue());
        LispAsAddress asAddress = asAddressCodec.decode((ObjectNode) json, context);
        assertThat("decoded address should not be null", asAddress, notNullValue());
        return asAddress;
    }
}
