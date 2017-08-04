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
import org.onosproject.drivers.lisp.extensions.LispNatAddress;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.mapping.codec.MappingAddressJsonMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for LispNatAddressCodec.
 */
public class LispNatAddressCodecTest {

    private static final short MS_UDP_PORT_NUMBER = (short) 1;
    private static final short ETR_UDP_PORT_NUMBER = (short) 2;
    private static final IpPrefix GLOBAL_ETR_RLOC_ADDRESS = IpPrefix.valueOf("10.1.1.1/24");
    private static final IpPrefix MS_RLOC_ADDRESS = IpPrefix.valueOf("10.1.1.2/24");
    private static final IpPrefix PRIVATE_ETR_RLOC_ADDRESS = IpPrefix.valueOf("10.1.1.3/24");

    private CodecContext context;
    private JsonCodec<LispNatAddress> natAddressCodec;
    private LispMappingExtensionCodecRegistrator registrator;

    /**
     * Sets up for each test.
     * Creates a context and fetches the LispNatAddress codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new LispMappingExtensionCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new LispMappingExtensionCodecContextAdapter(registrator.codecService);
        natAddressCodec = context.codec(LispNatAddress.class);
        assertThat("NAT address codec should not be null",
                natAddressCodec, notNullValue());
    }

    /**
     * Deactivates the codec registrator.
     */
    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests encoding of a LispNatAddress object.
     */
    @Test
    public void testLispNatAddressEncode() {

        List<MappingAddress> rtrRlocs =
                ImmutableList.of(MappingAddresses.ipv4MappingAddress(GLOBAL_ETR_RLOC_ADDRESS),
                        MappingAddresses.ipv4MappingAddress(MS_RLOC_ADDRESS),
                        MappingAddresses.ipv4MappingAddress(PRIVATE_ETR_RLOC_ADDRESS));


        LispNatAddress address = new LispNatAddress.Builder()
                .withMsUdpPortNumber(MS_UDP_PORT_NUMBER)
                .withEtrUdpPortNumber(ETR_UDP_PORT_NUMBER)
                .withGlobalEtrRlocAddress(MappingAddresses.ipv4MappingAddress(GLOBAL_ETR_RLOC_ADDRESS))
                .withMsRlocAddress(MappingAddresses.ipv4MappingAddress(MS_RLOC_ADDRESS))
                .withPrivateEtrRlocAddress(MappingAddresses.ipv4MappingAddress(PRIVATE_ETR_RLOC_ADDRESS))
                .withRtrRlocAddresses(rtrRlocs)
                .build();

        ObjectNode addressJson = natAddressCodec.encode(address, context);
        assertThat("errors in encoding NAT address JSON",
                addressJson, LispNatAddressJsonMatcher.matchesNatAddress(address));
    }

    /**
     * Tests decoding of a LispNatAddress JSON object.
     */
    @Test
    public void testLispNatAddressDecode() throws IOException {
        LispNatAddress address = getLispNatAddress("LispNatAddress.json");

        assertThat("incorrect MS UDP port number",
                address.getMsUdpPortNumber(), is(MS_UDP_PORT_NUMBER));
        assertThat("incorrect ETR UDP port number",
                address.getEtrUdpPortNumber(), is(ETR_UDP_PORT_NUMBER));
        assertThat("incorrect global ETR RLOC address", address.getGlobalEtrRlocAddress(),
                is(MappingAddresses.ipv4MappingAddress(GLOBAL_ETR_RLOC_ADDRESS)));
        assertThat("incorrect MS RLOC address", address.getMsRlocAddress(),
                is(MappingAddresses.ipv4MappingAddress(MS_RLOC_ADDRESS)));
        assertThat("incorrect private ETR RLOC address", address.getPrivateEtrRlocAddress(),
                is(MappingAddresses.ipv4MappingAddress(PRIVATE_ETR_RLOC_ADDRESS)));
    }

    /**
     * Hamcrest matcher for LispNatAddress.
     */
    public static final class LispNatAddressJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final LispNatAddress address;

        /**
         * Default constructor.
         *
         * @param address LispNatAddress object
         */
        private LispNatAddressJsonMatcher(LispNatAddress address) {
            this.address = address;
        }

        private int filteredSize(JsonNode node) {
            return node.size();
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check MS UDP port number
            short jsonMsUdpPortNumber = (short)
                    jsonNode.get(LispNatAddressCodec.MS_UDP_PORT_NUMBER).asInt();
            short msUdpPortNumber = address.getMsUdpPortNumber();
            if (jsonMsUdpPortNumber != msUdpPortNumber) {
                description.appendText("MS UDP port number was " + jsonMsUdpPortNumber);
                return false;
            }

            // check ETR UDP port number
            short jsonEtrUdpPortNumber = (short)
                    jsonNode.get(LispNatAddressCodec.ETR_UDP_PORT_NUMBER).asInt();
            short etrUdpPortNumber = address.getEtrUdpPortNumber();
            if (jsonEtrUdpPortNumber != etrUdpPortNumber) {
                description.appendText("ETR UDP port number was " + jsonEtrUdpPortNumber);
                return false;
            }

            // check RTR RLOC addresses
            final JsonNode jsonRtrRlocs = jsonNode.get(LispNatAddressCodec.RTR_RLOC_ADDRESSES);

            if (address.getRtrRlocAddresses().size() != filteredSize(jsonRtrRlocs)) {
                description.appendText("addresses array size of " +
                        Integer.toString(address.getRtrRlocAddresses().size()));
                return false;
            }

            for (final MappingAddress address : address.getRtrRlocAddresses()) {
                boolean addressFound = false;
                for (int addressIndex = 0; addressIndex < jsonRtrRlocs.size(); addressIndex++) {
                    final String jsonType =
                            jsonRtrRlocs.get(addressIndex).get("type").asText();
                    final String addressType = address.type().name();
                    if (jsonType.equals(addressType)) {
                        addressFound = true;
                    }
                }
                if (!addressFound) {
                    description.appendText("address " + address.toString());
                    return false;
                }
            }

            // check global ETR RLOC address
            MappingAddressJsonMatcher globalEtrRlocMatcher =
                    MappingAddressJsonMatcher.matchesMappingAddress(
                            address.getGlobalEtrRlocAddress());

            // check MS RLOC address
            MappingAddressJsonMatcher msRlocMatcher =
                    MappingAddressJsonMatcher.matchesMappingAddress(
                            address.getMsRlocAddress());

            // check private ETR RLOC address
            MappingAddressJsonMatcher privateEtrRlocMatcher =
                    MappingAddressJsonMatcher.matchesMappingAddress(
                            address.getPrivateEtrRlocAddress());

            return globalEtrRlocMatcher.matches(jsonNode.get(LispNatAddressCodec.GLOBAL_ETR_RLOC_ADDRESS)) ||
                    msRlocMatcher.matches(jsonNode.get(LispNatAddressCodec.MS_RLOC_ADDRESS)) ||
                    privateEtrRlocMatcher.matches(jsonNode.get(LispNatAddressCodec.PRIVATE_ETR_RLOC_ADDRESS));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(address.toString());
        }

        /**
         * Factory to allocate a LispNatAddress matcher.
         *
         * @param address LispNatAddress object we are looking for
         * @return matcher
         */
        public static LispNatAddressJsonMatcher matchesNatAddress(LispNatAddress address) {
            return new LispNatAddressJsonMatcher(address);
        }
    }

    /**
     * Reads in a LispNatAddress from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded LispGcAddress
     * @throws IOException if processing the resource fails
     */
    private LispNatAddress getLispNatAddress(String resourceName) throws IOException {
        InputStream jsonStream = LispNatAddressCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat("JSON string should not be null", json, notNullValue());
        LispNatAddress natAddress = natAddressCodec.decode((ObjectNode) json, context);
        assertThat("decoded address should not be null", natAddress, notNullValue());
        return natAddress;
    }
}
