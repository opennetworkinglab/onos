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
package org.onosproject.provider.lisp.mapping.util;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.lisp.msg.protocols.DefaultLispLocator.DefaultLocatorBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapNotify.DefaultNotifyBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.DefaultMapRecordBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapReply.DefaultReplyBuilder;
import org.onosproject.lisp.msg.protocols.LispLocator;
import org.onosproject.lisp.msg.protocols.LispLocator.LocatorBuilder;
import org.onosproject.lisp.msg.protocols.LispMapNotify;
import org.onosproject.lisp.msg.protocols.LispMapNotify.NotifyBuilder;
import org.onosproject.lisp.msg.protocols.LispMapRecord;
import org.onosproject.lisp.msg.protocols.LispMapRecord.MapRecordBuilder;
import org.onosproject.lisp.msg.protocols.LispMapReply;
import org.onosproject.lisp.msg.protocols.LispMapReply.ReplyBuilder;
import org.onosproject.lisp.msg.protocols.LispMapReplyAction;
import org.onosproject.lisp.msg.types.AddressFamilyIdentifierEnum;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispAsAddress;
import org.onosproject.lisp.msg.types.LispDistinguishedNameAddress;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.types.LispIpv6Address;
import org.onosproject.lisp.msg.types.LispMacAddress;
import org.onosproject.lisp.msg.types.lcaf.LispAppDataLcafAddress;
import org.onosproject.lisp.msg.types.lcaf.LispAsLcafAddress;
import org.onosproject.lisp.msg.types.lcaf.LispCanonicalAddressFormatEnum;
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
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingKey;
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.MappingValue;
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.addresses.ASMappingAddress;
import org.onosproject.mapping.addresses.DNMappingAddress;
import org.onosproject.mapping.addresses.EthMappingAddress;
import org.onosproject.mapping.addresses.IPMappingAddress;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.net.DeviceId;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onosproject.lisp.msg.types.AddressFamilyIdentifierEnum.AS;
import static org.onosproject.lisp.msg.types.AddressFamilyIdentifierEnum.DISTINGUISHED_NAME;
import static org.onosproject.lisp.msg.types.AddressFamilyIdentifierEnum.IP4;
import static org.onosproject.lisp.msg.types.AddressFamilyIdentifierEnum.IP6;
import static org.onosproject.lisp.msg.types.AddressFamilyIdentifierEnum.MAC;
import static org.onosproject.lisp.msg.types.lcaf.LispCanonicalAddressFormatEnum.UNKNOWN;

/**
 * Mapping entry builder unit test.
 */
public class MappingEntryBuilderTest {

    private static final String IP_RECORD_ADDRESS = "192.168.1.1";
    private static final int IP_RECORD_MASK_LENGTH = 32;
    private static final int IP_LOCATOR_MASK_LENGTH = 32;

    private static final LispAfiAddress IPV4_ADDRESS_1 =
                         new LispIpv4Address(IpAddress.valueOf("1.2.3.4"));
    private static final LispAfiAddress IPV4_ADDRESS_2 =
                         new LispIpv4Address(IpAddress.valueOf("5.6.7.8"));
    private static final LispAfiAddress IPV6_ADDRESS =
                         new LispIpv6Address(IpAddress.valueOf(
                                 "1111:2222:3333:4444:5555:6666:7777:8885"));
    private static final LispAfiAddress MAC_ADDRESS =
                         new LispMacAddress(MacAddress.valueOf("00:00:00:00:00:01"));

    private static final IpPrefix IPV4_MAPPING_ADDRESS_1 = IpPrefix.valueOf("1.2.3.4/32");
    private static final IpPrefix IPV4_MAPPING_ADDRESS_2 = IpPrefix.valueOf("5.6.7.8/32");

    private static final IpPrefix IPV6_MAPPING_ADDRESS =
            IpPrefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8885/128");
    private static final MacAddress MAC_MAPPING_ADDRESS =
                                        MacAddress.valueOf("00:00:00:00:00:01");

    private static final byte UNIQUE_BYTE = (byte) 0x01;
    private static final int UNIQUE_INT = 1;
    private static final short UNIQUE_SHORT = 1;
    private static final boolean UNIQUE_BOOLEAN = true;
    private static final long UNIQUE_LONG = 1L;
    private static final String UNIQUE_STRING = "onos";

    private static final String AUTH_KEY = "onos";

    private static final DeviceId DEVICE_ID = DeviceId.deviceId("lisp:10.1.1.2");

    @Before
    public void setUp() {

    }

    @Test
    public void testMapReplyConversion() {

        ReplyBuilder replyBuilder = new DefaultReplyBuilder();

        List<LispMapRecord> records = ImmutableList.of(getMapRecord(IP4, UNKNOWN));

        LispMapReply mapReply = replyBuilder
                                    .withIsEtr(true)
                                    .withIsProbe(false)
                                    .withIsSecurity(true)
                                    .withNonce(UNIQUE_LONG)
                                    .withMapRecords(records)
                                    .build();

        List<LispMapRecord> replyRecords = mapReply.getMapRecords();

        assertThat(replyRecords.size(), is(1));

        testMapRecordConversion(replyRecords.get(0));
    }

    @Test
    public void testMapNotifyConversion() {

        List<LispMapRecord> records = ImmutableList.of(getMapRecord(IP4, UNKNOWN));

        NotifyBuilder notifyBuilder = new DefaultNotifyBuilder();

        LispMapNotify mapNotify = notifyBuilder
                                    .withKeyId(UNIQUE_SHORT)
                                    .withAuthKey(AUTH_KEY)
                                    .withNonce(UNIQUE_LONG)
                                    .withMapRecords(records)
                                    .build();

        List<LispMapRecord> notifyRecords = mapNotify.getMapRecords();

        assertThat(notifyRecords.size(), is(1));

        testMapRecordConversion(notifyRecords.get(0));
    }

    @Test
    public void testIpv4AddressConversion() {
        IPMappingAddress address = (IPMappingAddress) getMappingAddressByAfiType(IP4, UNKNOWN);
        assertThat(address.ip(), is(IPV4_MAPPING_ADDRESS_1));
    }

    @Test
    public void testIpv6AddressConversion() {
        IPMappingAddress address = (IPMappingAddress) getMappingAddressByAfiType(IP6, UNKNOWN);
        assertThat(address.ip(), is(IPV6_MAPPING_ADDRESS));
    }

    @Test
    public void testMacAddressConversion() {
        EthMappingAddress address = (EthMappingAddress) getMappingAddressByAfiType(MAC, UNKNOWN);
        assertThat(address.mac(), is(MAC_MAPPING_ADDRESS));
    }

    @Test
    public void testAsAddressConversion() {
        ASMappingAddress address = (ASMappingAddress) getMappingAddressByAfiType(AS, UNKNOWN);
        assertThat(address.asNumber(), is(String.valueOf(UNIQUE_INT)));
    }

    @Test
    public void testDnAddressConversion() {
        DNMappingAddress address = (DNMappingAddress)
                        getMappingAddressByAfiType(DISTINGUISHED_NAME, UNKNOWN);
        assertThat(address.name(), is(UNIQUE_STRING));
    }

    @Test
    public void testTeLcafAddressConversion() {
        // TODO: need to compare TeRecord list
    }

    private MappingAddress getMappingAddressByAfiType(AddressFamilyIdentifierEnum afiType,
                                                      LispCanonicalAddressFormatEnum lcafType) {
        LispMapRecord record = getMapRecord(afiType, lcafType);
        MappingEntry entry = new MappingEntryBuilder(DEVICE_ID, record).build();
        return entry.value().treatments().get(0).address();
    }

    private void testMapRecordConversion(LispMapRecord record) {
        MappingEntry mappingEntry =
                     new MappingEntryBuilder(DEVICE_ID, record).build();
        MappingKey key = mappingEntry.key();
        MappingValue value = mappingEntry.value();

        IPMappingAddress recordAddress = (IPMappingAddress) key.address();

        assertThat(recordAddress.ip(), is(IpPrefix.valueOf(IP_RECORD_ADDRESS + "/" +
                IP_RECORD_MASK_LENGTH)));

        assertThat(value.action().type(), is(MappingAction.Type.NATIVE_FORWARD));

        assertThat(value.treatments().size(), is(1));

        MappingTreatment treatment = value.treatments().get(0);
        IPMappingAddress locatorAddress = (IPMappingAddress) treatment.address();

        assertThat(locatorAddress.ip(), is(IpPrefix.valueOf(IPV4_ADDRESS_1 + "/" +
                IP_LOCATOR_MASK_LENGTH)));
    }

    /**
     * Obtains a MapRecord instance.
     *
     * @param afiType   AFI address type
     * @param lcafType  LCAF address type
     * @return a MapRecord instance
     */
    private LispMapRecord getMapRecord(AddressFamilyIdentifierEnum afiType,
                                       LispCanonicalAddressFormatEnum lcafType) {
        MapRecordBuilder recordBuilder = new DefaultMapRecordBuilder();

        LispIpv4Address recordAddress =
                        new LispIpv4Address(IpAddress.valueOf(IP_RECORD_ADDRESS));

        LocatorBuilder locatorBuilder = new DefaultLocatorBuilder();

        LispAfiAddress locatorAddress = getAfiAddress(afiType, lcafType);

        LispLocator locator = locatorBuilder
                                    .withPriority(UNIQUE_BYTE)
                                    .withWeight(UNIQUE_BYTE)
                                    .withMulticastPriority(UNIQUE_BYTE)
                                    .withMulticastWeight(UNIQUE_BYTE)
                                    .withLocalLocator(true)
                                    .withRlocProbed(false)
                                    .withRouted(true)
                                    .withLocatorAfi(locatorAddress)
                                    .build();

        return recordBuilder
                .withRecordTtl(UNIQUE_INT)
                .withIsAuthoritative(true)
                .withMapVersionNumber(UNIQUE_SHORT)
                .withMaskLength((byte) IP_RECORD_MASK_LENGTH)
                .withAction(LispMapReplyAction.NativelyForward)
                .withEidPrefixAfi(recordAddress)
                .withLocators(ImmutableList.of(locator))
                .build();
    }

    /**
     * Obtains the AFI address with respect to the AFI address type.
     *
     * @param afiType   AFI address type
     * @param lcafType  LCAF address type
     * @return AFI address instance
     */
    private LispAfiAddress getAfiAddress(AddressFamilyIdentifierEnum afiType,
                                         LispCanonicalAddressFormatEnum lcafType) {
        switch (afiType) {
            case IP4:
                return IPV4_ADDRESS_1;
            case IP6:
                return IPV6_ADDRESS;
            case AS:
                return new LispAsAddress(UNIQUE_INT);
            case DISTINGUISHED_NAME:
                return new LispDistinguishedNameAddress(UNIQUE_STRING);
            case MAC:
                return MAC_ADDRESS;
            case LCAF:
                return getLcafAddress(lcafType);
            default:
                return null;
        }
    }

    /**
     * Obtains the LCAF address with respect to the LCAF type.
     *
     * @param type LCAF type
     * @return LCAF address instance
     */
    private LispLcafAddress getLcafAddress(LispCanonicalAddressFormatEnum type) {

        List<LispAfiAddress> afiAddresses =
                                    ImmutableList.of(IPV4_ADDRESS_1, IPV6_ADDRESS);

        switch (type) {
            case LIST:
                return new LispListLcafAddress(afiAddresses);
            case SEGMENT:
                return new LispSegmentLcafAddress.SegmentAddressBuilder()
                                    .withIdMaskLength(UNIQUE_BYTE)
                                    .withInstanceId(UNIQUE_INT)
                                    .withAddress(IPV4_ADDRESS_1)
                                    .build();
            case AS:
                return new LispAsLcafAddress.AsAddressBuilder()
                                    .withAsNumber(UNIQUE_INT)
                                    .withAddress(IPV4_ADDRESS_1)
                                    .build();
            case APPLICATION_DATA:
                return new LispAppDataLcafAddress.AppDataAddressBuilder()
                                    .withProtocol(UNIQUE_BYTE)
                                    .withIpTos(UNIQUE_SHORT)
                                    .withLocalPortLow(UNIQUE_SHORT)
                                    .withLocalPortHigh(UNIQUE_SHORT)
                                    .withRemotePortLow(UNIQUE_SHORT)
                                    .withRemotePortHigh(UNIQUE_SHORT)
                                    .withAddress(IPV4_ADDRESS_1)
                                    .build();
            case GEO_COORDINATE:
                return new LispGeoCoordinateLcafAddress.GeoCoordinateAddressBuilder()
                                    .withIsNorth(UNIQUE_BOOLEAN)
                                    .withLatitudeDegree(UNIQUE_SHORT)
                                    .withLatitudeMinute(UNIQUE_BYTE)
                                    .withLatitudeSecond(UNIQUE_BYTE)
                                    .withIsEast(UNIQUE_BOOLEAN)
                                    .withLongitudeDegree(UNIQUE_SHORT)
                                    .withLongitudeMinute(UNIQUE_BYTE)
                                    .withLongitudeSecond(UNIQUE_BYTE)
                                    .withAltitude(UNIQUE_INT)
                                    .withAddress(IPV4_ADDRESS_1)
                                    .build();
            case NAT:
                return new LispNatLcafAddress.NatAddressBuilder()
                                    .withLength(UNIQUE_SHORT)
                                    .withMsUdpPortNumber(UNIQUE_SHORT)
                                    .withEtrUdpPortNumber(UNIQUE_SHORT)
                                    .withGlobalEtrRlocAddress(IPV4_ADDRESS_1)
                                    .withMsRlocAddress(IPV4_ADDRESS_1)
                                    .withPrivateEtrRlocAddress(IPV4_ADDRESS_1)
                                    .withRtrRlocAddresses(afiAddresses)
                                    .build();
            case NONCE:
                return new LispNonceLcafAddress.NonceAddressBuilder()
                                    .withNonce(UNIQUE_INT)
                                    .withAddress(IPV4_ADDRESS_1)
                                    .build();
            case MULTICAST:
                return new LispMulticastLcafAddress.MulticastAddressBuilder()
                                    .withInstanceId(UNIQUE_INT)
                                    .withSrcMaskLength(UNIQUE_BYTE)
                                    .withGrpMaskLength(UNIQUE_BYTE)
                                    .withSrcAddress(IPV4_ADDRESS_1)
                                    .withGrpAddress(IPV4_ADDRESS_1)
                                    .build();
            case TRAFFIC_ENGINEERING:
                LispTeRecord.TeRecordBuilder recordBuilder1 = new LispTeRecord.TeRecordBuilder();

                recordBuilder1.withIsLookup(UNIQUE_BOOLEAN);
                recordBuilder1.withIsRlocProbe(UNIQUE_BOOLEAN);
                recordBuilder1.withIsStrict(UNIQUE_BOOLEAN);
                recordBuilder1.withRtrRlocAddress(IPV4_ADDRESS_1);
                LispTeRecord record1 = recordBuilder1.build();

                LispTeRecord.TeRecordBuilder recordBuilder2 = new LispTeRecord.TeRecordBuilder();

                recordBuilder2.withIsLookup(UNIQUE_BOOLEAN);
                recordBuilder2.withIsRlocProbe(UNIQUE_BOOLEAN);
                recordBuilder2.withIsStrict(UNIQUE_BOOLEAN);
                recordBuilder2.withRtrRlocAddress(IPV4_ADDRESS_2);
                LispTeRecord record2 = recordBuilder2.build();

                return new LispTeLcafAddress.TeAddressBuilder()
                                    .withTeRecords(ImmutableList.of(record1, record2))
                                    .build();
            case SOURCE_DEST:
                return new LispSourceDestLcafAddress.SourceDestAddressBuilder()
                                    .withReserved(UNIQUE_SHORT)
                                    .withSrcMaskLength(UNIQUE_BYTE)
                                    .withDstMaskLength(UNIQUE_BYTE)
                                    .withSrcPrefix(IPV4_ADDRESS_1)
                                    .withDstPrefix(IPV4_ADDRESS_2)
                                    .build();
            case UNKNOWN:
            case UNSPECIFIED:
            default:
                return null;
        }
    }
}
