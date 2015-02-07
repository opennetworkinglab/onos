/*
 * Copyright 2015 Open Networking Laboratory
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

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Hamcrest matcher for criterion objects.
 */
public final class CriterionJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    final Criterion criterion;

    private CriterionJsonMatcher(Criterion criterionValue) {
        criterion = criterionValue;
    }

    // CHECKSTYLE IGNORE MethodLength FOR NEXT 300 LINES
    @Override
    public boolean matchesSafely(JsonNode jsonCriterion,
                                 Description description) {
        final String type = criterion.type().name();
        final String jsonType = jsonCriterion.get("type").asText();
        if (!type.equals(jsonType)) {
            description.appendText("type was " + type);
            return false;
        }

        switch (criterion.type()) {

            case IN_PORT:
            case IN_PHY_PORT:
                final Criteria.PortCriterion portCriterion =
                        (Criteria.PortCriterion) criterion;
                final long port = portCriterion.port().toLong();
                final long jsonPort = jsonCriterion.get("port").asLong();
                if (port != jsonPort) {
                    description.appendText("port was " + Long.toString(jsonPort));
                    return false;
                }
                break;

            case METADATA:
                final Criteria.MetadataCriterion metadataCriterion =
                        (Criteria.MetadataCriterion) criterion;
                final long metadata = metadataCriterion.metadata();
                final long jsonMetadata = jsonCriterion.get("metadata").asLong();
                if (metadata != jsonMetadata) {
                    description.appendText("metadata was " + Long.toString(jsonMetadata));
                    return false;
                }
                break;

            case ETH_DST:
            case ETH_SRC:
                final Criteria.EthCriterion ethCriterion =
                    (Criteria.EthCriterion) criterion;
                final String mac = ethCriterion.mac().toString();
                final String jsonMac = jsonCriterion.get("mac").textValue();
                if (!mac.equals(jsonMac)) {
                    description.appendText("mac was " + jsonMac);
                    return false;
                }
                break;

            case ETH_TYPE:
                final Criteria.EthTypeCriterion ethTypeCriterion =
                        (Criteria.EthTypeCriterion) criterion;
                final String ethType =
                    Long.toHexString(ethTypeCriterion.ethType());
                final String jsonEthType = jsonCriterion.get("ethType").textValue();
                if (!ethType.equals(jsonEthType)) {
                    description.appendText("ethType was " + jsonEthType);
                    return false;
                }
                break;

            case VLAN_VID:
                final Criteria.VlanIdCriterion vlanIdCriterion =
                        (Criteria.VlanIdCriterion) criterion;
                final short vlanId = vlanIdCriterion.vlanId().toShort();
                final short jsonVlanId = jsonCriterion.get("vlanId").shortValue();
                if (vlanId != jsonVlanId) {
                    description.appendText("vlanId was " + Short.toString(jsonVlanId));
                    return false;
                }
                break;

            case VLAN_PCP:
                final Criteria.VlanPcpCriterion vlanPcpCriterion =
                        (Criteria.VlanPcpCriterion) criterion;
                final byte priority = vlanPcpCriterion.priority();
                final byte jsonPriority = (byte) jsonCriterion.get("priority").shortValue();
                if (priority != jsonPriority) {
                    description.appendText("priority was " + Byte.toString(jsonPriority));
                    return false;
                }
                break;

            case IP_DSCP:
                final Criteria.IPDscpCriterion ipDscpCriterion =
                        (Criteria.IPDscpCriterion) criterion;
                final byte ipDscp = ipDscpCriterion.ipDscp();
                final byte jsonIpDscp = (byte) jsonCriterion.get("ipDscp").shortValue();
                if (ipDscp != jsonIpDscp) {
                    description.appendText("IP DSCP was " + Byte.toString(jsonIpDscp));
                    return false;
                }
                break;

            case IP_ECN:
                final Criteria.IPEcnCriterion ipEcnCriterion =
                        (Criteria.IPEcnCriterion) criterion;
                final byte ipEcn = ipEcnCriterion.ipEcn();
                final byte jsonIpEcn = (byte) jsonCriterion.get("ipEcn").shortValue();
                if (ipEcn != jsonIpEcn) {
                    description.appendText("IP ECN was " + Byte.toString(jsonIpEcn));
                    return false;
                }
                break;

            case IP_PROTO:
                final Criteria.IPProtocolCriterion iPProtocolCriterion =
                        (Criteria.IPProtocolCriterion) criterion;
                final short protocol = iPProtocolCriterion.protocol();
                final short jsonProtocol = jsonCriterion.get("protocol").shortValue();
                if (protocol != jsonProtocol) {
                    description.appendText("protocol was " + Short.toString(jsonProtocol));
                    return false;
                }
                break;

            case IPV4_SRC:
            case IPV4_DST:
            case IPV6_SRC:
            case IPV6_DST:
                final Criteria.IPCriterion ipCriterion =
                    (Criteria.IPCriterion) criterion;
                final String ip = ipCriterion.ip().toString();
                final String jsonIp = jsonCriterion.get("ip").textValue();
                if (!ip.equals(jsonIp)) {
                    description.appendText("ip was " + jsonIp);
                    return false;
                }
                break;

            case TCP_SRC:
            case TCP_DST:
                final Criteria.TcpPortCriterion tcpPortCriterion =
                        (Criteria.TcpPortCriterion) criterion;
                final int tcpPort = tcpPortCriterion.tcpPort();
                final int jsonTcpPort = jsonCriterion.get("tcpPort").intValue();
                if (tcpPort != jsonTcpPort) {
                    description.appendText("tcp port was " + Integer.toString(jsonTcpPort));
                    return false;
                }
                break;

            case UDP_SRC:
            case UDP_DST:
                final Criteria.UdpPortCriterion udpPortCriterion =
                        (Criteria.UdpPortCriterion) criterion;
                final int udpPort = udpPortCriterion.udpPort();
                final int jsonUdpPort = jsonCriterion.get("udpPort").intValue();
                if (udpPort != jsonUdpPort) {
                    description.appendText("udp port was " + Integer.toString(jsonUdpPort));
                    return false;
                }
                break;

            case SCTP_SRC:
            case SCTP_DST:
                final Criteria.SctpPortCriterion sctpPortCriterion =
                        (Criteria.SctpPortCriterion) criterion;
                final int sctpPort = sctpPortCriterion.sctpPort();
                final int jsonSctpPort = jsonCriterion.get("sctpPort").intValue();
                if (sctpPort != jsonSctpPort) {
                    description.appendText("sctp port was " + Integer.toString(jsonSctpPort));
                    return false;
                }
                break;

            case ICMPV4_TYPE:
                final Criteria.IcmpTypeCriterion icmpTypeCriterion =
                        (Criteria.IcmpTypeCriterion) criterion;
                final short icmpType = icmpTypeCriterion.icmpType();
                final short jsonIcmpType = jsonCriterion.get("icmpType").shortValue();
                if (icmpType != jsonIcmpType) {
                    description.appendText("icmp type was " + Short.toString(jsonIcmpType));
                    return false;
                }
                break;

            case ICMPV4_CODE:
                final Criteria.IcmpCodeCriterion icmpCodeCriterion =
                        (Criteria.IcmpCodeCriterion) criterion;
                final short icmpCode = icmpCodeCriterion.icmpCode();
                final short jsonIcmpCode = jsonCriterion.get("icmpCode").shortValue();
                if (icmpCode != jsonIcmpCode) {
                    description.appendText("icmp code was " + Short.toString(jsonIcmpCode));
                    return false;
                }
                break;

            case IPV6_FLABEL:
                final Criteria.IPv6FlowLabelCriterion ipv6FlowLabelCriterion =
                        (Criteria.IPv6FlowLabelCriterion) criterion;
                final int flowLabel = ipv6FlowLabelCriterion.flowLabel();
                final int jsonFlowLabel = jsonCriterion.get("flowLabel").intValue();
                if (flowLabel != jsonFlowLabel) {
                    description.appendText("IPv6 flow label was " + Integer.toString(jsonFlowLabel));
                    return false;
                }
                break;

            case ICMPV6_TYPE:
                final Criteria.Icmpv6TypeCriterion icmpv6TypeCriterion =
                        (Criteria.Icmpv6TypeCriterion) criterion;
                final short icmpv6Type = icmpv6TypeCriterion.icmpv6Type();
                final short jsonIcmpv6Type = jsonCriterion.get("icmpv6Type").shortValue();
                if (icmpv6Type != jsonIcmpv6Type) {
                    description.appendText("icmpv6 type was " + Short.toString(jsonIcmpv6Type));
                    return false;
                }
                break;

            case ICMPV6_CODE:
                final Criteria.Icmpv6CodeCriterion icmpv6CodeCriterion =
                        (Criteria.Icmpv6CodeCriterion) criterion;
                final short icmpv6Code = icmpv6CodeCriterion.icmpv6Code();
                final short jsonIcmpv6Code = jsonCriterion.get("icmpv6Code").shortValue();
                if (icmpv6Code != jsonIcmpv6Code) {
                    description.appendText("icmpv6 code was " + Short.toString(jsonIcmpv6Code));
                    return false;
                }
                break;

            case IPV6_ND_TARGET:
                final Criteria.IPv6NDTargetAddressCriterion
                    ipv6NDTargetAddressCriterion =
                        (Criteria.IPv6NDTargetAddressCriterion) criterion;
                final String targetAddress =
                    ipv6NDTargetAddressCriterion.targetAddress().toString();
                final String jsonTargetAddress =
                    jsonCriterion.get("targetAddress").textValue();
                if (!targetAddress.equals(jsonTargetAddress)) {
                    description.appendText("target address was " +
                                           jsonTargetAddress);
                    return false;
                }
                break;

            case IPV6_ND_SLL:
            case IPV6_ND_TLL:
                final Criteria.IPv6NDLinkLayerAddressCriterion
                    ipv6NDLinkLayerAddressCriterion =
                        (Criteria.IPv6NDLinkLayerAddressCriterion) criterion;
                final String llAddress =
                    ipv6NDLinkLayerAddressCriterion.mac().toString();
                final String jsonLlAddress =
                    jsonCriterion.get("mac").textValue();
                if (!llAddress.equals(jsonLlAddress)) {
                    description.appendText("mac was " + jsonLlAddress);
                    return false;
                }
                break;

            case MPLS_LABEL:
                final Criteria.MplsCriterion mplsCriterion =
                        (Criteria.MplsCriterion) criterion;
                final int label = mplsCriterion.label();
                final int jsonLabel = jsonCriterion.get("label").intValue();
                if (label != jsonLabel) {
                    description.appendText("label was " + Integer.toString(jsonLabel));
                    return false;
                }
                break;

            case IPV6_EXTHDR:
                final Criteria.IPv6ExthdrFlagsCriterion exthdrCriterion =
                        (Criteria.IPv6ExthdrFlagsCriterion) criterion;
                final int exthdrFlags = exthdrCriterion.exthdrFlags();
                final int jsonExthdrFlags =
                    jsonCriterion.get("exthdrFlags").intValue();
                if (exthdrFlags != jsonExthdrFlags) {
                    description.appendText("exthdrFlags was " + Long.toHexString(jsonExthdrFlags));
                    return false;
                }
                break;

            case OCH_SIGID:
                final Criteria.LambdaCriterion lambdaCriterion =
                        (Criteria.LambdaCriterion) criterion;
                final int lambda = lambdaCriterion.lambda();
                final int jsonLambda = jsonCriterion.get("lambda").intValue();
                if (lambda != jsonLambda) {
                    description.appendText("lambda was " + Integer.toString(lambda));
                    return false;
                }
                break;

            case OCH_SIGTYPE:
                final Criteria.OpticalSignalTypeCriterion opticalSignalTypeCriterion =
                        (Criteria.OpticalSignalTypeCriterion) criterion;
                final short signalType = opticalSignalTypeCriterion.signalType();
                final short jsonSignalType = jsonCriterion.get("signalType").shortValue();
                if (signalType != jsonSignalType) {
                    description.appendText("signal type was " + Short.toString(signalType));
                    return false;
                }
                break;

            default:
                // Don't know how to format this type
                description.appendText("unknown criterion type " +
                        criterion.type());
                return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(criterion.toString());
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
}
