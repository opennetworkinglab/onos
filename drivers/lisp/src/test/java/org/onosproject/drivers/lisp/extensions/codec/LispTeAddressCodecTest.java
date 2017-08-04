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
import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
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
import org.onosproject.drivers.lisp.extensions.LispTeAddress;
import org.onosproject.mapping.addresses.MappingAddresses;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * LISP traffic engineering address codec.
 */
public class LispTeAddressCodecTest {

    private static final boolean LOOKUP_1 = true;
    private static final boolean RLOC_PROBE_1 = true;
    private static final boolean STRICT_1 = true;
    private static final IpPrefix IPV4_ADDRESS_1 = IpPrefix.valueOf("10.1.1.1/24");

    private static final boolean LOOKUP_2 = false;
    private static final boolean RLOC_PROBE_2 = false;
    private static final boolean STRICT_2 = false;
    private static final IpPrefix IPV4_ADDRESS_2 = IpPrefix.valueOf("10.1.1.2/24");

    private LispTeAddress.TeRecord record1;
    private LispTeAddress.TeRecord record2;

    private CodecContext context;
    private JsonCodec<LispTeAddress> teAddressCodec;
    private LispMappingExtensionCodecRegistrator registrator;

    /**
     * Sets up for each test.
     * Creates a context and fetches the LispTeAddress codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new LispMappingExtensionCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new LispMappingExtensionCodecContextAdapter(registrator.codecService);
        teAddressCodec = context.codec(LispTeAddress.class);
        assertThat("Traffic Engineering address codec should not be null",
                teAddressCodec, notNullValue());

        record1 = new LispTeAddress.TeRecord.Builder()
                .withIsLookup(LOOKUP_1)
                .withIsRlocProbe(RLOC_PROBE_1)
                .withIsStrict(STRICT_1)
                .withRtrRlocAddress(MappingAddresses.ipv4MappingAddress(IPV4_ADDRESS_1))
                .build();

        record2 = new LispTeAddress.TeRecord.Builder()
                .withIsLookup(LOOKUP_2)
                .withIsRlocProbe(RLOC_PROBE_2)
                .withIsStrict(STRICT_2)
                .withRtrRlocAddress(MappingAddresses.ipv4MappingAddress(IPV4_ADDRESS_2))
                .build();
    }

    /**
     * Deactivates the codec registrator.
     */
    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests encoding of a LispTeAddress object.
     */
    @Test
    public void testLispTeAddressEncode() {

        LispTeAddress address = new LispTeAddress.Builder()
                .withTeRecords(ImmutableList.of(record1, record2))
                .build();

        ObjectNode addressJson = teAddressCodec.encode(address, context);
        assertThat("errors in encoding Traffic Engineering address JSON",
                addressJson, LispTeAddressJsonMatcher.matchesTeAddress(address));
    }

    /**
     * Tests decoding of a LispTeAddress JSON object.
     */
    @Test
    public void testLispTeAddressDecode() throws IOException {

        LispTeAddress address = getLispTeAddress("LispTeAddress.json");

        new EqualsTester()
                .addEqualityGroup(address.getTeRecords().get(0), record1)
                .addEqualityGroup(address.getTeRecords().get(1), record2).testEquals();
    }

    /**
     * Hamcrest matcher for LispTeAddress.
     */
    public static final class LispTeAddressJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final LispTeAddress address;

        /**
         * Default constructor.
         *
         * @param address LispTeAddress object
         */
        private LispTeAddressJsonMatcher(LispTeAddress address) {
            this.address = address;
        }

        private int filteredSize(JsonNode node) {
            return node.size();
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check TE records
            final JsonNode jsonTeRecords = jsonNode.get(LispTeAddressCodec.TE_RECORDS);

            if (address.getTeRecords().size() != filteredSize(jsonTeRecords)) {
                description.appendText("TE records array size of " +
                        Integer.toString(address.getTeRecords().size()));
                return false;
            }

            for (int recordIndex = 0; recordIndex < jsonTeRecords.size(); recordIndex++) {
                assertThat(jsonTeRecords.get(recordIndex),
                        LispTeRecordJsonMatcher.matchesTeRecord(address.getTeRecords().get(recordIndex)));
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(address.toString());
        }

        /**
         * Factory to allocate a LispTeAddress matcher.
         *
         * @param address LispTeAddress object we are looking for
         * @return matcher
         */
        public static LispTeAddressJsonMatcher matchesTeAddress(LispTeAddress address) {
            return new LispTeAddressJsonMatcher(address);
        }
    }

    /**
     * Reads in a LispTeAddress from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded LispTeAddress
     * @throws IOException if processing the resource fails
     */
    private LispTeAddress getLispTeAddress(String resourceName) throws IOException {
        InputStream jsonStream = LispTeAddressCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat("JSON string should not be null", json, notNullValue());
        LispTeAddress teAddress = teAddressCodec.decode((ObjectNode) json, context);
        assertThat("decoded address should not be null", teAddress, notNullValue());
        return teAddress;
    }
}