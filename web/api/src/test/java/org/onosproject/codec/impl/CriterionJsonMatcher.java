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
                final String ethType = ethTypeCriterion.ethType().toString();
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
                final byte protocol = iPProtocolCriterion.protocol();
                final byte jsonProtocol = (byte) jsonCriterion.get("protocol").shortValue();
                if (protocol != jsonProtocol) {
                    description.appendText("protocol was " + Byte.toString(jsonProtocol));
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
                final short tcpPort = tcpPortCriterion.tcpPort();
                final short jsonTcpPort = jsonCriterion.get("tcpPort").shortValue();
                if (tcpPort != jsonTcpPort) {
                    description.appendText("tcp port was " + Short.toString(jsonTcpPort));
                    return false;
                }
                break;

            case UDP_SRC:
            case UDP_DST:
                final Criteria.UdpPortCriterion udpPortCriterion =
                        (Criteria.UdpPortCriterion) criterion;
                final short udpPort = udpPortCriterion.udpPort();
                final short jsonUdpPort = jsonCriterion.get("udpPort").shortValue();
                if (udpPort != jsonUdpPort) {
                    description.appendText("udp port was " + Short.toString(jsonUdpPort));
                    return false;
                }
                break;

            case SCTP_SRC:
            case SCTP_DST:
                final Criteria.SctpPortCriterion sctpPortCriterion =
                        (Criteria.SctpPortCriterion) criterion;
                final short sctpPort = sctpPortCriterion.sctpPort();
                final short jsonSctpPort = jsonCriterion.get("sctpPort").shortValue();
                if (sctpPort != jsonSctpPort) {
                    description.appendText("sctp port was " + Short.toString(jsonSctpPort));
                    return false;
                }
                break;

            case ICMPV4_TYPE:
                final Criteria.IcmpTypeCriterion icmpTypeCriterion =
                        (Criteria.IcmpTypeCriterion) criterion;
                final byte icmpType = icmpTypeCriterion.icmpType();
                final byte jsonIcmpType = (byte) jsonCriterion.get("icmpType").shortValue();
                if (icmpType != jsonIcmpType) {
                    description.appendText("icmp type was " + Byte.toString(jsonIcmpType));
                    return false;
                }
                break;

            case ICMPV4_CODE:
                final Criteria.IcmpCodeCriterion icmpCodeCriterion =
                        (Criteria.IcmpCodeCriterion) criterion;
                final byte icmpCode = icmpCodeCriterion.icmpCode();
                final byte jsonIcmpCode = (byte) jsonCriterion.get("icmpCode").shortValue();
                if (icmpCode != jsonIcmpCode) {
                    description.appendText("icmp code was " + Byte.toString(jsonIcmpCode));
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
                final byte icmpv6Type = icmpv6TypeCriterion.icmpv6Type();
                final byte jsonIcmpv6Type = (byte) jsonCriterion.get("icmpv6Type").shortValue();
                if (icmpv6Type != jsonIcmpv6Type) {
                    description.appendText("icmpv6 type was " + Byte.toString(jsonIcmpv6Type));
                    return false;
                }
                break;

            case ICMPV6_CODE:
                final Criteria.Icmpv6CodeCriterion icmpv6CodeCriterion =
                        (Criteria.Icmpv6CodeCriterion) criterion;
                final byte icmpv6Code = icmpv6CodeCriterion.icmpv6Code();
                final byte jsonIcmpv6Code = (byte) jsonCriterion.get("icmpv6Code").shortValue();
                if (icmpv6Code != jsonIcmpv6Code) {
                    description.appendText("icmpv6 code was " + Byte.toString(jsonIcmpv6Code));
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

            case OCH_SIGID:
                final Criteria.LambdaCriterion lambdaCriterion =
                        (Criteria.LambdaCriterion) criterion;
                final short lambda = lambdaCriterion.lambda();
                final short jsonLambda = jsonCriterion.get("lambda").shortValue();
                if (lambda != jsonLambda) {
                    description.appendText("lambda was " + Short.toString(lambda));
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
