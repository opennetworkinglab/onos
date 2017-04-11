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
package org.onosproject.drivers.lisp.extensions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.lisp.ctl.ExtensionMappingAddressInterpreter;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispDistinguishedNameAddress;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.types.LispIpv6Address;
import org.onosproject.lisp.msg.types.LispMacAddress;
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
import org.onosproject.mapping.addresses.ExtensionMappingAddressResolver;
import org.onosproject.mapping.addresses.ExtensionMappingAddressType;
import org.onosproject.mapping.addresses.IPMappingAddress;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.onosproject.mapping.addresses.ExtensionMappingAddressType.ExtensionMappingAddressTypes.*;
/**
 * Interpreter for mapping address extension.
 */
public class LispExtensionMappingAddressInterpreter extends AbstractHandlerBehaviour
        implements ExtensionMappingAddressInterpreter, ExtensionMappingAddressResolver {

    private static final Logger log = LoggerFactory.getLogger(
            LispExtensionMappingAddressInterpreter.class);

    private static final int IPV4_PREFIX_LENGTH = 32;
    private static final int IPV6_PREFIX_LENGTH = 128;

    protected static final String LISP_LIST_ADDRESS = "listAddress";
    protected static final String LISP_SEGMENT_ADDRESS = "segmentAddress";
    protected static final String LISP_AS_ADDRESS = "asAddress";
    protected static final String LISP_APPLICATION_DATA_ADDRESS = "applicationDataAddress";
    protected static final String LISP_GEO_COORDINATE_ADDRESS = "geoCoordinateAddress";
    protected static final String LISP_NAT_ADDRESS = "natAddress";
    protected static final String LISP_NONCE_ADDRESS = "nonceAddress";
    protected static final String LISP_MULTICAST_ADDRESS = "multicastAddress";
    protected static final String LISP_TRAFFIC_ENGINEERING_ADDRESS = "trafficEngineeringAddress";
    protected static final String LISP_SOURCE_DEST_ADDRESS = "sourceDestAddress";

    private static final String TYPE = "type";

    private static final String MISSING_MEMBER_MESSAGE =
            " member is required in LispExtensionMappingAddressInterpreter";

    @Override
    public boolean supported(ExtensionMappingAddressType type) {

        if (type.equals(LIST_ADDRESS.type())) {
            return true;
        }
        if (type.equals(SEGMENT_ADDRESS.type())) {
            return true;
        }
        if (type.equals(AS_ADDRESS.type())) {
            return true;
        }
        if (type.equals(APPLICATION_DATA_ADDRESS.type())) {
            return true;
        }
        if (type.equals(GEO_COORDINATE_ADDRESS.type())) {
            return true;
        }
        if (type.equals(NAT_ADDRESS.type())) {
            return true;
        }
        if (type.equals(NONCE_ADDRESS.type())) {
            return true;
        }
        if (type.equals(MULTICAST_ADDRESS.type())) {
            return true;
        }
        if (type.equals(TRAFFIC_ENGINEERING_ADDRESS.type())) {
            return true;
        }
        if (type.equals(SOURCE_DEST_ADDRESS.type())) {
            return true;
        }

        return false;
    }

    @Override
    public LispLcafAddress mapMappingAddress(ExtensionMappingAddress mappingAddress) {
        ExtensionMappingAddressType type = mappingAddress.type();

        if (type.equals(LIST_ADDRESS.type())) {

            LispListAddress listAddress = (LispListAddress) mappingAddress;
            LispAfiAddress ipv4 = mapping2afi(listAddress.getIpv4());
            LispAfiAddress ipv6 = mapping2afi(listAddress.getIpv6());

            if (ipv4 != null && ipv6 != null) {
                return new LispListLcafAddress(ImmutableList.of(ipv4, ipv6));
            } else {
                return new LispListLcafAddress(ImmutableList.of());
            }
        }

        if (type.equals(SEGMENT_ADDRESS.type())) {

            LispSegmentAddress segmentAddress = (LispSegmentAddress) mappingAddress;

            return new LispSegmentLcafAddress.SegmentAddressBuilder()
                    .withInstanceId(segmentAddress.getInstanceId())
                    .withAddress(getAfiAddress(segmentAddress.getAddress()))
                    .build();
        }

        if (type.equals(AS_ADDRESS.type())) {

            LispAsAddress asAddress = (LispAsAddress) mappingAddress;

            return new LispAsLcafAddress.AsAddressBuilder()
                            .withAsNumber(asAddress.getAsNumber())
                            .withAddress(getAfiAddress(asAddress.getAddress()))
                            .build();
        }

        if (type.equals(APPLICATION_DATA_ADDRESS.type())) {

            LispAppDataAddress appDataAddress = (LispAppDataAddress) mappingAddress;

            return new LispAppDataLcafAddress.AppDataAddressBuilder()
                            .withProtocol(appDataAddress.getProtocol())
                            .withIpTos(appDataAddress.getIpTos())
                            .withLocalPortLow(appDataAddress.getLocalPortLow())
                            .withLocalPortHigh(appDataAddress.getLocalPortHigh())
                            .withRemotePortLow(appDataAddress.getRemotePortLow())
                            .withRemotePortHigh(appDataAddress.getRemotePortHigh())
                            .withAddress(getAfiAddress(appDataAddress.getAddress()))
                            .build();
        }

        if (type.equals(GEO_COORDINATE_ADDRESS.type())) {

            LispGcAddress gcAddress = (LispGcAddress) mappingAddress;

            return new LispGeoCoordinateLcafAddress.GeoCoordinateAddressBuilder()
                            .withIsNorth(gcAddress.isNorth())
                            .withLatitudeDegree(gcAddress.getLatitudeDegree())
                            .withLatitudeMinute(gcAddress.getLatitudeMinute())
                            .withLatitudeSecond(gcAddress.getLatitudeSecond())
                            .withIsEast(gcAddress.isEast())
                            .withLongitudeDegree(gcAddress.getLongitudeDegree())
                            .withLongitudeMinute(gcAddress.getLongitudeMinute())
                            .withLongitudeSecond(gcAddress.getLongitudeSecond())
                            .withAltitude(gcAddress.getAltitude())
                            .withAddress(getAfiAddress(gcAddress.getAddress()))
                            .build();
        }

        if (type.equals(NAT_ADDRESS.type())) {

            LispNatAddress natAddress = (LispNatAddress) mappingAddress;

            List<LispAfiAddress> aas = Lists.newArrayList();

            natAddress.getRtrRlocAddresses()
                    .forEach(rtr -> aas.add(getAfiAddress(rtr)));

            return new LispNatLcafAddress.NatAddressBuilder()
                            .withMsUdpPortNumber(natAddress.getMsUdpPortNumber())
                            .withEtrUdpPortNumber(natAddress.getEtrUdpPortNumber())
                            .withMsRlocAddress(getAfiAddress(natAddress.getMsRlocAddress()))
                            .withGlobalEtrRlocAddress(
                                    getAfiAddress(natAddress.getGlobalEtrRlocAddress()))
                            .withPrivateEtrRlocAddress(
                                    getAfiAddress(natAddress.getPrivateEtrRlocAddress()))
                            .withRtrRlocAddresses(aas)
                            .build();
        }

        if (type.equals(NONCE_ADDRESS.type())) {

            LispNonceAddress nonceAddress = (LispNonceAddress) mappingAddress;

            return new LispNonceLcafAddress.NonceAddressBuilder()
                            .withNonce(nonceAddress.getNonce())
                            .withAddress(getAfiAddress(nonceAddress.getAddress()))
                            .build();
        }

        if (type.equals(MULTICAST_ADDRESS.type())) {

            LispMulticastAddress multicastAddress = (LispMulticastAddress) mappingAddress;

            return new LispMulticastLcafAddress.MulticastAddressBuilder()
                            .withInstanceId(multicastAddress.getInstanceId())
                            .withSrcAddress(getAfiAddress(multicastAddress.getSrcAddress()))
                            .withSrcMaskLength(multicastAddress.getSrcMaskLength())
                            .withGrpAddress(getAfiAddress(multicastAddress.getGrpAddress()))
                            .withGrpMaskLength(multicastAddress.getGrpMaskLength())
                            .build();
        }

        if (type.equals(TRAFFIC_ENGINEERING_ADDRESS.type())) {

            LispTeAddress teAddress = (LispTeAddress) mappingAddress;

            List<LispTeRecord> records = Lists.newArrayList();

            teAddress.getTeRecords().forEach(record -> {
                LispTeRecord teRecord =
                        new LispTeRecord.TeRecordBuilder()
                                .withIsLookup(record.isLookup())
                                .withIsRlocProbe(record.isRlocProbe())
                                .withIsStrict(record.isStrict())
                                .withRtrRlocAddress(getAfiAddress(
                                        record.getAddress()))
                                .build();
                records.add(teRecord);
            });

            return new LispTeLcafAddress.TeAddressBuilder()
                            .withTeRecords(records)
                            .build();
        }

        if (type.equals(SOURCE_DEST_ADDRESS.type())) {

            LispSrcDstAddress srcDstAddress = (LispSrcDstAddress) mappingAddress;

            return new LispSourceDestLcafAddress.SourceDestAddressBuilder()
                            .withSrcPrefix(getAfiAddress(srcDstAddress.getSrcPrefix()))
                            .withSrcMaskLength(srcDstAddress.getSrcMaskLength())
                            .withDstPrefix(getAfiAddress(srcDstAddress.getDstPrefix()))
                            .withDstMaskLength(srcDstAddress.getDstMaskLength())
                            .build();
        }

        log.error("Unsupported extension mapping address type {}", mappingAddress.type());

        return null;
    }

    @Override
    public ExtensionMappingAddress mapLcafAddress(LispLcafAddress lcafAddress) {

        switch (lcafAddress.getType()) {
            case LIST:
                LispListLcafAddress lcafListAddress = (LispListLcafAddress) lcafAddress;
                MappingAddress ipv4Ma =
                        afi2mapping(lcafListAddress.getAddresses().get(0));
                MappingAddress ipv6Ma =
                        afi2mapping(lcafListAddress.getAddresses().get(1));

                return new LispListAddress.Builder()
                        .withIpv4(ipv4Ma)
                        .withIpv6(ipv6Ma)
                        .build();

            case SEGMENT:
                LispSegmentLcafAddress segmentLcafAddress =
                        (LispSegmentLcafAddress) lcafAddress;

                return new LispSegmentAddress.Builder()
                        .withInstanceId(segmentLcafAddress.getInstanceId())
                        .withAddress(getMappingAddress(segmentLcafAddress.getAddress()))
                        .build();

            case AS:
                LispAsLcafAddress asLcafAddress = (LispAsLcafAddress) lcafAddress;

                return new org.onosproject.drivers.lisp.extensions.LispAsAddress.Builder()
                        .withAsNumber(asLcafAddress.getAsNumber())
                        .withAddress(getMappingAddress(asLcafAddress.getAddress()))
                        .build();

            case APPLICATION_DATA:

                LispAppDataLcafAddress appLcafAddress = (LispAppDataLcafAddress) lcafAddress;

                return new LispAppDataAddress.Builder()
                        .withProtocol(appLcafAddress.getProtocol())
                        .withIpTos(appLcafAddress.getIpTos())
                        .withLocalPortLow(appLcafAddress.getLocalPortLow())
                        .withLocalPortHigh(appLcafAddress.getLocalPortHigh())
                        .withRemotePortLow(appLcafAddress.getRemotePortLow())
                        .withRemotePortHigh(appLcafAddress.getRemotePortHigh())
                        .withAddress(getMappingAddress(appLcafAddress.getAddress()))
                        .build();

            case GEO_COORDINATE:

                LispGeoCoordinateLcafAddress gcLcafAddress =
                        (LispGeoCoordinateLcafAddress) lcafAddress;

                return new LispGcAddress.Builder()
                        .withIsNorth(gcLcafAddress.isNorth())
                        .withLatitudeDegree(gcLcafAddress.getLatitudeDegree())
                        .withLatitudeMinute(gcLcafAddress.getLatitudeMinute())
                        .withLatitudeSecond(gcLcafAddress.getLatitudeSecond())
                        .withIsEast(gcLcafAddress.isEast())
                        .withLongitudeDegree(gcLcafAddress.getLongitudeDegree())
                        .withLongitudeMinute(gcLcafAddress.getLongitudeMinute())
                        .withLongitudeSecond(gcLcafAddress.getLongitudeSecond())
                        .withAltitude(gcLcafAddress.getAltitude())
                        .withAddress(getMappingAddress(gcLcafAddress.getAddress()))
                        .build();

            case NAT:

                LispNatLcafAddress natLcafAddress = (LispNatLcafAddress) lcafAddress;

                List<MappingAddress> mas = Lists.newArrayList();

                natLcafAddress.getRtrRlocAddresses()
                        .forEach(rtr -> mas.add(getMappingAddress(rtr)));

                return new LispNatAddress.Builder()
                        .withMsUdpPortNumber(natLcafAddress.getMsUdpPortNumber())
                        .withEtrUdpPortNumber(natLcafAddress.getEtrUdpPortNumber())
                        .withMsRlocAddress(getMappingAddress(natLcafAddress.getMsRlocAddress()))
                        .withGlobalEtrRlocAddress(
                                getMappingAddress(natLcafAddress.getGlobalEtrRlocAddress()))
                        .withPrivateEtrRlocAddress(
                                getMappingAddress(natLcafAddress.getPrivateEtrRlocAddress()))
                        .withRtrRlocAddresses(mas)
                        .build();

            case NONCE:

                LispNonceLcafAddress nonceLcafAddress = (LispNonceLcafAddress) lcafAddress;

                return new LispNonceAddress.Builder()
                        .withNonce(nonceLcafAddress.getNonce())
                        .withAddress(getMappingAddress(nonceLcafAddress.getAddress()))
                        .build();

            case MULTICAST:

                LispMulticastLcafAddress multiLcafAddress =
                        (LispMulticastLcafAddress) lcafAddress;

                return new LispMulticastAddress.Builder()
                        .withInstanceId(multiLcafAddress.getInstanceId())
                        .withSrcAddress(getMappingAddress(multiLcafAddress.getSrcAddress()))
                        .withSrcMaskLength(multiLcafAddress.getSrcMaskLength())
                        .withGrpAddress(getMappingAddress(multiLcafAddress.getGrpAddress()))
                        .withGrpMaskLength(multiLcafAddress.getGrpMaskLength())
                        .build();

            case TRAFFIC_ENGINEERING:

                LispTeLcafAddress teLcafAddress = (LispTeLcafAddress) lcafAddress;

                List<LispTeAddress.TeRecord> records = Lists.newArrayList();

                teLcafAddress.getTeRecords().forEach(record -> {
                    LispTeAddress.TeRecord teRecord =
                            new LispTeAddress.TeRecord.Builder()
                                    .withIsLookup(record.isLookup())
                                    .withIsRlocProbe(record.isRlocProbe())
                                    .withIsStrict(record.isStrict())
                                    .withRtrRlocAddress(getMappingAddress(
                                            record.getRtrRlocAddress()))
                                    .build();
                    records.add(teRecord);
                });

                return new LispTeAddress.Builder()
                        .withTeRecords(records)
                        .build();

            case SECURITY:

                // TODO: need to implement security type later
                log.warn("security type will be implemented later");

                return null;

            case SOURCE_DEST:

                LispSourceDestLcafAddress srcDstLcafAddress =
                        (LispSourceDestLcafAddress) lcafAddress;


                return new LispSrcDstAddress.Builder()
                        .withSrcPrefix(getMappingAddress(srcDstLcafAddress.getSrcPrefix()))
                        .withSrcMaskLength(srcDstLcafAddress.getSrcMaskLength())
                        .withDstPrefix(getMappingAddress(srcDstLcafAddress.getDstPrefix()))
                        .withDstMaskLength(srcDstLcafAddress.getDstMaskLength())
                        .build();

            case UNSPECIFIED:
            case UNKNOWN:
            default:
                log.error("Unsupported LCAF type {}", lcafAddress.getType());
                return null;
        }
    }

    @Override
    public ExtensionMappingAddress getExtensionMappingAddress(
                                            ExtensionMappingAddressType type) {

        if (type.equals(LIST_ADDRESS.type())) {
            return new LispListAddress();
        }
        if (type.equals(SEGMENT_ADDRESS.type())) {
            return new LispSegmentAddress();
        }
        if (type.equals(AS_ADDRESS.type())) {
            return new LispAsAddress();
        }
        if (type.equals(APPLICATION_DATA_ADDRESS.type())) {
            return new LispAppDataAddress();
        }
        if (type.equals(GEO_COORDINATE_ADDRESS.type())) {
            return new LispGcAddress();
        }
        if (type.equals(NAT_ADDRESS.type())) {
            return new LispNatAddress();
        }
        if (type.equals(NONCE_ADDRESS.type())) {
            return new LispNonceAddress();
        }
        if (type.equals(MULTICAST_ADDRESS.type())) {
            return new LispMulticastAddress();
        }
        if (type.equals(TRAFFIC_ENGINEERING_ADDRESS.type())) {
            return new LispTeAddress();
        }
        if (type.equals(SOURCE_DEST_ADDRESS.type())) {
            return new LispSrcDstAddress();
        }

        return null;
    }

    /**
     * Converts AFI address to generalized mapping address.
     *
     * @param afi IP typed AFI address
     * @return generalized mapping address
     */
    private MappingAddress afi2mapping(LispAfiAddress afi) {
        switch (afi.getAfi()) {
            case IP4:
                IpAddress ipv4Address = ((LispIpv4Address) afi).getAddress();
                IpPrefix ipv4Prefix = IpPrefix.valueOf(ipv4Address, IPV4_PREFIX_LENGTH);
                return MappingAddresses.ipv4MappingAddress(ipv4Prefix);
            case IP6:
                IpAddress ipv6Address = ((LispIpv6Address) afi).getAddress();
                IpPrefix ipv6Prefix = IpPrefix.valueOf(ipv6Address, IPV6_PREFIX_LENGTH);
                return MappingAddresses.ipv6MappingAddress(ipv6Prefix);
            default:
                log.warn("Only support to convert IP address type");
                break;
        }
        return null;
    }

    /**
     * Converts mapping address to AFI address.
     *
     * @param address generalized mapping address
     * @return IP typed AFI address
     */
    private LispAfiAddress mapping2afi(MappingAddress address) {
        switch (address.type()) {
            case IPV4:
                IpPrefix ipv4Prefix = ((IPMappingAddress) address).ip();
                return new LispIpv4Address(ipv4Prefix.address());
            case IPV6:
                IpPrefix ipv6Prefix = ((IPMappingAddress) address).ip();
                return new LispIpv6Address(ipv6Prefix.address());
            default:
                log.warn("Only support to convert IP address type");
                break;
        }
        return null;
    }

    /**
     * Converts LispAfiAddress into abstracted mapping address.
     *
     * @param address LispAfiAddress
     * @return abstracted mapping address
     */
    private MappingAddress getMappingAddress(LispAfiAddress address) {

        if (address == null) {
            log.warn("Address is not specified.");
            return null;
        }

        switch (address.getAfi()) {
            case IP4:
                return afi2mapping(address);
            case IP6:
                return afi2mapping(address);
            case AS:
                int asNum = ((org.onosproject.lisp.msg.types.LispAsAddress) address).getASNum();
                return MappingAddresses.asMappingAddress(String.valueOf(asNum));
            case DISTINGUISHED_NAME:
                String dn = ((LispDistinguishedNameAddress)
                        address).getDistinguishedName();
                return MappingAddresses.dnMappingAddress(dn);
            case MAC:
                MacAddress macAddress = ((LispMacAddress) address).getAddress();
                return MappingAddresses.ethMappingAddress(macAddress);
            case LCAF:
                LispLcafAddress lcafAddress = (LispLcafAddress) address;
                return MappingAddresses.extensionMappingAddressWrapper(mapLcafAddress(lcafAddress));
            default:
                log.warn("Unsupported address AFI type {}", address.getAfi());
                break;
        }

        return null;
    }

    /**
     * Converts mapping address into afi address.
     *
     * @param address mapping address
     * @return afi address
     */
    private LispAfiAddress getAfiAddress(MappingAddress address) {

        if (address == null) {
            log.warn("Address is not specified.");
            return null;
        }

        switch (address.type()) {
            case IPV4:
                return mapping2afi(address);
            case IPV6:
                return mapping2afi(address);
            case AS:
                int asNum = ((org.onosproject.lisp.msg.types.LispAsAddress) address).getASNum();
                return new org.onosproject.lisp.msg.types.LispAsAddress(asNum);
            case DN:
                String dn = ((LispDistinguishedNameAddress) address).getDistinguishedName();
                return new LispDistinguishedNameAddress(dn);
            case ETH:
                MacAddress macAddress = ((LispMacAddress) address).getAddress();
                return new LispMacAddress(macAddress);
            case EXTENSION:
                ExtensionMappingAddress extAddress = (ExtensionMappingAddress) address;
                return mapMappingAddress(extAddress);
            default:
                log.warn("Unsupported address type {}", address.type());
                break;
        }

        return null;
    }

    @Override
    public ObjectNode encode(ExtensionMappingAddress mappingAddress, CodecContext context) {
        checkNotNull(mappingAddress, "Extension mapping address cannot be null");
        ExtensionMappingAddressType type = mappingAddress.type();
        ObjectNode root = context.mapper().createObjectNode();

        if (type.equals(LIST_ADDRESS.type())) {
            LispListAddress listAddress = (LispListAddress) mappingAddress;
            root.set(LISP_LIST_ADDRESS,
                    context.codec(LispListAddress.class).encode(listAddress, context));
        }
        if (type.equals(SEGMENT_ADDRESS.type())) {
            LispSegmentAddress segmentAddress = (LispSegmentAddress) mappingAddress;
            root.set(LISP_SEGMENT_ADDRESS,
                    context.codec(LispSegmentAddress.class).encode(segmentAddress, context));
        }
        if (type.equals(AS_ADDRESS.type())) {
            LispAsAddress asAddress = (LispAsAddress) mappingAddress;
            root.set(LISP_AS_ADDRESS,
                    context.codec(LispAsAddress.class).encode(asAddress, context));
        }
        if (type.equals(APPLICATION_DATA_ADDRESS.type())) {
            LispAppDataAddress appDataAddress = (LispAppDataAddress) mappingAddress;
            root.set(LISP_APPLICATION_DATA_ADDRESS,
                    context.codec(LispAppDataAddress.class).encode(appDataAddress, context));
        }
        if (type.equals(GEO_COORDINATE_ADDRESS.type())) {
            LispGcAddress gcAddress = (LispGcAddress) mappingAddress;
            root.set(LISP_GEO_COORDINATE_ADDRESS,
                    context.codec(LispGcAddress.class).encode(gcAddress, context));
        }
        if (type.equals(NAT_ADDRESS.type())) {
            LispNatAddress natAddress = (LispNatAddress) mappingAddress;
            root.set(LISP_NAT_ADDRESS,
                    context.codec(LispNatAddress.class).encode(natAddress, context));
        }
        if (type.equals(NONCE_ADDRESS.type())) {
            LispNonceAddress nonceAddress = (LispNonceAddress) mappingAddress;
            root.set(LISP_NONCE_ADDRESS, context.codec(LispNonceAddress.class).encode(nonceAddress, context));
        }
        if (type.equals(MULTICAST_ADDRESS.type())) {
            LispMulticastAddress multicastAddress = (LispMulticastAddress) mappingAddress;
            root.set(LISP_MULTICAST_ADDRESS,
                    context.codec(LispMulticastAddress.class).encode(multicastAddress, context));
        }
        if (type.equals(TRAFFIC_ENGINEERING_ADDRESS.type())) {
            LispTeAddress teAddress = (LispTeAddress) mappingAddress;
            root.set(LISP_TRAFFIC_ENGINEERING_ADDRESS,
                    context.codec(LispTeAddress.class).encode(teAddress, context));
        }
        if (type.equals(SOURCE_DEST_ADDRESS.type())) {
            LispSrcDstAddress srcDstAddress = (LispSrcDstAddress) mappingAddress;
            root.set(LISP_SOURCE_DEST_ADDRESS,
                    context.codec(LispSrcDstAddress.class).encode(srcDstAddress, context));
        }

        return root;
    }

    @Override
    public ExtensionMappingAddress decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse extension type
        String typeString = nullIsIllegal(json.get(TYPE),
                TYPE + MISSING_MEMBER_MESSAGE).asText();

        if (typeString.equals(LIST_ADDRESS.name())) {
            return context.codec(LispListAddress.class)
                    .decode(get(json, LISP_LIST_ADDRESS), context);
        }
        if (typeString.equals(SEGMENT_ADDRESS.name())) {
            return context.codec(LispSegmentAddress.class)
                    .decode(get(json, LISP_SEGMENT_ADDRESS), context);
        }
        if (typeString.equals(AS_ADDRESS.name())) {
            return context.codec(LispAsAddress.class)
                    .decode(get(json, LISP_AS_ADDRESS), context);
        }
        if (typeString.equals(APPLICATION_DATA_ADDRESS.name())) {
            return context.codec(LispAppDataAddress.class)
                    .decode(get(json, LISP_APPLICATION_DATA_ADDRESS), context);
        }
        if (typeString.equals(GEO_COORDINATE_ADDRESS.name())) {
            return context.codec(LispGcAddress.class)
                    .decode(get(json, LISP_GEO_COORDINATE_ADDRESS), context);
        }
        if (typeString.equals(NAT_ADDRESS.name())) {
            return context.codec(LispNatAddress.class)
                    .decode(get(json, LISP_NAT_ADDRESS), context);
        }
        if (typeString.equals(NONCE_ADDRESS.name())) {
            return context.codec(LispNonceAddress.class)
                    .decode(get(json, LISP_NONCE_ADDRESS), context);
        }
        if (typeString.equals(MULTICAST_ADDRESS.name())) {
            return context.codec(LispMulticastAddress.class)
                    .decode(get(json, LISP_MULTICAST_ADDRESS), context);
        }
        if (typeString.equals(TRAFFIC_ENGINEERING_ADDRESS.name())) {
            return context.codec(LispTeAddress.class)
                    .decode(get(json, LISP_TRAFFIC_ENGINEERING_ADDRESS), context);
        }
        if (typeString.equals(SOURCE_DEST_ADDRESS.name())) {
            return context.codec(LispSrcDstAddress.class)
                    .decode(get(json, LISP_SOURCE_DEST_ADDRESS), context);
        }

        throw new UnsupportedOperationException(
                "Driver does not support extension type " + typeString);
    }

    /**
     * Gets a child Object Node from a parent by name. If the child is not found
     * or does nor represent an object, null is returned.
     *
     * @param parent parent object
     * @param childName name of child to query
     * @return child object if found, null if not found or if not an object
     */
    private static ObjectNode get(ObjectNode parent, String childName) {
        JsonNode node = parent.path(childName);
        return node.isObject() && !node.isNull() ? (ObjectNode) node : null;
    }
}
