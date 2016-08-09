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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onlab.util.HexString;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.Lambda;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;

import java.util.HashMap;
import java.util.Map;

import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Decode portion of the criterion codec.
 */
public final class DecodeCriterionCodecHelper {

    private final ObjectNode json;

    protected static final String MISSING_MEMBER_MESSAGE =
            " member is required in Criterion";

    private interface CriterionDecoder {
        Criterion decodeCriterion(ObjectNode json);
    }
    private final Map<String, CriterionDecoder> decoderMap;

    /**
     * Creates a decode criterion codec object.
     * Initializes the lookup map for criterion subclass decoders.
     *
     * @param json JSON object to decode
     */
    public DecodeCriterionCodecHelper(ObjectNode json) {
        this.json = json;
        decoderMap = new HashMap<>();

        decoderMap.put(Criterion.Type.IN_PORT.name(), new InPortDecoder());
        decoderMap.put(Criterion.Type.IN_PHY_PORT.name(), new InPhyPortDecoder());
        decoderMap.put(Criterion.Type.METADATA.name(), new MetadataDecoder());
        decoderMap.put(Criterion.Type.ETH_DST.name(), new EthDstDecoder());
        decoderMap.put(Criterion.Type.ETH_SRC.name(), new EthSrcDecoder());
        decoderMap.put(Criterion.Type.ETH_TYPE.name(), new EthTypeDecoder());
        decoderMap.put(Criterion.Type.VLAN_VID.name(), new VlanVidDecoder());
        decoderMap.put(Criterion.Type.VLAN_PCP.name(), new VlanPcpDecoder());
        decoderMap.put(Criterion.Type.INNER_VLAN_VID.name(), new InnerVlanVidDecoder());
        decoderMap.put(Criterion.Type.INNER_VLAN_PCP.name(), new InnerVlanPcpDecoder());
        decoderMap.put(Criterion.Type.IP_DSCP.name(), new IpDscpDecoder());
        decoderMap.put(Criterion.Type.IP_ECN.name(), new IpEcnDecoder());
        decoderMap.put(Criterion.Type.IP_PROTO.name(), new IpProtoDecoder());
        decoderMap.put(Criterion.Type.IPV4_SRC.name(), new IpV4SrcDecoder());
        decoderMap.put(Criterion.Type.IPV4_DST.name(), new IpV4DstDecoder());
        decoderMap.put(Criterion.Type.TCP_SRC.name(), new TcpSrcDecoder());
        decoderMap.put(Criterion.Type.TCP_DST.name(), new TcpDstDecoder());
        decoderMap.put(Criterion.Type.UDP_SRC.name(), new UdpSrcDecoder());
        decoderMap.put(Criterion.Type.UDP_DST.name(), new UdpDstDecoder());
        decoderMap.put(Criterion.Type.SCTP_SRC.name(), new SctpSrcDecoder());
        decoderMap.put(Criterion.Type.SCTP_DST.name(), new SctpDstDecoder());
        decoderMap.put(Criterion.Type.ICMPV4_TYPE.name(), new IcmpV4TypeDecoder());
        decoderMap.put(Criterion.Type.ICMPV4_CODE.name(), new IcmpV4CodeDecoder());
        decoderMap.put(Criterion.Type.IPV6_SRC.name(), new IpV6SrcDecoder());
        decoderMap.put(Criterion.Type.IPV6_DST.name(), new IpV6DstDecoder());
        decoderMap.put(Criterion.Type.IPV6_FLABEL.name(), new IpV6FLabelDecoder());
        decoderMap.put(Criterion.Type.ICMPV6_TYPE.name(), new IcmpV6TypeDecoder());
        decoderMap.put(Criterion.Type.ICMPV6_CODE.name(), new IcmpV6CodeDecoder());
        decoderMap.put(Criterion.Type.IPV6_ND_TARGET.name(), new V6NDTargetDecoder());
        decoderMap.put(Criterion.Type.IPV6_ND_SLL.name(), new V6NDSllDecoder());
        decoderMap.put(Criterion.Type.IPV6_ND_TLL.name(), new V6NDTllDecoder());
        decoderMap.put(Criterion.Type.MPLS_LABEL.name(), new MplsLabelDecoder());
        decoderMap.put(Criterion.Type.MPLS_BOS.name(), new MplsBosDecoder());
        decoderMap.put(Criterion.Type.IPV6_EXTHDR.name(), new IpV6ExthdrDecoder());
        decoderMap.put(Criterion.Type.OCH_SIGID.name(), new OchSigIdDecoder());
        decoderMap.put(Criterion.Type.OCH_SIGTYPE.name(), new OchSigTypeDecoder());
        decoderMap.put(Criterion.Type.TUNNEL_ID.name(), new TunnelIdDecoder());
        decoderMap.put(Criterion.Type.ODU_SIGID.name(), new OduSigIdDecoder());
        decoderMap.put(Criterion.Type.ODU_SIGTYPE.name(), new OduSigTypeDecoder());
    }

    private class EthTypeDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            JsonNode ethTypeNode = nullIsIllegal(json.get(CriterionCodec.ETH_TYPE),
                                              CriterionCodec.ETH_TYPE + MISSING_MEMBER_MESSAGE);
            int ethType;
            if (ethTypeNode.isInt()) {
                ethType = ethTypeNode.asInt();
            } else {
                ethType = Integer.decode(ethTypeNode.textValue());
            }
            return Criteria.matchEthType(ethType);
        }
    }
    private class EthDstDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            MacAddress mac = MacAddress.valueOf(nullIsIllegal(json.get(CriterionCodec.MAC),
                    CriterionCodec.MAC + MISSING_MEMBER_MESSAGE).asText());

            return Criteria.matchEthDst(mac);
        }
    }

    private class EthSrcDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            MacAddress mac = MacAddress.valueOf(nullIsIllegal(json.get(CriterionCodec.MAC),
                    CriterionCodec.MAC + MISSING_MEMBER_MESSAGE).asText());

            return Criteria.matchEthSrc(mac);
        }
    }

    private class InPortDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            PortNumber port = PortNumber.portNumber(nullIsIllegal(json.get(CriterionCodec.PORT),
                                                                  CriterionCodec.PORT +
                                                                          MISSING_MEMBER_MESSAGE).asLong());

            return Criteria.matchInPort(port);
        }
    }

    private class InPhyPortDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            PortNumber port = PortNumber.portNumber(nullIsIllegal(json.get(CriterionCodec.PORT),
                                                                  CriterionCodec.PORT +
                                                                          MISSING_MEMBER_MESSAGE).asLong());

            return Criteria.matchInPhyPort(port);
        }
    }

    private class MetadataDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            long metadata = nullIsIllegal(json.get(CriterionCodec.METADATA),
                    CriterionCodec.METADATA + MISSING_MEMBER_MESSAGE).asLong();

            return Criteria.matchMetadata(metadata);
        }
    }

    private class VlanVidDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            short vlanId = (short) nullIsIllegal(json.get(CriterionCodec.VLAN_ID),
                    CriterionCodec.VLAN_ID + MISSING_MEMBER_MESSAGE).asInt();

            return Criteria.matchVlanId(VlanId.vlanId(vlanId));
        }
    }

    private class VlanPcpDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            byte priority = (byte) nullIsIllegal(json.get(CriterionCodec.PRIORITY),
                    CriterionCodec.PRIORITY + MISSING_MEMBER_MESSAGE).asInt();

            return Criteria.matchVlanPcp(priority);
        }
    }

    private class InnerVlanVidDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            short vlanId = (short) nullIsIllegal(json.get(CriterionCodec.INNER_VLAN_ID),
                                                 CriterionCodec.INNER_VLAN_ID +
                                                         MISSING_MEMBER_MESSAGE).asInt();

            return Criteria.matchInnerVlanId(VlanId.vlanId(vlanId));
        }
    }

    private class InnerVlanPcpDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            byte priority = (byte) nullIsIllegal(json.get(CriterionCodec.INNER_PRIORITY),
                                                 CriterionCodec.INNER_PRIORITY +
                                                         MISSING_MEMBER_MESSAGE).asInt();

            return Criteria.matchInnerVlanPcp(priority);
        }
    }

    private class IpDscpDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            byte ipDscp = (byte) nullIsIllegal(json.get(CriterionCodec.IP_DSCP),
                    CriterionCodec.IP_DSCP + MISSING_MEMBER_MESSAGE).asInt();
            return Criteria.matchIPDscp(ipDscp);
        }
    }

    private class IpEcnDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            byte ipEcn = (byte) nullIsIllegal(json.get(CriterionCodec.IP_ECN),
                    CriterionCodec.IP_ECN + MISSING_MEMBER_MESSAGE).asInt();
            return Criteria.matchIPEcn(ipEcn);
        }
    }

    private class IpProtoDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            short proto = (short) nullIsIllegal(json.get(CriterionCodec.PROTOCOL),
                    CriterionCodec.PROTOCOL + MISSING_MEMBER_MESSAGE).asInt();
            return Criteria.matchIPProtocol(proto);
        }
    }

    private class IpV4SrcDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            String ip = nullIsIllegal(json.get(CriterionCodec.IP),
                    CriterionCodec.IP + MISSING_MEMBER_MESSAGE).asText();
            return Criteria.matchIPSrc(IpPrefix.valueOf(ip));
        }
    }

    private class IpV4DstDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            String ip = nullIsIllegal(json.get(CriterionCodec.IP),
                    CriterionCodec.IP + MISSING_MEMBER_MESSAGE).asText();
            return Criteria.matchIPDst(IpPrefix.valueOf(ip));
        }
    }

    private class IpV6SrcDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            String ip = nullIsIllegal(json.get(CriterionCodec.IP),
                    CriterionCodec.IP + MISSING_MEMBER_MESSAGE).asText();
            return Criteria.matchIPv6Src(IpPrefix.valueOf(ip));
        }
    }

    private class IpV6DstDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            String ip = nullIsIllegal(json.get(CriterionCodec.IP),
                    CriterionCodec.IP + MISSING_MEMBER_MESSAGE).asText();
            return Criteria.matchIPv6Dst(IpPrefix.valueOf(ip));
        }
    }

    private class TcpSrcDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            TpPort tcpPort = TpPort.tpPort(nullIsIllegal(json.get(CriterionCodec.TCP_PORT),
                    CriterionCodec.TCP_PORT + MISSING_MEMBER_MESSAGE).asInt());
            return Criteria.matchTcpSrc(tcpPort);
        }
    }

    private class TcpDstDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            TpPort tcpPort = TpPort.tpPort(nullIsIllegal(json.get(CriterionCodec.TCP_PORT),
                    CriterionCodec.TCP_PORT + MISSING_MEMBER_MESSAGE).asInt());
            return Criteria.matchTcpDst(tcpPort);
        }
    }

    private class UdpSrcDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            TpPort udpPort = TpPort.tpPort(nullIsIllegal(json.get(CriterionCodec.UDP_PORT),
                    CriterionCodec.UDP_PORT + MISSING_MEMBER_MESSAGE).asInt());
            return Criteria.matchUdpSrc(udpPort);
        }
    }

    private class UdpDstDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            TpPort udpPort = TpPort.tpPort(nullIsIllegal(json.get(CriterionCodec.UDP_PORT),
                    CriterionCodec.UDP_PORT + MISSING_MEMBER_MESSAGE).asInt());
            return Criteria.matchUdpDst(udpPort);
        }
    }

    private class SctpSrcDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            TpPort sctpPort = TpPort.tpPort(nullIsIllegal(json.get(CriterionCodec.SCTP_PORT),
                    CriterionCodec.SCTP_PORT + MISSING_MEMBER_MESSAGE).asInt());
            return Criteria.matchSctpSrc(sctpPort);
        }
    }

    private class SctpDstDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            TpPort sctpPort = TpPort.tpPort(nullIsIllegal(json.get(CriterionCodec.SCTP_PORT),
                    CriterionCodec.SCTP_PORT + MISSING_MEMBER_MESSAGE).asInt());
            return Criteria.matchSctpDst(sctpPort);
        }
    }

    private class IcmpV4TypeDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            short type = (short) nullIsIllegal(json.get(CriterionCodec.ICMP_TYPE),
                    CriterionCodec.ICMP_TYPE + MISSING_MEMBER_MESSAGE).asInt();
            return Criteria.matchIcmpType(type);
        }
    }

    private class IcmpV4CodeDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            short code = (short) nullIsIllegal(json.get(CriterionCodec.ICMP_CODE),
                    CriterionCodec.ICMP_CODE + MISSING_MEMBER_MESSAGE).asInt();
            return Criteria.matchIcmpCode(code);
        }
    }

    private class IpV6FLabelDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            int flowLabel = nullIsIllegal(json.get(CriterionCodec.FLOW_LABEL),
                    CriterionCodec.FLOW_LABEL + MISSING_MEMBER_MESSAGE).asInt();
            return Criteria.matchIPv6FlowLabel(flowLabel);
        }
    }

    private class IcmpV6TypeDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            short type = (short) nullIsIllegal(json.get(CriterionCodec.ICMPV6_TYPE),
                    CriterionCodec.ICMPV6_TYPE + MISSING_MEMBER_MESSAGE).asInt();
            return Criteria.matchIcmpv6Type(type);
        }
    }

    private class IcmpV6CodeDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            short code = (short) nullIsIllegal(json.get(CriterionCodec.ICMPV6_CODE),
                    CriterionCodec.ICMPV6_CODE + MISSING_MEMBER_MESSAGE).asInt();
            return Criteria.matchIcmpv6Code(code);
        }
    }

    private class V6NDTargetDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            Ip6Address target = Ip6Address.valueOf(nullIsIllegal(json.get(CriterionCodec.TARGET_ADDRESS),
                    CriterionCodec.TARGET_ADDRESS + MISSING_MEMBER_MESSAGE).asText());
            return Criteria.matchIPv6NDTargetAddress(target);
        }
    }

    private class V6NDSllDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            MacAddress mac = MacAddress.valueOf(nullIsIllegal(json.get(CriterionCodec.MAC),
                    CriterionCodec.MAC + MISSING_MEMBER_MESSAGE).asText());
            return Criteria.matchIPv6NDSourceLinkLayerAddress(mac);
        }
    }

    private class V6NDTllDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            MacAddress mac = MacAddress.valueOf(nullIsIllegal(json.get(CriterionCodec.MAC),
                    CriterionCodec.MAC + MISSING_MEMBER_MESSAGE).asText());
            return Criteria.matchIPv6NDTargetLinkLayerAddress(mac);
        }
    }

    private class MplsLabelDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            int label = nullIsIllegal(json.get(CriterionCodec.LABEL),
                    CriterionCodec.LABEL + MISSING_MEMBER_MESSAGE).asInt();
            return Criteria.matchMplsLabel(MplsLabel.mplsLabel(label));
        }
    }

    private class MplsBosDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            boolean bos = nullIsIllegal(json.get(CriterionCodec.BOS),
                    CriterionCodec.BOS + MISSING_MEMBER_MESSAGE).asBoolean();
            return Criteria.matchMplsBos(bos);
        }
    }

    private class IpV6ExthdrDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            int exthdrFlags = nullIsIllegal(json.get(CriterionCodec.EXT_HDR_FLAGS),
                    CriterionCodec.EXT_HDR_FLAGS + MISSING_MEMBER_MESSAGE).asInt();
            return Criteria.matchIPv6ExthdrFlags(exthdrFlags);
        }
    }

    private class OchSigIdDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            JsonNode ochSignalId = nullIsIllegal(json.get(CriterionCodec.OCH_SIGNAL_ID),
                    CriterionCodec.GRID_TYPE + MISSING_MEMBER_MESSAGE);
            GridType gridType =
                    GridType.valueOf(
                            nullIsIllegal(ochSignalId.get(CriterionCodec.GRID_TYPE),
                            CriterionCodec.GRID_TYPE + MISSING_MEMBER_MESSAGE).asText());
            ChannelSpacing channelSpacing =
                    ChannelSpacing.valueOf(
                            nullIsIllegal(ochSignalId.get(CriterionCodec.CHANNEL_SPACING),
                            CriterionCodec.CHANNEL_SPACING + MISSING_MEMBER_MESSAGE).asText());
            int spacingMultiplier = nullIsIllegal(ochSignalId.get(CriterionCodec.SPACING_MULIPLIER),
                    CriterionCodec.SPACING_MULIPLIER + MISSING_MEMBER_MESSAGE).asInt();
            int slotGranularity = nullIsIllegal(ochSignalId.get(CriterionCodec.SLOT_GRANULARITY),
                    CriterionCodec.SLOT_GRANULARITY + MISSING_MEMBER_MESSAGE).asInt();
            return Criteria.matchLambda(
                    Lambda.ochSignal(gridType, channelSpacing,
                            spacingMultiplier, slotGranularity));
        }
    }

    private class OchSigTypeDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            OchSignalType ochSignalType = OchSignalType.valueOf(nullIsIllegal(json.get(CriterionCodec.OCH_SIGNAL_TYPE),
                    CriterionCodec.OCH_SIGNAL_TYPE + MISSING_MEMBER_MESSAGE).asText());
            return Criteria.matchOchSignalType(ochSignalType);
        }
    }

    private class TunnelIdDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            long tunnelId = nullIsIllegal(json.get(CriterionCodec.TUNNEL_ID),
                    CriterionCodec.TUNNEL_ID + MISSING_MEMBER_MESSAGE).asLong();
            return Criteria.matchTunnelId(tunnelId);
        }
    }

    private class OduSigIdDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            JsonNode oduSignalId = nullIsIllegal(json.get(CriterionCodec.ODU_SIGNAL_ID),
                    CriterionCodec.TRIBUTARY_PORT_NUMBER + MISSING_MEMBER_MESSAGE);

            int tributaryPortNumber = nullIsIllegal(oduSignalId.get(CriterionCodec.TRIBUTARY_PORT_NUMBER),
                    CriterionCodec.TRIBUTARY_PORT_NUMBER + MISSING_MEMBER_MESSAGE).asInt();
            int tributarySlotLen = nullIsIllegal(oduSignalId.get(CriterionCodec.TRIBUTARY_SLOT_LEN),
                    CriterionCodec.TRIBUTARY_SLOT_LEN + MISSING_MEMBER_MESSAGE).asInt();
            byte[] tributarySlotBitmap = HexString.fromHexString(
                    nullIsIllegal(oduSignalId.get(CriterionCodec.TRIBUTARY_SLOT_BITMAP),
                    CriterionCodec.TRIBUTARY_SLOT_BITMAP + MISSING_MEMBER_MESSAGE).asText());

            return Criteria.matchOduSignalId(
                    OduSignalId.oduSignalId(tributaryPortNumber, tributarySlotLen, tributarySlotBitmap));
        }
    }

    private class OduSigTypeDecoder implements CriterionDecoder {
        @Override
        public Criterion decodeCriterion(ObjectNode json) {
            OduSignalType oduSignalType = OduSignalType.valueOf(nullIsIllegal(json.get(CriterionCodec.ODU_SIGNAL_TYPE),
                    CriterionCodec.ODU_SIGNAL_TYPE + MISSING_MEMBER_MESSAGE).asText());
            return Criteria.matchOduSignalType(oduSignalType);
        }
    }

    /**
     * Decodes the JSON into a criterion object.
     *
     * @return Criterion object
     * @throws IllegalArgumentException if the JSON is invalid
     */
    public Criterion decode() {
        String type =
                nullIsIllegal(json.get(CriterionCodec.TYPE), "Type not specified")
                        .asText();

        CriterionDecoder decoder = decoderMap.get(type);
        if (decoder != null) {
            return decoder.decodeCriterion(json);
        }

        throw new IllegalArgumentException("Type " + type + " is unknown");
    }


}
