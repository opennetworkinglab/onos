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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.util.HexString;
import org.onosproject.codec.CodecContext;
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
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.flow.criteria.OchSignalTypeCriterion;
import org.onosproject.net.flow.criteria.OduSignalIdCriterion;
import org.onosproject.net.flow.criteria.OduSignalTypeCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.SctpPortCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.TunnelIdCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.criteria.VlanPcpCriterion;

import java.util.EnumMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encode portion of the criterion codec.
 */
public final class EncodeCriterionCodecHelper {

    private final Criterion criterion;
    private final CodecContext context;

    private final EnumMap<Criterion.Type, CriterionTypeFormatter> formatMap;

    /**
     * Creates an encoder object for a criterion.
     * Initializes the formatter lookup map for the criterion subclasses.
     *
     * @param criterion Criterion to encode
     * @param context   context of the JSON encoding
     */
    public EncodeCriterionCodecHelper(Criterion criterion, CodecContext context) {
        this.criterion = criterion;
        this.context = context;

        formatMap = new EnumMap<>(Criterion.Type.class);

        formatMap.put(Criterion.Type.IN_PORT, new FormatInPort());
        formatMap.put(Criterion.Type.IN_PHY_PORT, new FormatInPort());
        formatMap.put(Criterion.Type.METADATA, new FormatMetadata());
        formatMap.put(Criterion.Type.ETH_DST, new FormatEth());
        formatMap.put(Criterion.Type.ETH_SRC, new FormatEth());
        formatMap.put(Criterion.Type.ETH_TYPE, new FormatEthType());
        formatMap.put(Criterion.Type.VLAN_VID, new FormatVlanVid());
        formatMap.put(Criterion.Type.VLAN_PCP, new FormatVlanPcp());
        formatMap.put(Criterion.Type.INNER_VLAN_VID, new FormatInnerVlanVid());
        formatMap.put(Criterion.Type.INNER_VLAN_PCP, new FormatInnerVlanPcp());
        formatMap.put(Criterion.Type.IP_DSCP, new FormatIpDscp());
        formatMap.put(Criterion.Type.IP_ECN, new FormatIpEcn());
        formatMap.put(Criterion.Type.IP_PROTO, new FormatIpProto());
        formatMap.put(Criterion.Type.IPV4_SRC, new FormatIp());
        formatMap.put(Criterion.Type.IPV4_DST, new FormatIp());
        formatMap.put(Criterion.Type.TCP_SRC, new FormatTcp());
        formatMap.put(Criterion.Type.TCP_DST, new FormatTcp());
        formatMap.put(Criterion.Type.UDP_SRC, new FormatUdp());
        formatMap.put(Criterion.Type.UDP_DST, new FormatUdp());
        formatMap.put(Criterion.Type.SCTP_SRC, new FormatSctp());
        formatMap.put(Criterion.Type.SCTP_DST, new FormatSctp());
        formatMap.put(Criterion.Type.ICMPV4_TYPE, new FormatIcmpV4Type());
        formatMap.put(Criterion.Type.ICMPV4_CODE, new FormatIcmpV4Code());
        formatMap.put(Criterion.Type.IPV6_SRC, new FormatIp());
        formatMap.put(Criterion.Type.IPV6_DST, new FormatIp());
        formatMap.put(Criterion.Type.IPV6_FLABEL, new FormatIpV6FLabel());
        formatMap.put(Criterion.Type.ICMPV6_TYPE, new FormatIcmpV6Type());
        formatMap.put(Criterion.Type.ICMPV6_CODE, new FormatIcmpV6Code());
        formatMap.put(Criterion.Type.IPV6_ND_TARGET, new FormatV6NDTarget());
        formatMap.put(Criterion.Type.IPV6_ND_SLL, new FormatV6NDTll());
        formatMap.put(Criterion.Type.IPV6_ND_TLL, new FormatV6NDTll());
        formatMap.put(Criterion.Type.MPLS_LABEL, new FormatMplsLabel());
        formatMap.put(Criterion.Type.MPLS_BOS, new FormatMplsBos());
        formatMap.put(Criterion.Type.IPV6_EXTHDR, new FormatIpV6Exthdr());
        formatMap.put(Criterion.Type.OCH_SIGID, new FormatOchSigId());
        formatMap.put(Criterion.Type.OCH_SIGTYPE, new FormatOchSigType());
        formatMap.put(Criterion.Type.TUNNEL_ID, new FormatTunnelId());
        formatMap.put(Criterion.Type.DUMMY, new FormatDummyType());
        formatMap.put(Criterion.Type.ODU_SIGID, new FormatOduSignalId());
        formatMap.put(Criterion.Type.ODU_SIGTYPE, new FormatOduSignalType());
        // Currently unimplemented
        formatMap.put(Criterion.Type.ARP_OP, new FormatUnknown());
        formatMap.put(Criterion.Type.ARP_SPA, new FormatUnknown());
        formatMap.put(Criterion.Type.ARP_TPA, new FormatUnknown());
        formatMap.put(Criterion.Type.ARP_SHA, new FormatUnknown());
        formatMap.put(Criterion.Type.ARP_THA, new FormatUnknown());
        formatMap.put(Criterion.Type.MPLS_TC, new FormatUnknown());
        formatMap.put(Criterion.Type.PBB_ISID, new FormatUnknown());
        formatMap.put(Criterion.Type.UNASSIGNED_40, new FormatUnknown());
        formatMap.put(Criterion.Type.PBB_UCA, new FormatUnknown());
        formatMap.put(Criterion.Type.TCP_FLAGS, new FormatUnknown());
        formatMap.put(Criterion.Type.ACTSET_OUTPUT, new FormatUnknown());
        formatMap.put(Criterion.Type.PACKET_TYPE, new FormatUnknown());
        formatMap.put(Criterion.Type.EXTENSION, new FormatUnknown());
        formatMap.put(Criterion.Type.ETH_DST_MASKED, new FormatUnknown());
        formatMap.put(Criterion.Type.ETH_SRC_MASKED, new FormatUnknown());
        formatMap.put(Criterion.Type.TCP_SRC_MASKED, new FormatUnknown());
        formatMap.put(Criterion.Type.TCP_DST_MASKED, new FormatUnknown());
        formatMap.put(Criterion.Type.UDP_SRC_MASKED, new FormatUnknown());
        formatMap.put(Criterion.Type.UDP_DST_MASKED, new FormatUnknown());
        formatMap.put(Criterion.Type.SCTP_SRC_MASKED, new FormatUnknown());
        formatMap.put(Criterion.Type.SCTP_DST_MASKED, new FormatUnknown());
        formatMap.put(Criterion.Type.PROTOCOL_INDEPENDENT, new FormatUnknown());

    }

    private interface CriterionTypeFormatter {
        ObjectNode encodeCriterion(ObjectNode root, Criterion criterion);
    }

    private static class FormatUnknown implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            return root;
        }
    }

    private static class FormatInPort implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final PortCriterion portCriterion = (PortCriterion) criterion;
            return root.put(CriterionCodec.PORT, portCriterion.port().toLong());
        }
    }

    private static class FormatMetadata implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final MetadataCriterion metadataCriterion =
                    (MetadataCriterion) criterion;
            return root.put(CriterionCodec.METADATA, metadataCriterion.metadata());
        }
    }

    private static class FormatEth implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final EthCriterion ethCriterion = (EthCriterion) criterion;
            return root.put(CriterionCodec.MAC, ethCriterion.mac().toString());
        }
    }

    private static class FormatEthType implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final EthTypeCriterion ethTypeCriterion =
                    (EthTypeCriterion) criterion;
            return root.put(CriterionCodec.ETH_TYPE, "0x"
                    + Integer.toHexString(ethTypeCriterion.ethType().toShort() & 0xffff));
        }
    }

    private static class FormatVlanVid implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final VlanIdCriterion vlanIdCriterion =
                    (VlanIdCriterion) criterion;
            return root.put(CriterionCodec.VLAN_ID, vlanIdCriterion.vlanId().toShort());
        }
    }

    private static class FormatVlanPcp implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final VlanPcpCriterion vlanPcpCriterion =
                    (VlanPcpCriterion) criterion;
            return root.put(CriterionCodec.PRIORITY, vlanPcpCriterion.priority());
        }
    }

    private static class FormatInnerVlanVid implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final VlanIdCriterion vlanIdCriterion =
                    (VlanIdCriterion) criterion;
            return root.put(CriterionCodec.INNER_VLAN_ID, vlanIdCriterion.vlanId().toShort());
        }
    }

    private static class FormatInnerVlanPcp implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final VlanPcpCriterion vlanPcpCriterion =
                    (VlanPcpCriterion) criterion;
            return root.put(CriterionCodec.INNER_PRIORITY, vlanPcpCriterion.priority());
        }
    }

    private static class FormatIpDscp implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final IPDscpCriterion ipDscpCriterion =
                    (IPDscpCriterion) criterion;
            return root.put(CriterionCodec.IP_DSCP, ipDscpCriterion.ipDscp());
        }
    }

    private static class FormatIpEcn implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final IPEcnCriterion ipEcnCriterion =
                    (IPEcnCriterion) criterion;
            return root.put(CriterionCodec.IP_ECN, ipEcnCriterion.ipEcn());
        }
    }

    private static class FormatIpProto implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final IPProtocolCriterion iPProtocolCriterion =
                    (IPProtocolCriterion) criterion;
            return root.put(CriterionCodec.PROTOCOL, iPProtocolCriterion.protocol());
        }
    }

    private static class FormatIp implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final IPCriterion iPCriterion = (IPCriterion) criterion;
            return root.put(CriterionCodec.IP, iPCriterion.ip().toString());
        }
    }

    private static class FormatTcp implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final TcpPortCriterion tcpPortCriterion =
                    (TcpPortCriterion) criterion;
            return root.put(CriterionCodec.TCP_PORT, tcpPortCriterion.tcpPort().toInt());
        }
    }

    private static class FormatUdp implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final UdpPortCriterion udpPortCriterion =
                    (UdpPortCriterion) criterion;
            return root.put(CriterionCodec.UDP_PORT, udpPortCriterion.udpPort().toInt());
        }
    }

    private static class FormatSctp implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final SctpPortCriterion sctpPortCriterion =
                    (SctpPortCriterion) criterion;
            return root.put(CriterionCodec.SCTP_PORT, sctpPortCriterion.sctpPort().toInt());
        }
    }

    private static class FormatIcmpV4Type implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final IcmpTypeCriterion icmpTypeCriterion =
                    (IcmpTypeCriterion) criterion;
            return root.put(CriterionCodec.ICMP_TYPE, icmpTypeCriterion.icmpType());
        }
    }

    private static class FormatIcmpV4Code implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final IcmpCodeCriterion icmpCodeCriterion =
                    (IcmpCodeCriterion) criterion;
            return root.put(CriterionCodec.ICMP_CODE, icmpCodeCriterion.icmpCode());
        }
    }

    private static class FormatIpV6FLabel implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final IPv6FlowLabelCriterion ipv6FlowLabelCriterion =
                    (IPv6FlowLabelCriterion) criterion;
            return root.put(CriterionCodec.FLOW_LABEL, ipv6FlowLabelCriterion.flowLabel());
        }
    }

    private static class FormatIcmpV6Type implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final Icmpv6TypeCriterion icmpv6TypeCriterion =
                    (Icmpv6TypeCriterion) criterion;
            return root.put(CriterionCodec.ICMPV6_TYPE, icmpv6TypeCriterion.icmpv6Type());
        }
    }

    private static class FormatIcmpV6Code implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final Icmpv6CodeCriterion icmpv6CodeCriterion =
                    (Icmpv6CodeCriterion) criterion;
            return root.put(CriterionCodec.ICMPV6_CODE, icmpv6CodeCriterion.icmpv6Code());
        }
    }

    private static class FormatV6NDTarget implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final IPv6NDTargetAddressCriterion ipv6NDTargetAddressCriterion
                    = (IPv6NDTargetAddressCriterion) criterion;
            return root.put(CriterionCodec.TARGET_ADDRESS, ipv6NDTargetAddressCriterion.targetAddress().toString());
        }
    }

    private static class FormatV6NDTll implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final IPv6NDLinkLayerAddressCriterion ipv6NDLinkLayerAddressCriterion
                    = (IPv6NDLinkLayerAddressCriterion) criterion;
            return root.put(CriterionCodec.MAC, ipv6NDLinkLayerAddressCriterion.mac().toString());
        }
    }

    private static class FormatMplsLabel implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final MplsCriterion mplsCriterion =
                    (MplsCriterion) criterion;
            return root.put(CriterionCodec.LABEL, mplsCriterion.label().toInt());
        }
    }

    private static class FormatMplsBos implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final MplsBosCriterion mplsBosCriterion =
                    (MplsBosCriterion) criterion;
            return root.put(CriterionCodec.BOS, mplsBosCriterion.mplsBos());
        }
    }

    private static class FormatIpV6Exthdr implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final IPv6ExthdrFlagsCriterion exthdrCriterion =
                    (IPv6ExthdrFlagsCriterion) criterion;
            return root.put(CriterionCodec.EXT_HDR_FLAGS, exthdrCriterion.exthdrFlags());
        }
    }

    private static class FormatOchSigId implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            OchSignal ochSignal = ((OchSignalCriterion) criterion).lambda();
            ObjectNode child = root.putObject(CriterionCodec.OCH_SIGNAL_ID);

            child.put(CriterionCodec.GRID_TYPE, ochSignal.gridType().name());
            child.put(CriterionCodec.CHANNEL_SPACING, ochSignal.channelSpacing().name());
            child.put(CriterionCodec.SPACING_MULIPLIER, ochSignal.spacingMultiplier());
            child.put(CriterionCodec.SLOT_GRANULARITY, ochSignal.slotGranularity());

            return root;
        }
    }

    private static class FormatOchSigType implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final OchSignalTypeCriterion ochSignalTypeCriterion =
                    (OchSignalTypeCriterion) criterion;
            return root.put(CriterionCodec.OCH_SIGNAL_TYPE, ochSignalTypeCriterion.signalType().name());
        }
    }

    private static class FormatTunnelId implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final TunnelIdCriterion tunnelIdCriterion =
                    (TunnelIdCriterion) criterion;
            return root.put(CriterionCodec.TUNNEL_ID, tunnelIdCriterion.tunnelId());
        }
    }

    private static class FormatOduSignalId implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            OduSignalId oduSignalId = ((OduSignalIdCriterion) criterion).oduSignalId();
            ObjectNode child = root.putObject(CriterionCodec.ODU_SIGNAL_ID);

            child.put(CriterionCodec.TRIBUTARY_PORT_NUMBER, oduSignalId.tributaryPortNumber());
            child.put(CriterionCodec.TRIBUTARY_SLOT_LEN, oduSignalId.tributarySlotLength());
            child.put(CriterionCodec.TRIBUTARY_SLOT_BITMAP, HexString.toHexString(oduSignalId.tributarySlotBitmap()));

            return root;
        }
    }


    private static class FormatOduSignalType implements CriterionTypeFormatter {
        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            final OduSignalTypeCriterion oduSignalTypeCriterion =
                    (OduSignalTypeCriterion) criterion;
            return root.put(CriterionCodec.ODU_SIGNAL_TYPE, oduSignalTypeCriterion.signalType().name());
        }
    }

    private class FormatDummyType implements CriterionTypeFormatter {

        @Override
        public ObjectNode encodeCriterion(ObjectNode root, Criterion criterion) {
            checkNotNull(criterion, "Criterion cannot be null");

            return root.put(CriterionCodec.TYPE, criterion.type().toString());

        }
    }

    /**
     * Encodes a criterion into a JSON node.
     *
     * @return encoded JSON object for the given criterion
     */
    public ObjectNode encode() {
        final ObjectNode result = context.mapper().createObjectNode()
                .put(CriterionCodec.TYPE, criterion.type().toString());

        CriterionTypeFormatter formatter =
                checkNotNull(
                        formatMap.get(criterion.type()),
                        "No formatter found for criterion type "
                                + criterion.type().toString());

        return formatter.encodeCriterion(result, criterion);
    }

}
