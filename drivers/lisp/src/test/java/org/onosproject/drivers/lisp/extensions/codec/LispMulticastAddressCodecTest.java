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
import org.onosproject.drivers.lisp.extensions.LispMulticastAddress;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.mapping.codec.MappingAddressJsonMatcher;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for LispMulticastAddressCodec.
 */
public class LispMulticastAddressCodecTest {

    private static final int INSTANCE_ID = 1;
    private static final byte SRC_MASK_LENGTH = 2;
    private static final byte GRP_MASK_LENGTH = 3;

    private static final IpPrefix SRC_ADDRESS_PREFIX = IpPrefix.valueOf("10.1.1.0/24");
    private static final IpPrefix GRP_ADDRESS_PREFIX = IpPrefix.valueOf("10.1.1.0/24");

    private CodecContext context;
    private JsonCodec<LispMulticastAddress> multicastAddressCodec;
    private LispMappingExtensionCodecRegistrator registrator;

    /**
     * Sets up for each test.
     * Creates a context and fetches the LispMulticastAddress codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new LispMappingExtensionCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new LispMappingExtensionCodecContextAdapter(registrator.codecService);
        multicastAddressCodec = context.codec(LispMulticastAddress.class);
        assertThat("Multicast address codec should not be null",
                multicastAddressCodec, notNullValue());
    }

    /**
     * Deactivates the codec registrator.
     */
    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests encoding of a LispMulticastAddress object.
     */
    @Test
    public void testLispMulticastAddressEncode() {
        LispMulticastAddress address = new LispMulticastAddress.Builder()
                .withInstanceId(INSTANCE_ID)
                .withSrcMaskLength(SRC_MASK_LENGTH)
                .withGrpMaskLength(GRP_MASK_LENGTH)
                .withSrcAddress(MappingAddresses.ipv4MappingAddress(SRC_ADDRESS_PREFIX))
                .withGrpAddress(MappingAddresses.ipv4MappingAddress(GRP_ADDRESS_PREFIX))
                .build();
        ObjectNode addressJson = multicastAddressCodec.encode(address, context);
        assertThat("errors in encoding Multicast address JSON",
                addressJson, LispMulticastAddressJsonMatcher.matchesMulticastAddress(address));
    }

    /**
     * Tests decoding of a LispMulticastAddress JSON object.
     */
    @Test
    public void testLispMulticastAddressDecode() throws IOException {
        LispMulticastAddress address =
                getLispMulticastAddress("LispMulticastAddress.json");

        assertThat("incorrect instance id",
                address.getInstanceId(), is(INSTANCE_ID));
        assertThat("incorrect srcMaskLength",
                address.getSrcMaskLength(), is(SRC_MASK_LENGTH));
        assertThat("incorrect srcAddress", address.getSrcAddress(),
                is(MappingAddresses.ipv4MappingAddress(SRC_ADDRESS_PREFIX)));
        assertThat("incorrect grpMaskLength",
                address.getGrpMaskLength(), is(GRP_MASK_LENGTH));
        assertThat("incorrect grpAddress", address.getGrpAddress(),
                is(MappingAddresses.ipv4MappingAddress(GRP_ADDRESS_PREFIX)));
    }

    /**
     * Hamcrest matcher for LispMulticastAddress.
     */
    public static final class LispMulticastAddressJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final LispMulticastAddress address;

        /**
         * Default constructor.
         *
         * @param address LispMulticastAddres object
         */
        private LispMulticastAddressJsonMatcher(LispMulticastAddress address) {
            this.address = address;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check instance id
            int jsonInstanceId = jsonNode.get(LispMulticastAddressCodec.INSTANCE_ID).asInt();
            int instanceId = address.getInstanceId();
            if (jsonInstanceId != instanceId) {
                description.appendText("Instance id was " + jsonInstanceId);
                return false;
            }

            // check source mask length
            byte jsonSrcMaskLength = (byte) jsonNode.get(
                    LispMulticastAddressCodec.SRC_MASK_LENGTH).asInt();
            byte srcMaskLength = address.getSrcMaskLength();
            if (jsonSrcMaskLength != srcMaskLength) {
                description.appendText("SrcMaskLength was " + jsonSrcMaskLength);
                return false;
            }

            // check group mask length
            byte jsonGrpMaskLength = (byte) jsonNode.get(
                    LispMulticastAddressCodec.GRP_MASK_LENGTH).asInt();
            byte grpMaskLength = address.getGrpMaskLength();
            if (jsonGrpMaskLength != grpMaskLength) {
                description.appendText("GrpMaskLength was " + jsonGrpMaskLength);
                return false;
            }

            // check source address
            MappingAddressJsonMatcher srcAddressMatcher =
                    MappingAddressJsonMatcher.matchesMappingAddress(address.getSrcAddress());

            // check group address
            MappingAddressJsonMatcher grpAddressMatcher =
                    MappingAddressJsonMatcher.matchesMappingAddress(address.getGrpAddress());

            return srcAddressMatcher.matches(jsonNode.get(LispMulticastAddressCodec.SRC_ADDRESS)) ||
                    grpAddressMatcher.matches(jsonNode.get(LispMulticastAddressCodec.GRP_ADDRESS));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(address.toString());
        }

        /**
         * Factory to allocate a LispMulticastAddress matcher.
         *
         * @param address LispMulticastAddress object we are looking for
         * @return matcher
         */
        public static LispMulticastAddressJsonMatcher matchesMulticastAddress(
                LispMulticastAddress address) {
            return new LispMulticastAddressJsonMatcher(address);
        }
    }

    /**
     * Reads in a LispMulticastAddress from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded LispMulticastAddress
     * @throws IOException if processing the resource fails
     */
    private LispMulticastAddress getLispMulticastAddress(String resourceName) throws IOException {
        InputStream jsonStream = LispMulticastAddressCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat("JSON string should not be null", json, notNullValue());
        LispMulticastAddress multicastAddress = multicastAddressCodec.decode((ObjectNode) json, context);
        assertThat("decoded address should not be null", multicastAddress, notNullValue());
        return multicastAddress;
    }
}
