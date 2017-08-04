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
package org.onosproject.drivers.lisp.extensions;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.drivers.lisp.extensions.codec.LispAppDataAddressCodecTest;
import org.onosproject.drivers.lisp.extensions.codec.LispAsAddressCodecTest;
import org.onosproject.drivers.lisp.extensions.codec.LispGcAddressCodecTest;
import org.onosproject.drivers.lisp.extensions.codec.LispListAddressCodecTest;
import org.onosproject.drivers.lisp.extensions.codec.LispMappingExtensionCodecContextAdapter;
import org.onosproject.drivers.lisp.extensions.codec.LispMulticastAddressCodecTest;
import org.onosproject.drivers.lisp.extensions.codec.LispNatAddressCodecTest;
import org.onosproject.drivers.lisp.extensions.codec.LispNonceAddressCodecTest;
import org.onosproject.drivers.lisp.extensions.codec.LispSegmentAddressCodecTest;
import org.onosproject.drivers.lisp.extensions.codec.LispSrcDstAddressCodecTest;
import org.onosproject.drivers.lisp.extensions.codec.LispTeAddressCodecTest;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.types.LispIpv6Address;
import org.onosproject.lisp.msg.types.lcaf.LispAppDataLcafAddress;
import org.onosproject.lisp.msg.types.lcaf.LispAsLcafAddress;
import org.onosproject.lisp.msg.types.lcaf.LispGeoCoordinateLcafAddress;
import org.onosproject.lisp.msg.types.lcaf.LispLcafAddress;
import org.onosproject.lisp.msg.types.lcaf.LispListLcafAddress;
import org.onosproject.lisp.msg.types.lcaf.LispMulticastLcafAddress;
import org.onosproject.lisp.msg.types.lcaf.LispNatLcafAddress;
import org.onosproject.lisp.msg.types.lcaf.LispNonceLcafAddress;
import org.onosproject.lisp.msg.types.lcaf.LispSegmentLcafAddress;
import org.onosproject.lisp.msg.types.lcaf.LispSourceDestLcafAddress;
import org.onosproject.lisp.msg.types.lcaf.LispTeLcafAddress;
import org.onosproject.lisp.msg.types.lcaf.LispTeRecord;
import org.onosproject.mapping.addresses.ExtensionMappingAddress;
import org.onosproject.mapping.addresses.ExtensionMappingAddressType;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.onosproject.drivers.lisp.extensions.LispExtensionMappingAddressInterpreter.*;
import static org.onosproject.mapping.addresses.ExtensionMappingAddressType.ExtensionMappingAddressTypes.*;
/**
 * Unit tests for LispExtensionMappingAddressInterpreter.
 */
public class LispExtensionMappingAddressInterpreterTest {

    private static final String IPV4_STRING = "1.2.3.4";
    private static final String IPV6_STRING = "1111:2222:3333:4444:5555:6666:7777:8886";
    private static final IpPrefix IPV4_PREFIX = IpPrefix.valueOf(IPV4_STRING + "/32");
    private static final IpPrefix IPV6_PREFIX = IpPrefix.valueOf(IPV6_STRING + "/128");
    private static final IpAddress IPV4_ADDRESS = IpAddress.valueOf(IPV4_STRING);
    private static final IpAddress IPV6_ADDRESS = IpAddress.valueOf(IPV6_STRING);

    private static final int UNIQUE_INT = 1;
    private static final short UNIQUE_SHORT = (short) 1;
    private static final byte UNIQUE_BYTE = (byte) 1;
    private static final boolean UNIQUE_BOOLEAN = true;

    private LispExtensionMappingAddressInterpreter interpreter;

    private CodecContext context;
    private LispMappingExtensionCodecRegistrator registrator;

    private ExtensionMappingAddress listExtAddress;
    private ExtensionMappingAddress segmentExtAddress;
    private ExtensionMappingAddress asExtAddress;
    private ExtensionMappingAddress appDataExtAddress;
    private ExtensionMappingAddress gcExtAddress;
    private ExtensionMappingAddress natExtAddress;
    private ExtensionMappingAddress nonceExtAddress;
    private ExtensionMappingAddress multicastExtAddress;
    private ExtensionMappingAddress teExtAddress;
    private ExtensionMappingAddress srcDstExtAddress;

    private LispLcafAddress listLcafAddress;
    private LispLcafAddress segmentLcafAddress;
    private LispLcafAddress asLcafAddress;
    private LispLcafAddress appDataLcafAddress;
    private LispLcafAddress gcLcafAddress;
    private LispLcafAddress natLcafAddress;
    private LispLcafAddress nonceLcafAddress;
    private LispLcafAddress multicastLcafAddress;
    private LispLcafAddress teLcafAddress;
    private LispLcafAddress srcDstLcafAddress;

    @Before
    public void setUp() {
        interpreter = new LispExtensionMappingAddressInterpreter();
        initExtAddresses();
        initLcafAddresses();

        CodecManager manager = new CodecManager();
        registrator = new LispMappingExtensionCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new LispMappingExtensionCodecContextAdapter(registrator.codecService);
    }

    /**
     * Initializes all extension mapping addresses.
     */
    private void initExtAddresses() {
        listExtAddress = getExtMappingAddress(LIST_ADDRESS.type());
        segmentExtAddress = getExtMappingAddress(SEGMENT_ADDRESS.type());
        asExtAddress = getExtMappingAddress(AS_ADDRESS.type());
        appDataExtAddress = getExtMappingAddress(APPLICATION_DATA_ADDRESS.type());
        gcExtAddress = getExtMappingAddress(GEO_COORDINATE_ADDRESS.type());
        natExtAddress = getExtMappingAddress(NAT_ADDRESS.type());
        nonceExtAddress = getExtMappingAddress(NONCE_ADDRESS.type());
        multicastExtAddress = getExtMappingAddress(MULTICAST_ADDRESS.type());
        teExtAddress = getExtMappingAddress(TRAFFIC_ENGINEERING_ADDRESS.type());
        srcDstExtAddress = getExtMappingAddress(SOURCE_DEST_ADDRESS.type());
    }

    /**
     * Initializes all LCAF addresses.
     */
    private void initLcafAddresses() {
        listLcafAddress = getLcafMappingAddress(LIST_ADDRESS.type());
        segmentLcafAddress = getLcafMappingAddress(SEGMENT_ADDRESS.type());
        asLcafAddress = getLcafMappingAddress(AS_ADDRESS.type());
        appDataLcafAddress = getLcafMappingAddress(APPLICATION_DATA_ADDRESS.type());
        gcLcafAddress = getLcafMappingAddress(GEO_COORDINATE_ADDRESS.type());
        natLcafAddress = getLcafMappingAddress(NAT_ADDRESS.type());
        nonceLcafAddress = getLcafMappingAddress(NONCE_ADDRESS.type());
        multicastLcafAddress = getLcafMappingAddress(MULTICAST_ADDRESS.type());
        teLcafAddress = getLcafMappingAddress(TRAFFIC_ENGINEERING_ADDRESS.type());
        srcDstLcafAddress = getLcafMappingAddress(SOURCE_DEST_ADDRESS.type());
    }

    /**
     * Tests supportability of a certain LISP extension mapping address.
     */
    @Test
    public void testSupported() {
        assertTrue("List extension address should be supported",
                interpreter.supported(listExtAddress.type()));
        assertTrue("Segment extension address should be supported",
                interpreter.supported(segmentExtAddress.type()));
        assertTrue("AS extension address should be supported",
                interpreter.supported(asExtAddress.type()));
        assertTrue("Application data extension address should be supported",
                interpreter.supported(appDataExtAddress.type()));
        assertTrue("Geo Coordinate extension address should be supported",
                interpreter.supported(gcExtAddress.type()));
        assertTrue("NAT extension address should be supported ",
                interpreter.supported(natExtAddress.type()));
        assertTrue("Nonce extension address should be supported",
                interpreter.supported(nonceExtAddress.type()));
        assertTrue("Multicast extension address should be supported",
                interpreter.supported(multicastExtAddress.type()));
        assertTrue("Traffic engineering extension address should be supported",
                interpreter.supported(teExtAddress.type()));
        assertTrue("Source Destination extension address should be supported",
                interpreter.supported(srcDstExtAddress.type()));
    }

    /**
     * Tests conversion from LISP extension mapping address to LCAF address.
     */
    @Test
    public void testMapMappingAddress() {

        new EqualsTester()
                .addEqualityGroup(listLcafAddress, interpreter.mapMappingAddress(listExtAddress))
                .addEqualityGroup(segmentLcafAddress, interpreter.mapMappingAddress(segmentExtAddress))
                .addEqualityGroup(asLcafAddress, interpreter.mapMappingAddress(asExtAddress))
                .addEqualityGroup(appDataLcafAddress, interpreter.mapMappingAddress(appDataExtAddress))
                .addEqualityGroup(gcLcafAddress, interpreter.mapMappingAddress(gcExtAddress))
                .addEqualityGroup(natLcafAddress, interpreter.mapMappingAddress(natExtAddress))
                .addEqualityGroup(nonceLcafAddress, interpreter.mapMappingAddress(nonceExtAddress))
                .addEqualityGroup(multicastLcafAddress, interpreter.mapMappingAddress(multicastExtAddress))
                .addEqualityGroup(teLcafAddress, interpreter.mapMappingAddress(teExtAddress))
                .addEqualityGroup(srcDstLcafAddress, interpreter.mapMappingAddress(srcDstExtAddress))
                .testEquals();
    }

    /**
     * Tests conversion from LCAF address to LISP extension mapping address.
     */
    @Test
    public void testMapLcafAddress() {

        new EqualsTester()
                .addEqualityGroup(listExtAddress, interpreter.mapLcafAddress(listLcafAddress))
                .addEqualityGroup(segmentExtAddress, interpreter.mapLcafAddress(segmentLcafAddress))
                .addEqualityGroup(asExtAddress, interpreter.mapLcafAddress(asLcafAddress))
                .addEqualityGroup(appDataExtAddress, interpreter.mapLcafAddress(appDataLcafAddress))
                .addEqualityGroup(gcExtAddress, interpreter.mapLcafAddress(gcLcafAddress))
                .addEqualityGroup(natExtAddress, interpreter.mapLcafAddress(natLcafAddress))
                .addEqualityGroup(nonceExtAddress, interpreter.mapLcafAddress(nonceLcafAddress))
                .addEqualityGroup(multicastExtAddress, interpreter.mapLcafAddress(multicastLcafAddress))
                .addEqualityGroup(teExtAddress, interpreter.mapLcafAddress(teLcafAddress))
                .addEqualityGroup(srcDstExtAddress, interpreter.mapLcafAddress(srcDstLcafAddress))
                .testEquals();
    }

    /**
     * Tests encoding of an ExtensionMappingAddress object.
     */
    @Test
    public void testAddressEncode() {
        JsonNode listAddressJson =
                interpreter.encode(listExtAddress, context).get(LISP_LIST_ADDRESS);
        JsonNode segmentAddressJson =
                interpreter.encode(segmentExtAddress, context).get(LISP_SEGMENT_ADDRESS);
        JsonNode asAddressJson =
                interpreter.encode(asExtAddress, context).get(LISP_AS_ADDRESS);
        JsonNode appDataAddressJson =
                interpreter.encode(appDataExtAddress, context).get(LISP_APPLICATION_DATA_ADDRESS);
        JsonNode gcAddressJson =
                interpreter.encode(gcExtAddress, context).get(LISP_GEO_COORDINATE_ADDRESS);
        JsonNode natAddressJson =
                interpreter.encode(natExtAddress, context).get(LISP_NAT_ADDRESS);
        JsonNode nonceAddressJson =
                interpreter.encode(nonceExtAddress, context).get(LISP_NONCE_ADDRESS);
        JsonNode multicastAddressJson =
                interpreter.encode(multicastExtAddress, context).get(LISP_MULTICAST_ADDRESS);
        JsonNode teAddressJson =
                interpreter.encode(teExtAddress, context).get(LISP_TRAFFIC_ENGINEERING_ADDRESS);
        JsonNode srcDstAddressJson =
                interpreter.encode(srcDstExtAddress, context).get(LISP_SOURCE_DEST_ADDRESS);

        MatcherAssert.assertThat("errors in encoding List address JSON",
                listAddressJson, LispListAddressCodecTest.LispListAddressJsonMatcher
                        .matchesListAddress((LispListAddress) listExtAddress));

        MatcherAssert.assertThat("errors in encoding Segment address JSON",
                segmentAddressJson, LispSegmentAddressCodecTest.LispSegmentAddressJsonMatcher
                        .matchesSegmentAddress((LispSegmentAddress) segmentExtAddress));

        MatcherAssert.assertThat("errors in encoding AS address JSON",
                asAddressJson, LispAsAddressCodecTest.LispAsAddressJsonMatcher
                        .matchesAsAddress((LispAsAddress) asExtAddress));

        MatcherAssert.assertThat("errors in encoding AppData address JSON",
                appDataAddressJson, LispAppDataAddressCodecTest.LispAppDataAddressJsonMatcher
                        .matchesAppDataAddress((LispAppDataAddress) appDataExtAddress));

        MatcherAssert.assertThat("errors in encoding GC address JSON",
                gcAddressJson, LispGcAddressCodecTest.LispGcAddressJsonMatcher
                        .matchesGcAddress((LispGcAddress) gcExtAddress));

        MatcherAssert.assertThat("errors in encoding NAT address JSON",
                natAddressJson, LispNatAddressCodecTest.LispNatAddressJsonMatcher
                        .matchesNatAddress((LispNatAddress) natExtAddress));

        MatcherAssert.assertThat("errors in encoding Nonce address JSON",
                nonceAddressJson, LispNonceAddressCodecTest.LispNonceAddressJsonMatcher
                        .matchesNonceAddress((LispNonceAddress) nonceExtAddress));

        MatcherAssert.assertThat("errors in encoding Multicast address JSON",
                multicastAddressJson, LispMulticastAddressCodecTest.LispMulticastAddressJsonMatcher
                        .matchesMulticastAddress((LispMulticastAddress) multicastExtAddress));

        MatcherAssert.assertThat("errors in encoding TE address JSON",
                teAddressJson, LispTeAddressCodecTest.LispTeAddressJsonMatcher
                        .matchesTeAddress((LispTeAddress) teExtAddress));

        MatcherAssert.assertThat("errors in encoding SrcDst address JSON",
                srcDstAddressJson, LispSrcDstAddressCodecTest.LispSrcDstAddressJsonMatcher
                        .matchesSrcDstAddress((LispSrcDstAddress) srcDstExtAddress));
    }

    /**
     * Tests decoding of an ExtensionMappingAddress JSON object.
     */
    @Test
    public void testAddressDecode() throws IOException {
        List<ExtensionMappingAddress> addresses =
                getLispExtensionMappingAddresses("LispExtensionMappingAddress.json");

        new EqualsTester()
                .addEqualityGroup(addresses.get(0), listExtAddress)
                .addEqualityGroup(addresses.get(1), segmentExtAddress)
                .addEqualityGroup(addresses.get(2), asExtAddress)
                .addEqualityGroup(addresses.get(3), appDataExtAddress)
                .addEqualityGroup(addresses.get(4), gcExtAddress)
                .addEqualityGroup(addresses.get(5), natExtAddress)
                .addEqualityGroup(addresses.get(6), nonceExtAddress)
                .addEqualityGroup(addresses.get(7), multicastExtAddress)
                .addEqualityGroup(addresses.get(8), teExtAddress)
                .addEqualityGroup(addresses.get(9), srcDstExtAddress)
                .testEquals();
    }

    /**
     * Reads in a collection of LispExtensionMappingAddresses from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded LispExtensionMappingAddresses
     * @throws IOException if processing the resource fails
     */
    private List<ExtensionMappingAddress> getLispExtensionMappingAddresses(String resourceName)
            throws IOException {
        InputStream jsonStream = LispExtensionMappingAddressInterpreterTest.class
                                 .getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat("JSON string should not be null", json, notNullValue());

        final List<ExtensionMappingAddress> addresses = Lists.newArrayList();

        for (int addrIndex = 0; addrIndex < json.size(); addrIndex++) {
            ExtensionMappingAddress address = interpreter.decode(json.get(addrIndex).deepCopy(), context);
            assertThat("decoded address should not be null", address, notNullValue());
            addresses.add(address);
        }

        return addresses;
    }

    private LispLcafAddress getLcafMappingAddress(ExtensionMappingAddressType type) {
        LispLcafAddress address = null;

        LispAfiAddress ipv4Addr = new LispIpv4Address(IPV4_ADDRESS);
        LispAfiAddress ipv6Addr = new LispIpv6Address(IPV6_ADDRESS);

        if (type.equals(LIST_ADDRESS.type())) {
            address = new LispListLcafAddress(ImmutableList.of(ipv4Addr, ipv6Addr));
        }

        if (type.equals(SEGMENT_ADDRESS.type())) {
            address = new LispSegmentLcafAddress.SegmentAddressBuilder()
                                .withInstanceId(UNIQUE_INT)
                                .withAddress(ipv4Addr)
                                .build();
        }

        if (type.equals(AS_ADDRESS.type())) {
            address = new LispAsLcafAddress.AsAddressBuilder()
                                .withAsNumber(UNIQUE_INT)
                                .withAddress(ipv4Addr)
                                .build();
        }

        if (type.equals(APPLICATION_DATA_ADDRESS.type())) {
            address = new LispAppDataLcafAddress.AppDataAddressBuilder()
                                .withProtocol(UNIQUE_BYTE)
                                .withIpTos(UNIQUE_INT)
                                .withLocalPortLow(UNIQUE_SHORT)
                                .withLocalPortHigh(UNIQUE_SHORT)
                                .withRemotePortLow(UNIQUE_SHORT)
                                .withRemotePortHigh(UNIQUE_SHORT)
                                .withAddress(ipv4Addr)
                                .build();
        }

        if (type.equals(GEO_COORDINATE_ADDRESS.type())) {
            address = new LispGeoCoordinateLcafAddress.GeoCoordinateAddressBuilder()
                                .withIsNorth(UNIQUE_BOOLEAN)
                                .withLatitudeDegree(UNIQUE_SHORT)
                                .withLatitudeMinute(UNIQUE_BYTE)
                                .withLatitudeSecond(UNIQUE_BYTE)
                                .withIsEast(UNIQUE_BOOLEAN)
                                .withLongitudeDegree(UNIQUE_SHORT)
                                .withLongitudeMinute(UNIQUE_BYTE)
                                .withLongitudeSecond(UNIQUE_BYTE)
                                .withAltitude(UNIQUE_INT)
                                .withAddress(ipv4Addr)
                                .build();
        }

        if (type.equals(NAT_ADDRESS.type())) {
            address = new LispNatLcafAddress.NatAddressBuilder()
                                .withMsUdpPortNumber(UNIQUE_SHORT)
                                .withEtrUdpPortNumber(UNIQUE_SHORT)
                                .withGlobalEtrRlocAddress(ipv4Addr)
                                .withMsRlocAddress(ipv4Addr)
                                .withPrivateEtrRlocAddress(ipv4Addr)
                                .withRtrRlocAddresses(ImmutableList.of(ipv4Addr, ipv6Addr))
                                .build();
        }

        if (type.equals(NONCE_ADDRESS.type())) {
            address = new LispNonceLcafAddress.NonceAddressBuilder()
                                .withNonce(UNIQUE_INT)
                                .withAddress(ipv4Addr)
                                .build();
        }

        if (type.equals(MULTICAST_ADDRESS.type())) {
            address = new LispMulticastLcafAddress.MulticastAddressBuilder()
                                .withInstanceId(UNIQUE_INT)
                                .withSrcMaskLength(UNIQUE_BYTE)
                                .withSrcAddress(ipv4Addr)
                                .withGrpMaskLength(UNIQUE_BYTE)
                                .withGrpAddress(ipv4Addr)
                                .build();
        }

        if (type.equals(TRAFFIC_ENGINEERING_ADDRESS.type())) {
            LispTeRecord tr = new LispTeRecord.TeRecordBuilder()
                                .withIsLookup(UNIQUE_BOOLEAN)
                                .withIsRlocProbe(UNIQUE_BOOLEAN)
                                .withIsStrict(UNIQUE_BOOLEAN)
                                .withRtrRlocAddress(ipv4Addr)
                                .build();

            address = new LispTeLcafAddress.TeAddressBuilder()
                                .withTeRecords(ImmutableList.of(tr))
                                .build();
        }

        if (type.equals(SOURCE_DEST_ADDRESS.type())) {
            address = new LispSourceDestLcafAddress.SourceDestAddressBuilder()
                                .withSrcMaskLength(UNIQUE_BYTE)
                                .withSrcPrefix(ipv4Addr)
                                .withDstMaskLength(UNIQUE_BYTE)
                                .withDstPrefix(ipv4Addr)
                                .build();
        }

        return address;
    }

    private ExtensionMappingAddress getExtMappingAddress(ExtensionMappingAddressType type) {

        ExtensionMappingAddress address = null;

        MappingAddress ipv4Addr = MappingAddresses.ipv4MappingAddress(IPV4_PREFIX);
        MappingAddress ipv6Addr = MappingAddresses.ipv6MappingAddress(IPV6_PREFIX);

        if (type.equals(LIST_ADDRESS.type())) {
            address = new LispListAddress.Builder()
                                .withIpv4(ipv4Addr)
                                .withIpv6(ipv6Addr)
                                .build();
        }

        if (type.equals(SEGMENT_ADDRESS.type())) {
            address = new LispSegmentAddress.Builder()
                                .withInstanceId(UNIQUE_INT)
                                .withAddress(ipv4Addr)
                                .build();
        }

        if (type.equals(AS_ADDRESS.type())) {
            address = new LispAsAddress.Builder()
                                .withAsNumber(UNIQUE_INT)
                                .withAddress(ipv4Addr)
                                .build();
        }

        if (type.equals(APPLICATION_DATA_ADDRESS.type())) {
            address = new LispAppDataAddress.Builder()
                                .withProtocol(UNIQUE_BYTE)
                                .withIpTos(UNIQUE_INT)
                                .withLocalPortLow(UNIQUE_SHORT)
                                .withLocalPortHigh(UNIQUE_SHORT)
                                .withRemotePortLow(UNIQUE_SHORT)
                                .withRemotePortHigh(UNIQUE_SHORT)
                                .withAddress(ipv4Addr)
                                .build();
        }

        if (type.equals(GEO_COORDINATE_ADDRESS.type())) {
            address = new LispGcAddress.Builder()
                                .withIsNorth(UNIQUE_BOOLEAN)
                                .withLatitudeDegree(UNIQUE_SHORT)
                                .withLatitudeMinute(UNIQUE_BYTE)
                                .withLatitudeSecond(UNIQUE_BYTE)
                                .withIsEast(UNIQUE_BOOLEAN)
                                .withLongitudeDegree(UNIQUE_SHORT)
                                .withLongitudeMinute(UNIQUE_BYTE)
                                .withLongitudeSecond(UNIQUE_BYTE)
                                .withAltitude(UNIQUE_INT)
                                .withAddress(ipv4Addr)
                                .build();
        }

        if (type.equals(NAT_ADDRESS.type())) {
            address = new LispNatAddress.Builder()
                                .withMsUdpPortNumber(UNIQUE_SHORT)
                                .withEtrUdpPortNumber(UNIQUE_SHORT)
                                .withGlobalEtrRlocAddress(ipv4Addr)
                                .withMsRlocAddress(ipv4Addr)
                                .withPrivateEtrRlocAddress(ipv4Addr)
                                .withRtrRlocAddresses(ImmutableList.of(ipv4Addr, ipv6Addr))
                                .build();
        }

        if (type.equals(NONCE_ADDRESS.type())) {
            address = new LispNonceAddress.Builder()
                                .withNonce(UNIQUE_INT)
                                .withAddress(ipv4Addr)
                                .build();
        }

        if (type.equals(MULTICAST_ADDRESS.type())) {
            address = new LispMulticastAddress.Builder()
                                .withInstanceId(UNIQUE_INT)
                                .withSrcMaskLength(UNIQUE_BYTE)
                                .withSrcAddress(ipv4Addr)
                                .withGrpMaskLength(UNIQUE_BYTE)
                                .withGrpAddress(ipv4Addr)
                                .build();
        }

        if (type.equals(TRAFFIC_ENGINEERING_ADDRESS.type())) {

            LispTeAddress.TeRecord tr = new LispTeAddress.TeRecord.Builder()
                                .withIsLookup(UNIQUE_BOOLEAN)
                                .withIsRlocProbe(UNIQUE_BOOLEAN)
                                .withIsStrict(UNIQUE_BOOLEAN)
                                .withRtrRlocAddress(ipv4Addr)
                                .build();

            address = new LispTeAddress.Builder()
                                .withTeRecords(ImmutableList.of(tr))
                                .build();
        }

        if (type.equals(SOURCE_DEST_ADDRESS.type())) {
            address = new LispSrcDstAddress.Builder()
                                .withSrcMaskLength(UNIQUE_BYTE)
                                .withSrcPrefix(ipv4Addr)
                                .withDstMaskLength(UNIQUE_BYTE)
                                .withDstPrefix(ipv4Addr)
                                .build();
        }
        return address;
    }
}
