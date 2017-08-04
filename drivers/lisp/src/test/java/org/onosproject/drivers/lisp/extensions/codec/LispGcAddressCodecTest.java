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
import org.onosproject.drivers.lisp.extensions.LispGcAddress;
import org.onosproject.drivers.lisp.extensions.LispMappingExtensionCodecRegistrator;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.mapping.codec.MappingAddressJsonMatcher;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for LispGcAddressCodec.
 */
public class LispGcAddressCodecTest {

    private static final boolean NORTH = true;
    private static final short LATITUDE_DEGREE = (short) 1;
    private static final byte LATITUDE_MINUTE = (byte) 1;
    private static final byte LATITUDE_SECOND = (byte) 1;

    private static final boolean EAST = false;
    private static final short LONGITUDE_DEGREE = (short) 2;
    private static final byte LONGITUDE_MINUTE = (byte) 2;
    private static final byte LONGITUDE_SECOND = (byte) 2;

    private static final int ALTITUDE = 3;
    private static final IpPrefix IPV4_PREFIX = IpPrefix.valueOf("10.1.1.0/24");

    private CodecContext context;
    private JsonCodec<LispGcAddress> gcAddressCodec;
    private LispMappingExtensionCodecRegistrator registrator;

    /**
     * Sets up for each test.
     * Creates a context and fetches the LispGcAddress codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new LispMappingExtensionCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new LispMappingExtensionCodecContextAdapter(registrator.codecService);
        gcAddressCodec = context.codec(LispGcAddress.class);
        assertThat("Geo Coordinate address codec should not be null",
                gcAddressCodec, notNullValue());
    }

    /**
     * Deactivates the codec registrator.
     */
    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests encoding of a LispGcAddress object.
     */
    @Test
    public void testLispGcAddressEncode() {
        LispGcAddress address = new LispGcAddress.Builder()
                .withIsNorth(NORTH)
                .withLatitudeDegree(LATITUDE_DEGREE)
                .withLatitudeMinute(LATITUDE_MINUTE)
                .withLatitudeSecond(LATITUDE_SECOND)
                .withIsEast(EAST)
                .withLongitudeDegree(LONGITUDE_DEGREE)
                .withLongitudeMinute(LONGITUDE_MINUTE)
                .withLongitudeSecond(LONGITUDE_SECOND)
                .withAltitude(ALTITUDE)
                .withAddress(MappingAddresses.ipv4MappingAddress(IPV4_PREFIX))
                .build();
        ObjectNode addressJson = gcAddressCodec.encode(address, context);
        assertThat("errors in encoding Geo Coordinate address JSON",
                addressJson, LispGcAddressJsonMatcher.matchesGcAddress(address));
    }

    /**
     * Tests decoding of a LispGcAddress JSON object.
     */
    @Test
    public void testLispGcAddressDecode() throws IOException {
        LispGcAddress address = getLispGcAddress("LispGcAddress.json");

        assertThat("incorrect north", address.isNorth(), is(NORTH));
        assertThat("incorrect latitude degree",
                address.getLatitudeDegree(), is(LATITUDE_DEGREE));
        assertThat("incorrect latitude minute",
                address.getLatitudeMinute(), is(LATITUDE_MINUTE));
        assertThat("incorrect latitude second",
                address.getLatitudeSecond(), is(LATITUDE_SECOND));
        assertThat("incorrect east", address.isEast(), is(EAST));
        assertThat("incorrect longitude degree",
                address.getLongitudeDegree(), is(LONGITUDE_DEGREE));
        assertThat("incorrect longitude minute",
                address.getLongitudeMinute(), is(LONGITUDE_MINUTE));
        assertThat("incorrect longitude second",
                address.getLongitudeSecond(), is(LONGITUDE_SECOND));
        assertThat("incorrect altitude", address.getAltitude(), is(ALTITUDE));
        assertThat("incorrect mapping address", address.getAddress(),
                is(MappingAddresses.ipv4MappingAddress(IPV4_PREFIX)));
    }

    /**
     * Hamcrest matcher for LispGcAddress.
     */
    public static final class LispGcAddressJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final LispGcAddress address;

        /**
         * Default constructor.
         *
         * @param address LispGcAddress object
         */
        private LispGcAddressJsonMatcher(LispGcAddress address) {
            this.address = address;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check north
            boolean jsonNorth = jsonNode.get(LispGcAddressCodec.NORTH).asBoolean();
            boolean north = address.isNorth();
            if (jsonNorth != north) {
                description.appendText("North was " + jsonNorth);
                return false;
            }

            // check latitude degree
            short jsonLatitudeDegree = (short) jsonNode.get(LispGcAddressCodec.LATITUDE_DEGREE).asInt();
            short latitudeDegree = address.getLatitudeDegree();
            if (jsonLatitudeDegree != latitudeDegree) {
                description.appendText("Latitude degree was " + jsonLatitudeDegree);
                return false;
            }

            // check latitude minute
            byte jsonLatitudeMinute = (byte) jsonNode.get(LispGcAddressCodec.LATITUDE_MINUTE).asInt();
            byte latitudeMinute = address.getLatitudeMinute();
            if (jsonLatitudeMinute != latitudeMinute) {
                description.appendText("Latitude minute was " + jsonLatitudeMinute);
                return false;
            }

            // check latitude second
            byte jsonLatitudeSecond = (byte) jsonNode.get(LispGcAddressCodec.LATITUDE_SECOND).asInt();
            byte latitudeSecond = address.getLatitudeSecond();
            if (jsonLatitudeSecond != latitudeSecond) {
                description.appendText("Latitude second was " + jsonLatitudeSecond);
                return false;
            }

            // check east
            boolean jsonEast = jsonNode.get(LispGcAddressCodec.EAST).asBoolean();
            boolean east = address.isEast();
            if (jsonEast != east) {
                description.appendText("East was " + jsonEast);
                return false;
            }

            // check longitude degree
            short jsonLongitudeDegree = (short) jsonNode.get(LispGcAddressCodec.LONGITUDE_DEGREE).asInt();
            short longitudeDegree = address.getLongitudeDegree();
            if (jsonLongitudeDegree != longitudeDegree) {
                description.appendText("Longitude degree was " + jsonLongitudeDegree);
                return false;
            }

            // check longitude minute
            byte jsonLongitudeMinute = (byte) jsonNode.get(LispGcAddressCodec.LONGITUDE_MINUTE).asInt();
            byte longitudeMinute = address.getLongitudeMinute();
            if (jsonLongitudeMinute != longitudeMinute) {
                description.appendText("Longitude minute was " + jsonLongitudeMinute);
                return false;
            }

            // check longitude second
            byte jsonLongitudeSecond = (byte) jsonNode.get(LispGcAddressCodec.LONGITUDE_SECOND).asInt();
            byte longitudeSecond = address.getLongitudeSecond();
            if (jsonLongitudeSecond != longitudeSecond) {
                description.appendText("Longitude second was " + jsonLongitudeSecond);
                return false;
            }

            // check altitude
            int jsonAltitude = jsonNode.get(LispGcAddressCodec.ALTITUDE).asInt();
            int altitude = address.getAltitude();
            if (jsonAltitude != altitude) {
                description.appendText("Altitude was " + jsonAltitude);
                return false;
            }

            // check address
            MappingAddressJsonMatcher addressMatcher =
                    MappingAddressJsonMatcher.matchesMappingAddress(address.getAddress());

            return addressMatcher.matches(jsonNode.get(LispGcAddressCodec.ADDRESS));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(address.toString());
        }

        /**
         * Factory to allocate a LispGcAddress matcher.
         *
         * @param address LispGcAddress object we are looking for
         * @return matcher
         */
        public static LispGcAddressJsonMatcher matchesGcAddress(LispGcAddress address) {
            return new LispGcAddressJsonMatcher(address);
        }
    }

    /**
     * Reads in a LispGcAddress from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded LispGcAddress
     * @throws IOException if processing the resource fails
     */
    private LispGcAddress getLispGcAddress(String resourceName) throws IOException {
        InputStream jsonStream = LispGcAddressCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat("JSON string should not be null", json, notNullValue());
        LispGcAddress gcAddress = gcAddressCodec.decode((ObjectNode) json, context);
        assertThat("Decoded address should not be null", gcAddress, notNullValue());
        return gcAddress;
    }
}
