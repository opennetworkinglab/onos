/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.drivers.lisp.extensions.LispSegmentAddress;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.mapping.codec.MappingAddressJsonMatcher;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for LispSegmentAddressCodec.
 */
public class LispSegmentAddressCodecTest {

    private static final int INSTANCE_ID = 1;
    private static final IpPrefix IPV4_PREFIX = IpPrefix.valueOf("10.1.1.0/24");

    private CodecContext context;
    private JsonCodec<LispSegmentAddress> segmentAddressCodec;
    private LispMappingExtensionCodecRegistrator registrator;

    /**
     * Sets up for each test.
     * Creates a context and fetches the LispSegmentAddress codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new LispMappingExtensionCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new LispMappingExtensionCodecContextAdapter(registrator.codecService);
        segmentAddressCodec = context.codec(LispSegmentAddress.class);
        assertThat("segment address codec should not be null",
                segmentAddressCodec, notNullValue());
    }

    /**
     * Deactivates the codec registrator.
     */
    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests encoding of a LispSegmentAddress object.
     */
    @Test
    public void testLispSegmentAddressEncode() {
        LispSegmentAddress address = new LispSegmentAddress.Builder()
                .withInstanceId(INSTANCE_ID)
                .withAddress(MappingAddresses.ipv4MappingAddress(IPV4_PREFIX))
                .build();
        ObjectNode addressJson = segmentAddressCodec.encode(address, context);
        assertThat("errors in encoding segment address JSON",
                addressJson, LispSegmentAddressJsonMatcher.matchesSegmentAddress(address));
    }

    /**
     * Tests decoding of a LispSegmentAddress JSON object.
     */
    @Test
    public void testLispSegmentAddressDecode() throws IOException {
        LispSegmentAddress address = getLispSegmentAddress("LispSegmentAddress.json");

        assertThat("incorrect instance ID", address.getInstanceId(), is(INSTANCE_ID));
        assertThat("incorrect mapping address", address.getAddress(),
                is(MappingAddresses.ipv4MappingAddress(IPV4_PREFIX)));
    }

    /**
     * Hamcrest matcher for LispSegmentAddress.
     */
    public static final class LispSegmentAddressJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final LispSegmentAddress address;

        /**
         * Default constructor.
         *
         * @param address LispSegmentAddress object
         */
        private LispSegmentAddressJsonMatcher(LispSegmentAddress address) {
            this.address = address;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check instance ID
            int jsonInstanceId = jsonNode.get(LispSegmentAddressCodec.INSTANCE_ID).asInt();
            int instanceId = address.getInstanceId();
            if (jsonInstanceId != instanceId) {
                description.appendText("Instance ID was " + jsonInstanceId);
                return false;
            }

            // check address
            MappingAddressJsonMatcher addressMatcher =
                    MappingAddressJsonMatcher.matchesMappingAddress(address.getAddress());

            return addressMatcher.matches(jsonNode.get(LispSegmentAddressCodec.ADDRESS));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(address.toString());
        }

        /**
         * Factory to allocate a LispSegmentAddress matcher.
         *
         * @param address LispSegmentAddress object we are looking for
         * @return matcher
         */
        public static LispSegmentAddressJsonMatcher matchesSegmentAddress(LispSegmentAddress address) {
            return new LispSegmentAddressJsonMatcher(address);
        }
    }

    /**
     * Reads in a LispSegmentAddress from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded LispSegmentAddress
     * @throws IOException if processing the resource fails
     */
    private LispSegmentAddress getLispSegmentAddress(String resourceName) throws IOException {
        InputStream jsonStream = LispSegmentAddressCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat("JSON string should not be null", json, notNullValue());
        LispSegmentAddress segmentAddress = segmentAddressCodec.decode((ObjectNode) json, context);
        assertThat("decoded address should not be null", segmentAddress, notNullValue());
        return segmentAddress;
    }
}
