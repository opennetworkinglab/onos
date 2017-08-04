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
import org.onosproject.drivers.lisp.extensions.LispMappingExtensionCodecRegistrator;
import org.onosproject.drivers.lisp.extensions.LispNonceAddress;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.mapping.codec.MappingAddressJsonMatcher;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for LispNonceAddressCodec.
 */
public class LispNonceAddressCodecTest {

    private static final int NONCE = 1;
    private static final IpPrefix ADDRESS = IpPrefix.valueOf("10.1.1.0/24");

    private CodecContext context;
    private JsonCodec<LispNonceAddress> nonceAddressCodec;
    private LispMappingExtensionCodecRegistrator registrator;

    /**
     * Sets up for each test.
     * Creates a context and fetches the LispNonceAddress codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new LispMappingExtensionCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new LispMappingExtensionCodecContextAdapter(registrator.codecService);
        nonceAddressCodec = context.codec(LispNonceAddress.class);
        assertThat("nonce address codec should not be null",
                nonceAddressCodec, notNullValue());
    }

    /**
     * Deactivates the codec registrator.
     */
    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests encoding of a LispNonceAddress object.
     */
    @Test
    public void testLispNonceAddressEncode() {
        LispNonceAddress address = new LispNonceAddress.Builder()
                .withNonce(NONCE)
                .withAddress(MappingAddresses.ipv4MappingAddress(ADDRESS))
                .build();
        ObjectNode addressJson = nonceAddressCodec.encode(address, context);
        assertThat("errors in encoding nonce address JSON",
                addressJson, LispNonceAddressJsonMatcher.matchesNonceAddress(address));
    }

    /**
     * Tests decoding of a LispNonceAddress JSON object.
     */
    @Test
    public void testLispNonceAddressDecode() throws IOException {
        LispNonceAddress address = getLispNonceAddress("LispNonceAddress.json");

        assertThat("incorrect nonce", address.getNonce(), is(NONCE));
        assertThat("incorrect address", address.getAddress(),
                is(MappingAddresses.ipv4MappingAddress(ADDRESS)));
    }

    /**
     * Hamcrest matcher for LispNonceAddress.
     */
    public static final class LispNonceAddressJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final LispNonceAddress address;

        /**
         * Default constructor.
         *
         * @param address LispNonceAddress object
         */
        private LispNonceAddressJsonMatcher(LispNonceAddress address) {
            this.address = address;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check nonce
            int jsonNonce = jsonNode.get(LispNonceAddressCodec.NONCE).asInt();
            int nonce = address.getNonce();
            if (jsonNonce != nonce) {
                description.appendText("Nonce was " + jsonNonce);
                return false;
            }

            // check address
            MappingAddressJsonMatcher addressMatcher =
                    MappingAddressJsonMatcher.matchesMappingAddress(address.getAddress());

            return addressMatcher.matches(jsonNode.get(LispNonceAddressCodec.ADDRESS));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(address.toString());
        }

        /**
         * Factory to allocate a LispNonceAddress matcher.
         *
         * @param address LispNonceAddress object we are looking for
         * @return matcher
         */
        public static LispNonceAddressJsonMatcher matchesNonceAddress(LispNonceAddress address) {
            return new LispNonceAddressJsonMatcher(address);
        }
    }

    /**
     * Reads in a LispNonceAddress from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded LispNonceAddress
     * @throws IOException if processing the resource fails
     */
    private LispNonceAddress getLispNonceAddress(String resourceName) throws IOException {
        InputStream jsonStream = LispNonceAddressCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat("JSON string should not be null", json, notNullValue());
        LispNonceAddress nonceAddress = nonceAddressCodec.decode((ObjectNode) json, context);
        assertThat("decoded address should not be null", nonceAddress, notNullValue());
        return nonceAddress;
    }
}