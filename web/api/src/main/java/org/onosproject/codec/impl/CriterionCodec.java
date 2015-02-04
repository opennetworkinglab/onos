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

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Criterion codec.
 */
public final class CriterionCodec extends JsonCodec<Criterion> {

    protected static final Logger log = LoggerFactory.getLogger(CriterionCodec.class);

    @Override
    public ObjectNode encode(Criterion criterion, CodecContext context) {
        checkNotNull(criterion, "Criterion cannot be null");

        final ObjectNode result = context.mapper().createObjectNode()
                .put("type", criterion.type().toString());

        switch (criterion.type()) {

            case IN_PORT:
            case IN_PHY_PORT:
                final Criteria.PortCriterion portCriterion = (Criteria.PortCriterion) criterion;
                result.put("port", portCriterion.port().toLong());
                break;

            case METADATA:
                final Criteria.MetadataCriterion metadataCriterion =
                        (Criteria.MetadataCriterion) criterion;
                result.put("metadata", metadataCriterion.metadata());
                break;

            case ETH_DST:
            case ETH_SRC:
                final Criteria.EthCriterion ethCriterion = (Criteria.EthCriterion) criterion;
                result.put("mac", ethCriterion.mac().toString());
                break;

            case ETH_TYPE:
                final Criteria.EthTypeCriterion ethTypeCriterion =
                        (Criteria.EthTypeCriterion) criterion;
                result.put("ethType", ethTypeCriterion.ethType());
                break;

            case VLAN_VID:
                final Criteria.VlanIdCriterion vlanIdCriterion =
                        (Criteria.VlanIdCriterion) criterion;
                result.put("vlanId", vlanIdCriterion.vlanId().toShort());
                break;

            case VLAN_PCP:
                final Criteria.VlanPcpCriterion vlanPcpCriterion =
                        (Criteria.VlanPcpCriterion) criterion;
                result.put("priority", vlanPcpCriterion.priority());
                break;

            case IP_DSCP:
                final Criteria.IPDscpCriterion ipDscpCriterion =
                        (Criteria.IPDscpCriterion) criterion;
                result.put("ipDscp", ipDscpCriterion.ipDscp());
                break;

            case IP_ECN:
                final Criteria.IPEcnCriterion ipEcnCriterion =
                        (Criteria.IPEcnCriterion) criterion;
                result.put("ipEcn", ipEcnCriterion.ipEcn());
                break;

            case IP_PROTO:
                final Criteria.IPProtocolCriterion iPProtocolCriterion =
                        (Criteria.IPProtocolCriterion) criterion;
                result.put("protocol", iPProtocolCriterion.protocol());
                break;

            case IPV4_SRC:
            case IPV4_DST:
            case IPV6_SRC:
            case IPV6_DST:
                final Criteria.IPCriterion iPCriterion = (Criteria.IPCriterion) criterion;
                result.put("ip", iPCriterion.ip().toString());
                break;

            case TCP_SRC:
            case TCP_DST:
                final Criteria.TcpPortCriterion tcpPortCriterion =
                        (Criteria.TcpPortCriterion) criterion;
                result.put("tcpPort", tcpPortCriterion.tcpPort().byteValue());
                break;

            case UDP_SRC:
            case UDP_DST:
                final Criteria.UdpPortCriterion udpPortCriterion =
                        (Criteria.UdpPortCriterion) criterion;
                result.put("udpPort", udpPortCriterion.udpPort().byteValue());
                break;

            case SCTP_SRC:
            case SCTP_DST:
                final Criteria.SctpPortCriterion sctpPortCriterion =
                        (Criteria.SctpPortCriterion) criterion;
                result.put("sctpPort",
                           sctpPortCriterion.sctpPort().byteValue());
                break;

            case ICMPV4_TYPE:
                final Criteria.IcmpTypeCriterion icmpTypeCriterion =
                        (Criteria.IcmpTypeCriterion) criterion;
                result.put("icmpType", icmpTypeCriterion.icmpType());
                break;

            case ICMPV4_CODE:
                final Criteria.IcmpCodeCriterion icmpCodeCriterion =
                        (Criteria.IcmpCodeCriterion) criterion;
                result.put("icmpCode", icmpCodeCriterion.icmpCode());
                break;

            case IPV6_FLABEL:
                final Criteria.IPv6FlowLabelCriterion ipv6FlowLabelCriterion =
                        (Criteria.IPv6FlowLabelCriterion) criterion;
                result.put("flowLabel",
                           ipv6FlowLabelCriterion.flowLabel());
                break;

            case ICMPV6_TYPE:
                final Criteria.Icmpv6TypeCriterion icmpv6TypeCriterion =
                        (Criteria.Icmpv6TypeCriterion) criterion;
                result.put("icmpv6Type", icmpv6TypeCriterion.icmpv6Type());
                break;

            case ICMPV6_CODE:
                final Criteria.Icmpv6CodeCriterion icmpv6CodeCriterion =
                        (Criteria.Icmpv6CodeCriterion) criterion;
                result.put("icmpv6Code", icmpv6CodeCriterion.icmpv6Code());
                break;

            case IPV6_ND_TARGET:
                final Criteria.IPv6NDTargetAddressCriterion ipv6NDTargetAddressCriterion
                    = (Criteria.IPv6NDTargetAddressCriterion) criterion;
                result.put("targetAddress",
                           ipv6NDTargetAddressCriterion.targetAddress().toString());
                break;

            case IPV6_ND_SLL:
            case IPV6_ND_TLL:
                final Criteria.IPv6NDLinkLayerAddressCriterion ipv6NDLinkLayerAddressCriterion
                    = (Criteria.IPv6NDLinkLayerAddressCriterion) criterion;
                result.put("mac",
                           ipv6NDLinkLayerAddressCriterion.mac().toString());
                break;

            case MPLS_LABEL:
                final Criteria.MplsCriterion mplsCriterion =
                        (Criteria.MplsCriterion) criterion;
                result.put("label", mplsCriterion.label());
                break;

            case OCH_SIGID:
                final Criteria.LambdaCriterion lambdaCriterion =
                        (Criteria.LambdaCriterion) criterion;
                result.put("lambda", lambdaCriterion.lambda());
                break;

            case OCH_SIGTYPE:
                final Criteria.OpticalSignalTypeCriterion opticalSignalTypeCriterion =
                        (Criteria.OpticalSignalTypeCriterion) criterion;
                result.put("signalType", opticalSignalTypeCriterion.signalType());
                break;

            default:
                // Don't know how to format this type
                log.info("Cannot convert criterion of type {} to JSON",
                        criterion.type());
                break;
        }

        return result;
    }
}
