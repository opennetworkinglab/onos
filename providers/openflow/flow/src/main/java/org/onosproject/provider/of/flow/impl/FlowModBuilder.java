/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.provider.of.flow.impl;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.ArpHaCriterion;
import org.onosproject.net.flow.criteria.ArpOpCriterion;
import org.onosproject.net.flow.criteria.ArpPaCriterion;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.ExtensionCriterion;
import org.onosproject.net.flow.criteria.ExtensionSelector;
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
import org.onosproject.openflow.controller.ExtensionSelectorInterpreter;
import org.onosproject.provider.of.flow.util.NoMappingFoundException;
import org.onosproject.provider.of.flow.util.OpenFlowValueMapper;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.types.ArpOpcode;
import org.projectfloodlight.openflow.types.CircuitSignalID;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.ICMPv4Code;
import org.projectfloodlight.openflow.types.ICMPv4Type;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IPv6FlowLabel;
import org.projectfloodlight.openflow.types.IpDscp;
import org.projectfloodlight.openflow.types.IpEcn;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.Masked;
import org.projectfloodlight.openflow.types.OFBooleanValue;
import org.projectfloodlight.openflow.types.OFMetadata;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.OduSignalID;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.U8;
import org.projectfloodlight.openflow.types.VlanPcp;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Builder for OpenFlow flow mods based on FlowRules.
 */
public abstract class FlowModBuilder {

    private final Logger log = getLogger(getClass());

    private final OFFactory factory;
    private final FlowRule flowRule;
    private final TrafficSelector selector;
    protected final Long xid;
    protected final Optional<DriverService> driverService;
    protected final DeviceId deviceId;

    /**
     * Creates a new flow mod builder.
     *
     * @param flowRule the flow rule to transform into a flow mod
     * @param factory the OpenFlow factory to use to build the flow mod
     * @param xid the transaction ID
     * @param driverService the device driver service
     * @return the new flow mod builder
     */
    public static FlowModBuilder builder(FlowRule flowRule,
                                         OFFactory factory,
                                         Optional<Long> xid,
                                         Optional<DriverService> driverService) {
        switch (factory.getVersion()) {
        case OF_10:
            return new FlowModBuilderVer10(flowRule, factory, xid, driverService);
        case OF_13:
            return new FlowModBuilderVer13(flowRule, factory, xid, driverService);
        default:
            throw new UnsupportedOperationException(
                    "No flow mod builder for protocol version " + factory.getVersion());
        }
    }

    /**
     * Constructs a flow mod builder.
     *
     * @param flowRule the flow rule to transform into a flow mod
     * @param factory the OpenFlow factory to use to build the flow mod
     * @param driverService the device driver service
     * @param xid the transaction ID
     */
    protected FlowModBuilder(FlowRule flowRule, OFFactory factory, Optional<Long> xid,
                             Optional<DriverService> driverService) {
        this.factory = factory;
        this.flowRule = flowRule;
        this.selector = flowRule.selector();
        this.xid = xid.orElse(0L);
        this.driverService = driverService;
        this.deviceId = flowRule.deviceId();
    }

    /**
     * Builds an ADD flow mod.
     *
     * @return the flow mod
     */
    public abstract OFFlowMod buildFlowAdd();

    /**
     * Builds a MODIFY flow mod.
     *
     * @return the flow mod
     */
    public abstract OFFlowMod buildFlowMod();

    /**
     * Builds a DELETE flow mod.
     *
     * @return the flow mod
     */
    public abstract OFFlowMod buildFlowDel();

    /**
     * Builds the match for the flow mod.
     *
     * @return the match
     */
    // CHECKSTYLE IGNORE MethodLength FOR NEXT 300 LINES
    protected Match buildMatch() {
        Match.Builder mBuilder = factory.buildMatch();
        Ip6Address ip6Address;
        Ip4Prefix ip4Prefix;
        Ip6Prefix ip6Prefix;
        EthCriterion ethCriterion;
        IPCriterion ipCriterion;
        TcpPortCriterion tcpPortCriterion;
        UdpPortCriterion udpPortCriterion;
        SctpPortCriterion sctpPortCriterion;
        IPv6NDLinkLayerAddressCriterion llAddressCriterion;
        ArpHaCriterion arpHaCriterion;
        ArpPaCriterion arpPaCriterion;

        for (Criterion c : selector.criteria()) {
            switch (c.type()) {
            case IN_PORT:
                PortCriterion inPort = (PortCriterion) c;
                mBuilder.setExact(MatchField.IN_PORT,
                                  OFPort.of((int) inPort.port().toLong()));
                break;
            case IN_PHY_PORT:
                PortCriterion inPhyPort = (PortCriterion) c;
                mBuilder.setExact(MatchField.IN_PORT,
                                  OFPort.of((int) inPhyPort.port().toLong()));
                break;
            case METADATA:
                MetadataCriterion metadata = (MetadataCriterion) c;
                mBuilder.setExact(MatchField.METADATA,
                                  OFMetadata.ofRaw(metadata.metadata()));
                break;
            case ETH_DST:
                ethCriterion = (EthCriterion) c;
                mBuilder.setExact(MatchField.ETH_DST,
                                  MacAddress.of(ethCriterion.mac().toLong()));
                break;
            case ETH_DST_MASKED:
                ethCriterion = (EthCriterion) c;
                mBuilder.setMasked(MatchField.ETH_DST,
                                   MacAddress.of(ethCriterion.mac().toLong()),
                                   MacAddress.of(ethCriterion.mask().toLong()));
                break;
            case ETH_SRC:
                ethCriterion = (EthCriterion) c;
                mBuilder.setExact(MatchField.ETH_SRC,
                                  MacAddress.of(ethCriterion.mac().toLong()));
                break;
            case ETH_SRC_MASKED:
                ethCriterion = (EthCriterion) c;
                mBuilder.setMasked(MatchField.ETH_SRC,
                                   MacAddress.of(ethCriterion.mac().toLong()),
                                   MacAddress.of(ethCriterion.mask().toLong()));
                break;
            case ETH_TYPE:
                EthTypeCriterion ethType = (EthTypeCriterion) c;
                mBuilder.setExact(MatchField.ETH_TYPE, EthType.of(ethType.ethType().toShort()));
                break;
            case VLAN_VID:
                VlanIdCriterion vid = (VlanIdCriterion) c;

                if (vid.vlanId().equals(VlanId.ANY)) {
                    mBuilder.setMasked(MatchField.VLAN_VID, OFVlanVidMatch.PRESENT,
                                       OFVlanVidMatch.PRESENT);
                } else if (vid.vlanId().equals(VlanId.NONE)) {
                    mBuilder.setExact(MatchField.VLAN_VID, OFVlanVidMatch.NONE);
                } else {
                    mBuilder.setExact(MatchField.VLAN_VID,
                            OFVlanVidMatch.ofVlanVid(VlanVid.ofVlan(vid.vlanId().toShort())));
                }
                break;
            case VLAN_PCP:
                VlanPcpCriterion vpcp = (VlanPcpCriterion) c;
                mBuilder.setExact(MatchField.VLAN_PCP, VlanPcp.of(vpcp.priority()));
                break;
            case IP_DSCP:
                IPDscpCriterion ipDscpCriterion = (IPDscpCriterion) c;
                mBuilder.setExact(MatchField.IP_DSCP,
                                  IpDscp.of(ipDscpCriterion.ipDscp()));
                break;
            case IP_ECN:
                IPEcnCriterion ipEcnCriterion = (IPEcnCriterion) c;
                mBuilder.setExact(MatchField.IP_ECN,
                                  IpEcn.of(ipEcnCriterion.ipEcn()));
                break;
            case IP_PROTO:
                IPProtocolCriterion p = (IPProtocolCriterion) c;
                mBuilder.setExact(MatchField.IP_PROTO, IpProtocol.of(p.protocol()));
                break;
            case IPV4_SRC: {
                ipCriterion = (IPCriterion) c;
                ip4Prefix = ipCriterion.ip().getIp4Prefix();
                Ip4Address maskAddr = null;

                if (ipCriterion.getIpv4SuffixLength() != null) {
                    maskAddr = Ip4Address.makeMaskSuffix(ipCriterion.getIpv4SuffixLength());
                } else if (ip4Prefix.prefixLength() != Ip4Prefix.MAX_MASK_LENGTH) {
                    maskAddr = Ip4Address.makeMaskPrefix(ip4Prefix.prefixLength());
                }

                if (maskAddr != null) {
                    Masked<IPv4Address> maskedIp =
                            Masked.of(IPv4Address.of(ip4Prefix.address().toInt()),
                                    IPv4Address.of(maskAddr.toInt()));
                    mBuilder.setMasked(MatchField.IPV4_SRC, maskedIp);
                } else {
                    mBuilder.setExact(MatchField.IPV4_SRC,
                            IPv4Address.of(ip4Prefix.address().toInt()));
                }
                break;
            }
            case IPV4_DST:
                ipCriterion = (IPCriterion) c;
                ip4Prefix = ipCriterion.ip().getIp4Prefix();
                if (ip4Prefix.prefixLength() != Ip4Prefix.MAX_MASK_LENGTH) {
                    Ip4Address maskAddr =
                        Ip4Address.makeMaskPrefix(ip4Prefix.prefixLength());
                    Masked<IPv4Address> maskedIp =
                        Masked.of(IPv4Address.of(ip4Prefix.address().toInt()),
                                  IPv4Address.of(maskAddr.toInt()));
                    mBuilder.setMasked(MatchField.IPV4_DST, maskedIp);
                } else {
                    mBuilder.setExact(MatchField.IPV4_DST,
                                IPv4Address.of(ip4Prefix.address().toInt()));
                }
                break;
            case TCP_SRC:
                tcpPortCriterion = (TcpPortCriterion) c;
                mBuilder.setExact(MatchField.TCP_SRC,
                                  TransportPort.of(tcpPortCriterion.tcpPort().toInt()));
                break;
            case TCP_DST:
                tcpPortCriterion = (TcpPortCriterion) c;
                mBuilder.setExact(MatchField.TCP_DST,
                                  TransportPort.of(tcpPortCriterion.tcpPort().toInt()));
                break;
            case UDP_SRC:
                udpPortCriterion = (UdpPortCriterion) c;
                mBuilder.setExact(MatchField.UDP_SRC,
                                  TransportPort.of(udpPortCriterion.udpPort().toInt()));
                break;
            case UDP_DST:
                udpPortCriterion = (UdpPortCriterion) c;
                mBuilder.setExact(MatchField.UDP_DST,
                                  TransportPort.of(udpPortCriterion.udpPort().toInt()));
                break;
            case SCTP_SRC:
                sctpPortCriterion = (SctpPortCriterion) c;
                mBuilder.setExact(MatchField.SCTP_SRC,
                                  TransportPort.of(sctpPortCriterion.sctpPort().toInt()));
                break;
            case SCTP_DST:
                sctpPortCriterion = (SctpPortCriterion) c;
                mBuilder.setExact(MatchField.SCTP_DST,
                                  TransportPort.of(sctpPortCriterion.sctpPort().toInt()));
                break;
            case ICMPV4_TYPE:
                IcmpTypeCriterion icmpType = (IcmpTypeCriterion) c;
                mBuilder.setExact(MatchField.ICMPV4_TYPE,
                                  ICMPv4Type.of(icmpType.icmpType()));
                break;
            case ICMPV4_CODE:
                IcmpCodeCriterion icmpCode = (IcmpCodeCriterion) c;
                mBuilder.setExact(MatchField.ICMPV4_CODE,
                                  ICMPv4Code.of(icmpCode.icmpCode()));
                break;
            case IPV6_SRC:
                ipCriterion = (IPCriterion) c;
                ip6Prefix = ipCriterion.ip().getIp6Prefix();
                if (ip6Prefix.prefixLength() != Ip6Prefix.MAX_MASK_LENGTH) {
                    Ip6Address maskAddr =
                            Ip6Address.makeMaskPrefix(ip6Prefix.prefixLength());
                    Masked<IPv6Address> maskedIp =
                            Masked.of(IPv6Address.of(ip6Prefix.address().toString()),
                                    IPv6Address.of(maskAddr.toString()));
                    mBuilder.setMasked(MatchField.IPV6_SRC, maskedIp);
                } else {
                    mBuilder.setExact(MatchField.IPV6_SRC,
                            IPv6Address.of(ip6Prefix.address().toString()));
                }
                break;
            case IPV6_DST:
                ipCriterion = (IPCriterion) c;
                ip6Prefix = ipCriterion.ip().getIp6Prefix();
                if (ip6Prefix.prefixLength() != Ip6Prefix.MAX_MASK_LENGTH) {
                    Ip6Address maskAddr =
                            Ip6Address.makeMaskPrefix(ip6Prefix.prefixLength());
                    Masked<IPv6Address> maskedIp =
                            Masked.of(IPv6Address.of(ip6Prefix.address().toString()),
                                    IPv6Address.of(maskAddr.toString()));
                    mBuilder.setMasked(MatchField.IPV6_DST, maskedIp);
                } else {
                    mBuilder.setExact(MatchField.IPV6_DST,
                            IPv6Address.of(ip6Prefix.address().toString()));
                }
                break;
            case IPV6_FLABEL:
                IPv6FlowLabelCriterion flowLabelCriterion =
                    (IPv6FlowLabelCriterion) c;
                mBuilder.setExact(MatchField.IPV6_FLABEL,
                                  IPv6FlowLabel.of(flowLabelCriterion.flowLabel()));
                break;
            case ICMPV6_TYPE:
                Icmpv6TypeCriterion icmpv6Type = (Icmpv6TypeCriterion) c;
                mBuilder.setExact(MatchField.ICMPV6_TYPE,
                                  U8.of(icmpv6Type.icmpv6Type()));
                break;
            case ICMPV6_CODE:
                Icmpv6CodeCriterion icmpv6Code = (Icmpv6CodeCriterion) c;
                mBuilder.setExact(MatchField.ICMPV6_CODE,
                                  U8.of(icmpv6Code.icmpv6Code()));
                break;
            case IPV6_ND_TARGET:
                IPv6NDTargetAddressCriterion targetAddressCriterion =
                    (IPv6NDTargetAddressCriterion) c;
                ip6Address = targetAddressCriterion.targetAddress();
                mBuilder.setExact(MatchField.IPV6_ND_TARGET,
                                  IPv6Address.of(ip6Address.toOctets()));
                break;
            case IPV6_ND_SLL:
                llAddressCriterion =
                    (IPv6NDLinkLayerAddressCriterion) c;
                mBuilder.setExact(MatchField.IPV6_ND_SLL,
                        MacAddress.of(llAddressCriterion.mac().toLong()));
                break;
            case IPV6_ND_TLL:
                llAddressCriterion =
                    (IPv6NDLinkLayerAddressCriterion) c;
                mBuilder.setExact(MatchField.IPV6_ND_TLL,
                        MacAddress.of(llAddressCriterion.mac().toLong()));
                break;
            case MPLS_LABEL:
                MplsCriterion mp = (MplsCriterion) c;
                mBuilder.setExact(MatchField.MPLS_LABEL, U32.of(mp.label().toInt()));
                break;
            case IPV6_EXTHDR:
                IPv6ExthdrFlagsCriterion exthdrFlagsCriterion =
                    (IPv6ExthdrFlagsCriterion) c;
                mBuilder.setExact(MatchField.IPV6_EXTHDR,
                                  U16.of(exthdrFlagsCriterion.exthdrFlags()));
                break;
            case OCH_SIGID:
                try {
                    OchSignalCriterion ochSignalCriterion = (OchSignalCriterion) c;
                    OchSignal signal = ochSignalCriterion.lambda();
                    byte gridType = OpenFlowValueMapper.lookupGridType(signal.gridType());
                    byte channelSpacing = OpenFlowValueMapper.lookupChannelSpacing(signal.channelSpacing());
                    mBuilder.setExact(MatchField.EXP_OCH_SIG_ID,
                            new CircuitSignalID(gridType, channelSpacing,
                                    (short) signal.spacingMultiplier(), (short) signal.slotGranularity()));
                } catch (NoMappingFoundException e) {
                    log.warn(e.getMessage());
                }
                break;
            case OCH_SIGTYPE:
                try {
                    OchSignalTypeCriterion sc = (OchSignalTypeCriterion) c;
                    byte signalType = OpenFlowValueMapper.lookupOchSignalType(sc.signalType());
                    mBuilder.setExact(MatchField.EXP_OCH_SIGTYPE, U8.of(signalType));
                } catch (NoMappingFoundException e) {
                    log.warn(e.getMessage());
                }
                break;
            case ODU_SIGID:
                OduSignalIdCriterion oduSignalIdCriterion = (OduSignalIdCriterion) c;
                OduSignalId oduSignalId = oduSignalIdCriterion.oduSignalId();
                mBuilder.setExact(MatchField.EXP_ODU_SIG_ID,
                        new OduSignalID((short) oduSignalId.tributaryPortNumber(),
                                (short) oduSignalId.tributarySlotLength(),
                                oduSignalId.tributarySlotBitmap()));
                break;
            case ODU_SIGTYPE:
                try {
                    OduSignalTypeCriterion oduSignalTypeCriterion = (OduSignalTypeCriterion) c;
                    byte oduSigType = OpenFlowValueMapper.lookupOduSignalType(oduSignalTypeCriterion.signalType());
                    mBuilder.setExact(MatchField.EXP_ODU_SIGTYPE, U8.of(oduSigType));
                } catch (NoMappingFoundException e) {
                    log.warn(e.getMessage());
                }
                break;
            case TUNNEL_ID:
                TunnelIdCriterion tunnelId = (TunnelIdCriterion) c;
                mBuilder.setExact(MatchField.TUNNEL_ID,
                                  U64.of(tunnelId.tunnelId()));
                break;
            case MPLS_BOS:
                MplsBosCriterion mplsBos = (MplsBosCriterion) c;
                mBuilder.setExact(MatchField.MPLS_BOS,
                                  mplsBos.mplsBos() ? OFBooleanValue.TRUE
                                                    : OFBooleanValue.FALSE);
                break;
            case ARP_OP:
                ArpOpCriterion arpOp = (ArpOpCriterion) c;
                mBuilder.setExact(MatchField.ARP_OP,
                                  ArpOpcode.of(arpOp.arpOp()));
                break;
            case ARP_SHA:
                arpHaCriterion = (ArpHaCriterion) c;
                mBuilder.setExact(MatchField.ARP_SHA,
                                  MacAddress.of(arpHaCriterion.mac().toLong()));
                break;
            case ARP_SPA:
                arpPaCriterion = (ArpPaCriterion) c;
                mBuilder.setExact(MatchField.ARP_SPA,
                                  IPv4Address.of(arpPaCriterion.ip().toInt()));
                break;
            case ARP_THA:
                arpHaCriterion = (ArpHaCriterion) c;
                mBuilder.setExact(MatchField.ARP_THA,
                                  MacAddress.of(arpHaCriterion.mac().toLong()));
                break;
            case ARP_TPA:
                arpPaCriterion = (ArpPaCriterion) c;
                mBuilder.setExact(MatchField.ARP_TPA,
                                  IPv4Address.of(arpPaCriterion.ip().toInt()));
                break;
            case EXTENSION:
                ExtensionCriterion extensionCriterion = (ExtensionCriterion) c;
                OFOxm oxm = buildExtensionOxm(extensionCriterion.extensionSelector());
                if (oxm == null) {
                    log.warn("Unable to build extension selector");
                    break;
                }

                if (oxm.isMasked()) {
                    mBuilder.setMasked(oxm.getMatchField(), oxm.getValue(), oxm.getMask());
                } else {
                    mBuilder.setExact(oxm.getMatchField(), oxm.getValue());
                }

                break;
            case MPLS_TC:
            case PBB_ISID:
                // TODO: need to implement PBB-ISID case when OpenFlowJ is ready
            default:
                log.warn("Match type {} not yet implemented.", c.type());
            }
        }
        return mBuilder.build();
    }

    /**
     * Returns the flow rule for this builder.
     *
     * @return the flow rule
     */
    protected FlowRule flowRule() {
        return flowRule;
    }

    /**
     * Returns the factory used for building OpenFlow constructs.
     *
     * @return the factory
     */
    protected OFFactory factory() {
        return factory;
    }

    private OFOxm buildExtensionOxm(ExtensionSelector extension) {
        if (!driverService.isPresent()) {
            log.error("No driver service present");
            return null;
        }
        Driver driver = driverService.get().getDriver(deviceId);
        if (driver.hasBehaviour(ExtensionSelectorInterpreter.class)) {
            DefaultDriverHandler handler =
                    new DefaultDriverHandler(new DefaultDriverData(driver, deviceId));
            ExtensionSelectorInterpreter interpreter = handler.behaviour(ExtensionSelectorInterpreter.class);

            return interpreter.mapSelector(factory(), extension);
        }

        return null;
    }

}
