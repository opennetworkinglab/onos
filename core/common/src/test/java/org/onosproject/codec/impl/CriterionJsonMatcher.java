/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.codec.impl;

import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onlab.util.HexString;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPDscpCriterion;
import org.onosproject.net.flow.criteria.IPEcnCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.IPv6ExthdrFlagsCriterion;
import org.onosproject.net.flow.criteria.IPv6FlowLabelCriterion;
import org.onosproject.net.flow.criteria.IPv6NDLinkLayerAddressCriterion;
import org.onosproject.net.flow.criteria.IPv6NDTargetAddressCriterion;
import org.onosproject.net.flow.criteria.IcmpCodeCriterion;
import org.onosproject.net.flow.criteria.IcmpTypeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6CodeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6TypeCriterion;
import org.onosproject.net.flow.criteria.MetadataCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.flow.criteria.OchSignalTypeCriterion;
import org.onosproject.net.flow.criteria.OduSignalIdCriterion;
import org.onosproject.net.flow.criteria.OduSignalTypeCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.SctpPortCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.criteria.VlanPcpCriterion;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;

/**
 * Hamcrest matcher for criterion objects.
 */
public final class CriterionJsonMatcher extends
        TypeSafeDiagnosingMatcher<JsonNode> {

    final Criterion criterion;
    Description description;
    JsonNode jsonCriterion;

    /**
     * Constructs a matcher object.
     *
     * @param criterionValue criterion to match
     */
    private CriterionJsonMatcher(Criterion criterionValue) {
        criterion = criterionValue;
    }

    /**
     * Factory to allocate an criterion matcher.
     *
     * @param criterion criterion object we are looking for
     * @return matcher
     */
    public static CriterionJsonMatcher matchesCriterion(Criterion criterion) {
        return new CriterionJsonMatcher(criterion);
    }

    /**
     * Matches a port criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(PortCriterion criterion) {
        final long port = criterion.port().toLong();
        final long jsonPort = jsonCriterion.get("port").asLong();
        if (port != jsonPort) {
            description.appendText("port was " + Long.toString(jsonPort));
            return false;
        }
        return true;
    }

    /**
     * Matches a metadata criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(MetadataCriterion criterion) {
        final long metadata = criterion.metadata();
        final long jsonMetadata = jsonCriterion.get("metadata").asLong();
        if (metadata != jsonMetadata) {
            description.appendText("metadata was "
                    + Long.toString(jsonMetadata));
            return false;
        }
        return true;
    }

    /**
     * Matches an eth criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(EthCriterion criterion) {
        final String mac = criterion.mac().toString();
        final String jsonMac = jsonCriterion.get("mac").textValue();
        if (!mac.equals(jsonMac)) {
            description.appendText("mac was " + jsonMac);
            return false;
        }
        return true;
    }

    /**
     * Matches an eth type criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(EthTypeCriterion criterion) {
        final int ethType = criterion.ethType().toShort() & 0xffff;
        final int jsonEthType = Integer.decode(jsonCriterion.get("ethType").textValue()) & 0xffff;
        if (ethType != jsonEthType) {
            description.appendText("ethType was "
                    + Integer.toString(jsonEthType));
            return false;
        }
        return true;
    }

    /**
     * Matches a VLAN ID criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(VlanIdCriterion criterion) {
        final short vlanId = criterion.vlanId().toShort();
        final short jsonVlanId = jsonCriterion.get("vlanId").shortValue();
        if (vlanId != jsonVlanId) {
            description.appendText("vlanId was " + Short.toString(jsonVlanId));
            return false;
        }
        return true;
    }

    /**
     * Matches a VLAN PCP criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(VlanPcpCriterion criterion) {
        final byte priority = criterion.priority();
        final byte jsonPriority =
                (byte) jsonCriterion.get("priority").shortValue();
        if (priority != jsonPriority) {
            description.appendText("priority was " + Byte.toString(jsonPriority));
            return false;
        }
        return true;
    }

    /**
     * Matches an IP DSCP criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(IPDscpCriterion criterion) {
        final byte ipDscp = criterion.ipDscp();
        final byte jsonIpDscp = (byte) jsonCriterion.get("ipDscp").shortValue();
        if (ipDscp != jsonIpDscp) {
            description.appendText("IP DSCP was " + Byte.toString(jsonIpDscp));
            return false;
        }
        return true;
    }

    /**
     * Matches an IP ECN criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(IPEcnCriterion criterion) {
        final byte ipEcn = criterion.ipEcn();
        final byte jsonIpEcn = (byte) jsonCriterion.get("ipEcn").shortValue();
        if (ipEcn != jsonIpEcn) {
            description.appendText("IP ECN was " + Byte.toString(jsonIpEcn));
            return false;
        }
        return true;
    }

    /**
     * Matches an IP protocol criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(IPProtocolCriterion criterion) {
        final short protocol = criterion.protocol();
        final short jsonProtocol = jsonCriterion.get("protocol").shortValue();
        if (protocol != jsonProtocol) {
            description.appendText("protocol was "
                    + Short.toString(jsonProtocol));
            return false;
        }
        return true;
    }

    /**
     * Matches an IP address criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(IPCriterion criterion) {
        final String ip = criterion.ip().toString();
        final String jsonIp = jsonCriterion.get("ip").textValue();
        if (!ip.equals(jsonIp)) {
            description.appendText("ip was " + jsonIp);
            return false;
        }
        return true;
    }

    /**
     * Matches a TCP port criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(TcpPortCriterion criterion) {
        final int tcpPort = criterion.tcpPort().toInt();
        final int jsonTcpPort = jsonCriterion.get("tcpPort").intValue();
        if (tcpPort != jsonTcpPort) {
            description.appendText("tcp port was "
                    + Integer.toString(jsonTcpPort));
            return false;
        }
        return true;
    }

    /**
     * Matches a UDP port criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(UdpPortCriterion criterion) {
        final int udpPort = criterion.udpPort().toInt();
        final int jsonUdpPort = jsonCriterion.get("udpPort").intValue();
        if (udpPort != jsonUdpPort) {
            description.appendText("udp port was "
                    + Integer.toString(jsonUdpPort));
            return false;
        }
        return true;
    }

    /**
     * Matches an SCTP port criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(SctpPortCriterion criterion) {
        final int sctpPort = criterion.sctpPort().toInt();
        final int jsonSctpPort = jsonCriterion.get("sctpPort").intValue();
        if (sctpPort != jsonSctpPort) {
            description.appendText("sctp port was "
                    + Integer.toString(jsonSctpPort));
            return false;
        }
        return true;
    }

    /**
     * Matches an ICMP type criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(IcmpTypeCriterion criterion) {
        final short icmpType = criterion.icmpType();
        final short jsonIcmpType = jsonCriterion.get("icmpType").shortValue();
        if (icmpType != jsonIcmpType) {
            description.appendText("icmp type was "
                    + Short.toString(jsonIcmpType));
            return false;
        }
        return true;
    }

    /**
     * Matches an ICMP code criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(IcmpCodeCriterion criterion) {
        final short icmpCode = criterion.icmpCode();
        final short jsonIcmpCode = jsonCriterion.get("icmpCode").shortValue();
        if (icmpCode != jsonIcmpCode) {
            description.appendText("icmp code was "
                    + Short.toString(jsonIcmpCode));
            return false;
        }
        return true;
    }

    /**
     * Matches an IPV6 flow label criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(IPv6FlowLabelCriterion criterion) {
        final int flowLabel = criterion.flowLabel();
        final int jsonFlowLabel = jsonCriterion.get("flowLabel").intValue();
        if (flowLabel != jsonFlowLabel) {
            description.appendText("IPv6 flow label was "
                    + Integer.toString(jsonFlowLabel));
            return false;
        }
        return true;
    }

    /**
     * Matches an ICMP V6 type criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(Icmpv6TypeCriterion criterion) {
        final short icmpv6Type = criterion.icmpv6Type();
        final short jsonIcmpv6Type =
                jsonCriterion.get("icmpv6Type").shortValue();
        if (icmpv6Type != jsonIcmpv6Type) {
            description.appendText("icmpv6 type was "
                    + Short.toString(jsonIcmpv6Type));
            return false;
        }
        return true;
    }

    /**
     * Matches an IPV6 code criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(Icmpv6CodeCriterion criterion) {
        final short icmpv6Code = criterion.icmpv6Code();
        final short jsonIcmpv6Code =
                jsonCriterion.get("icmpv6Code").shortValue();
        if (icmpv6Code != jsonIcmpv6Code) {
            description.appendText("icmpv6 code was "
                    + Short.toString(jsonIcmpv6Code));
            return false;
        }
        return true;
    }

    /**
     * Matches an IPV6 ND target criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(IPv6NDTargetAddressCriterion criterion) {
        final String targetAddress =
                criterion.targetAddress().toString();
        final String jsonTargetAddress =
                jsonCriterion.get("targetAddress").textValue();
        if (!targetAddress.equals(jsonTargetAddress)) {
            description.appendText("target address was " +
                    jsonTargetAddress);
            return false;
        }
        return true;
    }

    /**
     * Matches an IPV6 ND link layer criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(IPv6NDLinkLayerAddressCriterion criterion) {
        final String llAddress =
                criterion.mac().toString();
        final String jsonLlAddress =
                jsonCriterion.get("mac").textValue();
        if (!llAddress.equals(jsonLlAddress)) {
            description.appendText("mac was " + jsonLlAddress);
            return false;
        }
        return true;
    }

    /**
     * Matches an MPLS label criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(MplsCriterion criterion) {
        final int label = criterion.label().toInt();
        final int jsonLabel = jsonCriterion.get("label").intValue();
        if (label != jsonLabel) {
            description.appendText("label was " + Integer.toString(jsonLabel));
            return false;
        }
        return true;
    }

    /**
     * Matches an IPV6 exthdr criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(IPv6ExthdrFlagsCriterion criterion) {
        final int exthdrFlags = criterion.exthdrFlags();
        final int jsonExthdrFlags =
                jsonCriterion.get("exthdrFlags").intValue();
        if (exthdrFlags != jsonExthdrFlags) {
            description.appendText("exthdrFlags was "
                    + Long.toHexString(jsonExthdrFlags));
            return false;
        }
        return true;
    }

    /**
     * Matches an Och signal criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(OchSignalCriterion criterion) {
        final OchSignal ochSignal = criterion.lambda();
        final JsonNode jsonOchSignal = jsonCriterion.get("ochSignalId");
        String jsonGridType = jsonOchSignal.get("gridType").textValue();
        String jsonChannelSpacing = jsonOchSignal.get("channelSpacing").textValue();
        int jsonSpacingMultiplier = jsonOchSignal.get("spacingMultiplier").intValue();
        int jsonSlotGranularity = jsonOchSignal.get("slotGranularity").intValue();

        boolean equality = Objects.equals(ochSignal.gridType().name(), jsonGridType)
                && Objects.equals(ochSignal.channelSpacing().name(), jsonChannelSpacing)
                && Objects.equals(ochSignal.spacingMultiplier(), jsonSpacingMultiplier)
                && Objects.equals(ochSignal.slotGranularity(), jsonSlotGranularity);

        if (!equality) {
            String joined = Joiner.on(", ")
                    .join(jsonGridType, jsonChannelSpacing, jsonSpacingMultiplier, jsonSlotGranularity);

            description.appendText("och signal id was " + joined);
            return false;
        }
        return true;
    }

    /**
     * Matches an Och signal type criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(OchSignalTypeCriterion criterion) {
        final String signalType = criterion.signalType().name();
        final String jsonSignalType = jsonCriterion.get("ochSignalType").textValue();
        if (!signalType.equals(jsonSignalType)) {
            description.appendText("signal type was " + jsonSignalType);
            return false;
        }
        return true;
    }

   /**
     * Matches an ODU signal ID criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(OduSignalIdCriterion criterion) {
        final OduSignalId oduSignal = criterion.oduSignalId();
        final JsonNode jsonOduSignal = jsonCriterion.get(CriterionCodec.ODU_SIGNAL_ID);
        int jsonTpn = jsonOduSignal.get(CriterionCodec.TRIBUTARY_PORT_NUMBER).intValue();
        int jsonTsLen = jsonOduSignal.get(CriterionCodec.TRIBUTARY_SLOT_LEN).intValue();
        byte[] jsonTributaryBitMap = HexString.fromHexString(
                jsonOduSignal.get(CriterionCodec.TRIBUTARY_SLOT_BITMAP).asText());
        OduSignalId jsonOduSignalId = OduSignalId.oduSignalId(jsonTpn, jsonTsLen, jsonTributaryBitMap);
        if (!oduSignal.equals(jsonOduSignalId)) {
            description.appendText("oduSignalId was " + criterion);
            return false;
        }
       return true;
    }

    /**
     * Matches an ODU signal Type criterion object.
     *
     * @param criterion criterion to match
     * @return true if the JSON matches the criterion, false otherwise.
     */
    private boolean matchCriterion(OduSignalTypeCriterion criterion) {
        final String signalType = criterion.signalType().name();
        final String jsonOduSignalType = jsonCriterion.get("oduSignalType").textValue();
        if (!signalType.equals(jsonOduSignalType)) {
            description.appendText("signalType was " + signalType);
            return false;
        }
        return true;
    }


    @Override
    public boolean matchesSafely(JsonNode jsonCriterion,
                                 Description description) {
        this.description = description;
        this.jsonCriterion = jsonCriterion;
        final String type = criterion.type().name();
        final String jsonType = jsonCriterion.get("type").asText();
        if (!type.equals(jsonType)) {
            description.appendText("type was " + type);
            return false;
        }

        switch (criterion.type()) {

            case IN_PORT:
            case IN_PHY_PORT:
                return matchCriterion((PortCriterion) criterion);

            case METADATA:
                return matchCriterion((MetadataCriterion) criterion);

            case ETH_DST:
            case ETH_SRC:
                return matchCriterion((EthCriterion) criterion);

            case ETH_TYPE:
                return matchCriterion((EthTypeCriterion) criterion);

            case VLAN_VID:
                return matchCriterion((VlanIdCriterion) criterion);

            case VLAN_PCP:
                return matchCriterion((VlanPcpCriterion) criterion);

            case IP_DSCP:
                return matchCriterion((IPDscpCriterion) criterion);

            case IP_ECN:
                return matchCriterion((IPEcnCriterion) criterion);

            case IP_PROTO:
                return matchCriterion((IPProtocolCriterion) criterion);

            case IPV4_SRC:
            case IPV4_DST:
            case IPV6_SRC:
            case IPV6_DST:
                return matchCriterion((IPCriterion) criterion);

            case TCP_SRC:
            case TCP_DST:
                return matchCriterion((TcpPortCriterion) criterion);

            case UDP_SRC:
            case UDP_DST:
                return matchCriterion((UdpPortCriterion) criterion);

            case SCTP_SRC:
            case SCTP_DST:
                return matchCriterion((SctpPortCriterion) criterion);

            case ICMPV4_TYPE:
                return matchCriterion((IcmpTypeCriterion) criterion);

            case ICMPV4_CODE:
                return matchCriterion((IcmpCodeCriterion) criterion);

            case IPV6_FLABEL:
                return matchCriterion((IPv6FlowLabelCriterion) criterion);

            case ICMPV6_TYPE:
                return matchCriterion((Icmpv6TypeCriterion) criterion);

            case ICMPV6_CODE:
                return matchCriterion((Icmpv6CodeCriterion) criterion);

            case IPV6_ND_TARGET:
                return matchCriterion(
                        (IPv6NDTargetAddressCriterion) criterion);

            case IPV6_ND_SLL:
            case IPV6_ND_TLL:
                return matchCriterion(
                        (IPv6NDLinkLayerAddressCriterion) criterion);

            case MPLS_LABEL:
                return matchCriterion((MplsCriterion) criterion);

            case IPV6_EXTHDR:
                return matchCriterion(
                        (IPv6ExthdrFlagsCriterion) criterion);

            case OCH_SIGID:
                return matchCriterion((OchSignalCriterion) criterion);

            case OCH_SIGTYPE:
                return matchCriterion((OchSignalTypeCriterion) criterion);

            case ODU_SIGID:
                return matchCriterion((OduSignalIdCriterion) criterion);

            case ODU_SIGTYPE:
                return matchCriterion((OduSignalTypeCriterion) criterion);

            default:
                // Don't know how to format this type
                description.appendText("unknown criterion type " +
                        criterion.type());
                return false;
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(criterion.toString());
    }
}
