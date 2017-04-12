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
import org.onosproject.drivers.lisp.extensions.LispSrcDstAddress;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.mapping.codec.MappingAddressJsonMatcher;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for LispSrcDstAddressCodec.
 */
public class LispSrcDstAddressCodecTest {

    private static final byte SRC_MASK_LENGTH = (byte) 1;
    private static final byte DST_MASK_LENGTH = (byte) 2;
    private static final IpPrefix IPV4_SRC_PREFIX = IpPrefix.valueOf("10.1.1.1/24");
    private static final IpPrefix IPV4_DST_PREFIX = IpPrefix.valueOf("10.1.1.2/24");

    private CodecContext context;
    private JsonCodec<LispSrcDstAddress> srcDstAddressCodec;
    private LispMappingExtensionCodecRegistrator registrator;

    /**
     * Sets up for each test.
     * Creates a context and fetches the LispSrcDstAddress codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new LispMappingExtensionCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new LispMappingExtensionCodecContextAdapter(registrator.codecService);
        srcDstAddressCodec = context.codec(LispSrcDstAddress.class);
        assertThat("Source and Destination address codec should not be null",
                srcDstAddressCodec, notNullValue());
    }

    /**
     * Deactivates the codec registrator.
     */
    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests encoding of a LispSrcDstAddress object.
     */
    @Test
    public void testLispSrcDstAddressEncode() {
        LispSrcDstAddress address = new LispSrcDstAddress.Builder()
                .withSrcMaskLength(SRC_MASK_LENGTH)
                .withDstMaskLength(DST_MASK_LENGTH)
                .withSrcPrefix(MappingAddresses.ipv4MappingAddress(IPV4_SRC_PREFIX))
                .withDstPrefix(MappingAddresses.ipv4MappingAddress(IPV4_DST_PREFIX))
                .build();
        ObjectNode addressJson = srcDstAddressCodec.encode(address, context);
        assertThat("errors in encoding Source and Destination address JSON",
                addressJson, LispSrcDstAddressJsonMatcher.matchesSrcDstAddress(address));
    }

    /**
     * Tests decoding of a LispSrcDstAddress JSON object.
     */
    @Test
    public void testLispSrcDstAddressDecode() throws IOException {
        LispSrcDstAddress address = getLispSrcDstAddress("LispSrcDstAddress.json");

        assertThat("incorrect srcMaskLength", address.getSrcMaskLength(), is(SRC_MASK_LENGTH));
        assertThat("incorrect dstMaskLength", address.getDstMaskLength(), is(DST_MASK_LENGTH));
        assertThat("incorrect srcPrefix", address.getSrcPrefix(),
                is(MappingAddresses.ipv4MappingAddress(IPV4_SRC_PREFIX)));
        assertThat("incorrect dstPrefix", address.getDstPrefix(),
                is(MappingAddresses.ipv4MappingAddress(IPV4_DST_PREFIX)));
    }

    /**
     * Hamcrest matcher for LispSrcDstAddress.
     */
    public static final class LispSrcDstAddressJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final LispSrcDstAddress address;

        /**
         * Default constructor.
         *
         * @param address LispSrcDstAddress object
         */
        private LispSrcDstAddressJsonMatcher(LispSrcDstAddress address) {
            this.address = address;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check src mask length
            byte jsonSrcMaskLength = (byte) jsonNode.get(LispSrcDstAddressCodec.SRC_MASK_LENGTH).asInt();
            byte srcMaskLength = address.getSrcMaskLength();
            if (jsonSrcMaskLength != srcMaskLength) {
                description.appendText("Source mask length was " + jsonSrcMaskLength);
                return false;
            }

            // check dst mask length
            byte jsonDstMaskLength = (byte) jsonNode.get(LispSrcDstAddressCodec.DST_MASK_LENGTH).asInt();
            byte dstMaskLength = address.getDstMaskLength();
            if (jsonDstMaskLength != dstMaskLength) {
                description.appendText("Destination mask length was " + jsonDstMaskLength);
                return false;
            }

            // src prefix
            MappingAddressJsonMatcher srcPrefixMatcher =
                    MappingAddressJsonMatcher.matchesMappingAddress(address.getSrcPrefix());

            // dst prefix
            MappingAddressJsonMatcher dstPrefixMatcher =
                    MappingAddressJsonMatcher.matchesMappingAddress(address.getDstPrefix());

            return srcPrefixMatcher.matches(jsonNode.get(LispSrcDstAddressCodec.SRC_PREFIX)) ||
                    dstPrefixMatcher.matches(jsonNode.get(LispSrcDstAddressCodec.DST_PREFIX));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(address.toString());
        }

        /**
         * Factory to allocate a LispSrcDstAddress matcher.
         *
         * @param address LispSrcDstAddress object we are looking for
         * @return matcher
         */
        public static LispSrcDstAddressJsonMatcher matchesSrcDstAddress(LispSrcDstAddress address) {
            return new LispSrcDstAddressJsonMatcher(address);
        }
    }

    /**
     * Reads in a LispSrcDstAddress from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded LispSrcDstAddress
     * @throws IOException if processing the resource fails
     */
    private LispSrcDstAddress getLispSrcDstAddress(String resourceName) throws IOException {
        InputStream jsonStream = LispSrcDstAddressCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat("JSON string should not be null", json, notNullValue());
        LispSrcDstAddress srcDstAddress = srcDstAddressCodec.decode((ObjectNode) json, context);
        assertThat("decoded address should not be null", srcDstAddress, notNullValue());
        return srcDstAddress;
    }
}
