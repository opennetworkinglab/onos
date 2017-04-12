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
import org.onosproject.drivers.lisp.extensions.LispAppDataAddress;
import org.onosproject.drivers.lisp.extensions.LispMappingExtensionCodecRegistrator;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.mapping.codec.MappingAddressJsonMatcher;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for LispAppDataAddressCodec.
 */
public class LispAppDataAddressCodecTest {

    private static final byte PROTOCOL = (byte) 1;
    private static final int IP_TOS = 1;
    private static final short LOCAL_PORT_LOW = (short) 1;
    private static final short LOCAL_PORT_HIGH = (short) 1;
    private static final short REMOTE_PORT_LOW = (short) 1;
    private static final short REMOTE_PORT_HIGH = (short) 1;
    private static final IpPrefix IPV4_PREFIX = IpPrefix.valueOf("10.1.1.0/24");

    private CodecContext context;
    private JsonCodec<LispAppDataAddress> appDataAddressCodec;
    private LispMappingExtensionCodecRegistrator registrator;

    /**
     * Sets up for each test.
     * Creates a context and fetches the LispAppDataAddress codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new LispMappingExtensionCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new LispMappingExtensionCodecContextAdapter(registrator.codecService);
        appDataAddressCodec = context.codec(LispAppDataAddress.class);
        assertThat(appDataAddressCodec, notNullValue());
    }

    /**
     * Deactivates the codec registrator.
     */
    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests encoding of a LispAppDataAddress object.
     */
    @Test
    public void testLispAppDataAddressEncode() {

        LispAppDataAddress address = new LispAppDataAddress.Builder()
                .withProtocol(PROTOCOL)
                .withIpTos(IP_TOS)
                .withLocalPortLow(LOCAL_PORT_LOW)
                .withLocalPortHigh(LOCAL_PORT_HIGH)
                .withRemotePortLow(REMOTE_PORT_LOW)
                .withRemotePortHigh(REMOTE_PORT_HIGH)
                .withAddress(MappingAddresses.ipv4MappingAddress(IPV4_PREFIX))
                .build();
        ObjectNode addressJson = appDataAddressCodec.encode(address, context);
        assertThat(addressJson, LispAppDataAddressJsonMatcher.matchesAppDataAddress(address));
    }

    /**
     * Tests decoding of a LispAppDataAddress JSON object.
     */
    @Test
    public void testLispAppDataAddressDecode() throws IOException {
        LispAppDataAddress appDataAddress =
                getLispAppDataAddress("LispAppDataAddress.json");

        assertThat("incorrect protocol value",
                appDataAddress.getProtocol(), is(PROTOCOL));
        assertThat("incorrect IP ToS value",
                appDataAddress.getIpTos(), is(IP_TOS));
        assertThat("incorrect local port low value",
                appDataAddress.getLocalPortLow(), is(LOCAL_PORT_LOW));
        assertThat("incorrect local port high value",
                appDataAddress.getLocalPortHigh(), is(LOCAL_PORT_HIGH));
        assertThat("incorrect remote port low value",
                appDataAddress.getRemotePortLow(), is(REMOTE_PORT_LOW));
        assertThat("incorrect remote port high value",
                appDataAddress.getRemotePortHigh(), is(REMOTE_PORT_HIGH));
        assertThat("incorrect mapping address",
                appDataAddress.getAddress(),
                    is(MappingAddresses.ipv4MappingAddress(IPV4_PREFIX)));
    }

    /**
     * Hamcrest matcher for LispAppDataAddress.
     */
    public static final class LispAppDataAddressJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final LispAppDataAddress address;

        /**
         * Default constructor.
         *
         * @param address LispAppDataAddress object
         */
        private LispAppDataAddressJsonMatcher(LispAppDataAddress address) {
            this.address = address;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check protocol
            byte jsonProtocol = (byte) jsonNode.get(LispAppDataAddressCodec.PROTOCOL).asInt();
            byte protocol = address.getProtocol();
            if (jsonProtocol != protocol) {
                description.appendText("protocol was " + jsonProtocol);
                return false;
            }

            // check IP type of service
            int jsonIpTos = jsonNode.get(LispAppDataAddressCodec.IP_TOS).asInt();
            int ipTos = address.getIpTos();
            if (jsonIpTos != ipTos) {
                description.appendText("IP ToS was " + jsonIpTos);
                return false;
            }

            // check local port low
            short jsonLocalPortLow = (short)
                    jsonNode.get(LispAppDataAddressCodec.LOCAL_PORT_LOW).asInt();
            short localPortLow = address.getLocalPortLow();
            if (jsonLocalPortLow != localPortLow) {
                description.appendText("Local port low was " + jsonLocalPortLow);
                return false;
            }

            // check local port high
            short jsonLocalPortHigh = (short)
                    jsonNode.get(LispAppDataAddressCodec.LOCAL_PORT_HIGH).asInt();
            short localPortHigh = address.getLocalPortHigh();
            if (jsonLocalPortHigh != localPortHigh) {
                description.appendText("Local port high was " + jsonLocalPortHigh);
                return false;
            }

            // check remote port low
            short jsonRemotePortLow = (short)
                    jsonNode.get(LispAppDataAddressCodec.REMOTE_PORT_LOW).asInt();
            short remotePortLow = address.getRemotePortLow();
            if (jsonRemotePortLow != remotePortLow) {
                description.appendText("Remote port low was " + jsonRemotePortLow);
                return false;
            }

            // check remote port high
            short jsonRemotePortHigh = (short)
                    jsonNode.get(LispAppDataAddressCodec.REMOTE_PORT_HIGH).asInt();
            short remotePortHigh = address.getRemotePortHigh();
            if (jsonRemotePortHigh != remotePortHigh) {
                description.appendText("Remote port high was " + jsonRemotePortHigh);
                return false;
            }

            // check address
            MappingAddressJsonMatcher addressMatcher =
                    MappingAddressJsonMatcher.matchesMappingAddress(address.getAddress());
            if (!addressMatcher.matches(jsonNode.get(LispAppDataAddressCodec.ADDRESS))) {
                return false;
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(address.toString());
        }

        /**
         * Factory to allocate a LispAppDataAddress matcher.
         *
         * @param address LispAppDataAddress object we are looking for
         * @return matcher
         */
        public static LispAppDataAddressJsonMatcher matchesAppDataAddress(
                LispAppDataAddress address) {
            return new LispAppDataAddressJsonMatcher(address);
        }
    }

    /**
     * Reads in a LispAppDataAddress from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded LispAppDataAddress
     * @throws IOException if processing the resource fails
     */
    private LispAppDataAddress getLispAppDataAddress(String resourceName) throws IOException {
        InputStream jsonStream = LispAppDataAddressCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        LispAppDataAddress appDataAddress = appDataAddressCodec.decode((ObjectNode) json, context);
        assertThat(appDataAddress, notNullValue());
        return appDataAddress;
    }
}
