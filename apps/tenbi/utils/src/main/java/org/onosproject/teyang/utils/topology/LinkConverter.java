/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.teyang.utils.topology;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.List;

import org.onlab.packet.Ip4Address;
import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.TeStatus;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.TeTopologyService;
import org.onosproject.tetopology.management.api.link.CommonLinkData;
import org.onosproject.tetopology.management.api.link.DefaultNetworkLink;
import org.onosproject.tetopology.management.api.link.DefaultTeLink;
import org.onosproject.tetopology.management.api.link.ExternalLink;
import org.onosproject.tetopology.management.api.link.LinkBandwidth;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.link.PathElement;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.link.TeLinkTpKey;
import org.onosproject.tetopology.management.api.link.TePathAttributes;
import org.onosproject.tetopology.management.api.link.TeTunnelId;
import org.onosproject.tetopology.management.api.link.TunnelProtectionType;
import org.onosproject.tetopology.management.api.link.UnderlayBackupPath;
import org.onosproject.tetopology.management.api.link.UnderlayPath;
import org.onosproject.tetopology.management.api.node.NodeTpKey;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TerminationPointKey;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NetworkId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.Networks;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NodeId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.networks.Network;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.networks.network.Node;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.LinkId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.TpId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.ietfnetworktopology.networks.network.AugmentedNdNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.augmentedndnetwork.DefaultLink;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.augmentedndnetwork.DefaultLink.LinkBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.augmentedndnetwork.Link;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.augmentedndnetwork.link.DefaultDestination;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.augmentedndnetwork.link.DefaultSource;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.augmentedndnetwork.link.DefaultSupportingLink;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
                .ietfnetworktopology.networks.network.augmentedndnetwork.link.Destination
                .DestinationBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.augmentedndnetwork.link.Source.SourceBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.augmentedndnetwork.link.SupportingLink;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.augmentedndnetwork.link.SupportingLink
               .SupportingLinkBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.ietfnetworktopology.networks.network.node.AugmentedNdNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.DefaultTeLinkEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.TeLinkEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.informationsourceattributes.DefaultInformationSourceState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.informationsourceattributes.InformationSourceState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.informationsourceattributes.informationsourcestate.DefaultTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.informationsourceattributes.informationsourcestate.Topology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.networks.network.AugmentedNwNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.AugmentedNtLink;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.DefaultAugmentedNtLink;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.DefaultAugmentedNtLink.AugmentedNtLinkBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.networks.network.node.AugmentedNwNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.networks.network.node.terminationpoint.AugmentedNtTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkaugment.DefaultTe;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkaugment.DefaultTe.TeBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkaugment.te.Config;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkaugment.te.DefaultConfig;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkaugment.te.DefaultConfig.ConfigBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkaugment.te.DefaultState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkaugment.te.DefaultState.StateBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkaugment.te.State;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkconfigattributes.DefaultTeLinkAttributes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkconfigattributes.DefaultTeLinkAttributes.TeLinkAttributesBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkconfigattributes.TeLinkAttributes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkconfigattributes.telinkattributes.DefaultExternalDomain;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkconfigattributes.telinkattributes.DefaultExternalDomain.ExternalDomainBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkconfigattributes.telinkattributes.DefaultUnderlay;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkconfigattributes.telinkattributes.DefaultUnderlay.UnderlayBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkconfigattributes.telinkattributes.ExternalDomain;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkconnectivityattributes.DefaultTeSrlgs;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkconnectivityattributes.DefaultTeSrlgs.TeSrlgsBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkconnectivityattributes.DefaultUnreservedBandwidth;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkconnectivityattributes.DefaultUnreservedBandwidth.UnreservedBandwidthBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeaugment.Te;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkconnectivityattributes.TeSrlgs;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkconnectivityattributes.UnreservedBandwidth;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkinfoattributes.LinkProtectionTypeEnum;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkunderlayattributes.DefaultUnderlayBackupPath;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkunderlayattributes.DefaultUnderlayBackupPath.UnderlayBackupPathBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkunderlayattributes.DefaultUnderlayPrimaryPath;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkunderlayattributes.DefaultUnderlayPrimaryPath.UnderlayPrimaryPathBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkunderlayattributes.UnderlayPrimaryPath;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.telinkunderlayattributes.underlayprimarypath.DefaultPathElement;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.Srlg;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeNodeId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeOperStatus;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeTopologyEventType;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeTpId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.tetopologyeventtype.TeTopologyEventTypeEnum;

import com.google.common.collect.Lists;


/**
 * The conversion functions.
 */
public final class LinkConverter {
    private static final String E_NULL_TELINK_UNDERLAY_PATH =
            "TeSubsystem link underlayPath object cannot be null";
    private static final String E_NULL_TELINK_DATA =
            "TeSubsystem teLinkAttrBuilder data cannot be null";
    private static final String E_NULL_TELINK =
            "TeSubsystem teLink object cannot be null";
    private static final String E_NULL_YANG_TELINK_CONFIG =
            "YANG telink config object cannot be null";
    private static final String E_NULL_YANG_TELINK =
            "YANG Link object cannot be null";

    // no instantiation
    private LinkConverter() {
    }

    private static LinkProtectionTypeEnum teSubsystem2YangLinkProtectionType(TunnelProtectionType linkProtectionType) {
        switch (linkProtectionType) {
        case ENHANCED:
            return LinkProtectionTypeEnum.ENHANCED;
        case EXTRA_TRAFFIC:
            return LinkProtectionTypeEnum.EXTRA_TRAFFIC;
        case SHARED:
            return LinkProtectionTypeEnum.SHARED;
        case UNPROTECTED:
            return LinkProtectionTypeEnum.UNPROTECTED;
        case ONE_FOR_ONE:
            return LinkProtectionTypeEnum.YANGAUTOPREFIX1_FOR_1;
        case ONE_PLUS_ONE:
            return LinkProtectionTypeEnum.YANGAUTOPREFIX1_PLUS_1;
        default:
            return null;
        }
    }
    /**
     * TE Link Config object conversion from TE Topology subsystem to YANG.
     *
     * @param teLink TE link object
     * @return TE Link Config YANG object
     */
    private static Config teLink2YangConfig(TeLink teLink) {
        checkNotNull(teLink, E_NULL_TELINK_DATA);

        TeLinkAttributesBuilder attrBuilder = DefaultTeLinkAttributes.builder();

        if (teLink.teLinkKey() != null) {
            // what is link index? for now I used teLinkTpId
            attrBuilder = attrBuilder.linkIndex(BigInteger.valueOf(teLink.teLinkKey().teLinkTpId()));
        }

        if (teLink.adminStatus() != null) {
            attrBuilder = attrBuilder.adminStatus(EnumConverter.teSubsystem2YangAdminStatus(teLink.adminStatus()));
        }

        if (teLink.tunnelProtectionType() != null) {
            attrBuilder = attrBuilder
                    .linkProtectionType(teSubsystem2YangLinkProtectionType(teLink
                            .tunnelProtectionType()));
        }

        attrBuilder = attrBuilder.teDefaultMetric(teLink.cost());

        if (teLink.srlgs() != null) {
            TeSrlgsBuilder teSrlgsBuilder = DefaultTeSrlgs.builder();
            for (Long srlgLongVal : teLink.srlgs()) {
                teSrlgsBuilder = teSrlgsBuilder.addToValue(new Srlg(srlgLongVal));
            }
            attrBuilder = attrBuilder.teSrlgs(teSrlgsBuilder.build());
        }
        attrBuilder = attrBuilder.isAbstract(teLink.flags().get(TeNode.BIT_ABSTRACT));

        if (teLink.externalLink() != null) {
            ExternalDomainBuilder edBuilder =
                    DefaultExternalDomain.builder();
            if (teLink.externalLink().plugId() != null) {
                edBuilder.plugId(teLink.externalLink().plugId());
            }
            if (teLink.externalLink().externalLinkKey() != null) {
                edBuilder = edBuilder
                        .remoteTeLinkTpId(TeTpId.fromString(
                                                                String.valueOf(teLink
                                                                        .externalLink()
                                                                        .externalLinkKey()
                                                                        .teLinkTpId())))
                        .remoteTeNodeId(TeNodeId.fromString(
                                                                String.valueOf(teLink
                                                                        .externalLink()
                                                                        .externalLinkKey()
                                                                        .teNodeId())));
            }
            attrBuilder = attrBuilder.externalDomain(edBuilder.build());
        }

        if (teLink.availBandwidth() != null) {
            short i = 0;
            for (float f : teLink.availBandwidth()) {
                UnreservedBandwidthBuilder urBuilder =
                        DefaultUnreservedBandwidth.builder()
                                                  .bandwidth(BigDecimal.valueOf(f))
                                                  .priority(i);
                i++;
                attrBuilder = attrBuilder.addToUnreservedBandwidth(urBuilder.build());
            }
        }

        if (teLink.maxBandwidth() != null) {
            // maxBandwidth is an array of float[], but in yang is just a big decimal
            attrBuilder = attrBuilder.maxLinkBandwidth(BigDecimal.valueOf(teLink.maxBandwidth()[0]));
        }
        // FIXME: how to retrieve maxResvLinkBandwidth from teLink
//        if (teLink.maxResvLinkBandwidth() != null) {
//            attrBuilder = attrBuilder.maxResvLinkBandwidth(teLink.maxResvLinkBandwidth());
//        }

        UnderlayBuilder builder = DefaultUnderlay.builder();
        if (teLink.primaryPath() != null) {

            // TODO: what is underlayProtectionType in tePath?
            // builder = builder.underlayProtectionType(tePath.protectionType());

            builder = te2YangConfigUnderlayPrimaryPath(builder, teLink);
        }

        if (teLink.backupPaths() != null) {
            builder = te2YangConfigUnderlayBackupPaths(builder, teLink);
        }

        attrBuilder = attrBuilder.underlay(builder.build());

        ConfigBuilder yangConfigBuilder = DefaultConfig.builder()
                                                       .teLinkAttributes(attrBuilder.build());
        return yangConfigBuilder.build();
    }

    /**
     * TE Link State object conversion from TE Topology subsystem to YANG.
     *
     * @param teLink TE link object
     * @return TE Link State YANG object
     */
    private static State teLink2YangState(TeLink teLink) {
        TeLinkAttributes
            .TeLinkAttributesBuilder attrBuilder =
                        DefaultTeLinkAttributes
            .builder()
            .teDefaultMetric(teLink.cost())
            .isAbstract(teLink.flags().get(TeLink.BIT_ABSTRACT));
        if (teLink.teLinkKey() != null) {
            // what is link index? for now I used teLinkTpId
            attrBuilder = attrBuilder.linkIndex(BigInteger.valueOf(teLink.teLinkKey().teLinkTpId()));
        }

        if (teLink.adminStatus() != null) {
            attrBuilder = attrBuilder.adminStatus(EnumConverter.teSubsystem2YangAdminStatus(teLink.adminStatus()));
        }
        if (teLink.tunnelProtectionType() != null) {
            attrBuilder = attrBuilder
                    .linkProtectionType(teSubsystem2YangLinkProtectionType(teLink
                            .tunnelProtectionType()));
        }
        // FIXME: maxBandwidth stuff are array[] in TE but not in yang...
//        if (teLink.maxLinkBandwidth() != null) {
//            attrBuilder = attrBuilder.maxLinkBandwidth(teLink.maxLinkBandwidth());
//        }
//        if (teLink.maxResvLinkBandwidth() != null) {
//            attrBuilder = attrBuilder.maxResvLinkBandwidth(teLink.maxResvLinkBandwidth());
//        }
        if (teLink.srlgs() != null) {
            TeSrlgs.TeSrlgsBuilder srlgsBuilder = DefaultTeSrlgs.builder();
            for (Long srlgLongVal : teLink.srlgs()) {
                srlgsBuilder = srlgsBuilder.addToValue(new Srlg(srlgLongVal));
            }
            attrBuilder = attrBuilder.teSrlgs(srlgsBuilder.build());
        }

        if (teLink.externalLink() != null) {
            ExternalDomain.ExternalDomainBuilder edBuilder = DefaultExternalDomain
                    .builder();
            if (teLink.externalLink().plugId() != null) {
                edBuilder = edBuilder.plugId(teLink.externalLink().plugId());
            }
            if (teLink.externalLink().externalLinkKey() != null) {
                edBuilder = edBuilder
                        .remoteTeLinkTpId(TeTpId.fromString(String.valueOf(teLink
                                                                           .externalLink()
                                                                           .externalLinkKey()
                                                                           .teLinkTpId())))
                        .remoteTeNodeId(TeNodeId.fromString(String.valueOf(teLink
                                                                           .externalLink()
                                                                           .externalLinkKey()
                                                                           .teNodeId())));
            }
            attrBuilder = attrBuilder.externalDomain(edBuilder.build());

        }

        if (teLink.availBandwidth() != null) {
            short i = 0;
            for (float f : teLink.availBandwidth()) {
                UnreservedBandwidth.UnreservedBandwidthBuilder urBuilder = DefaultUnreservedBandwidth
                        .builder()
                                               .bandwidth(BigDecimal.valueOf(f))
                                               .priority(i);
                i++;
                attrBuilder = attrBuilder.addToUnreservedBandwidth(urBuilder.build());
            }
        }

        StateBuilder yangStateBuilder = DefaultState.builder()
                                                    .teLinkAttributes(attrBuilder.build());
        if (teLink.opStatus() != null) {
            yangStateBuilder = yangStateBuilder.operStatus(EnumConverter
                                                           .teSubsystem2YangOperStatus(teLink.opStatus()));
        }

        if (teLink.sourceTeLinkId() != null) {
            InformationSourceState.InformationSourceStateBuilder issBuilder = DefaultInformationSourceState.builder();

            Topology.TopologyBuilder topologyBuilder = DefaultTopology.builder();
            topologyBuilder = topologyBuilder.clientIdRef(teLink.sourceTeLinkId().clientId())
                                             .providerIdRef(teLink.sourceTeLinkId().providerId())
                                             .teTopologyIdRef(teLink.sourceTeLinkId().topologyId());
            issBuilder = issBuilder.topology(topologyBuilder.build());
            yangStateBuilder.informationSourceState(issBuilder.build());
        }

        // Once stateDerived underlay is available in yang and core TE Topology
        // object model, set the value properly
        // stateDerivedUnderlay = org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
        // .rev20160708.ietftetopology.telinkstatederived.Underlay
        // yangStateBuilder = yangStateBuilder.underlay(stateDerivedUnderlay);

        return yangStateBuilder.build();
    }

    /**
     * Link object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsNetworkLink TE subsystem link object
     * @return YANG link object
     */
    public static Link teSubsystem2YangLink(
            org.onosproject.tetopology.management.api.link.NetworkLink teSubsNetworkLink) {
        checkNotNull(teSubsNetworkLink, E_NULL_TELINK);

        LinkId linkId = LinkId.fromString(teSubsNetworkLink.linkId().toString());
        LinkBuilder builder = DefaultLink.builder().linkId(linkId);
        if (teSubsNetworkLink.supportingLinkIds() != null) {
            List<SupportingLink> slinks = Lists.newArrayList();
            SupportingLinkBuilder spLinkBuilder = DefaultSupportingLink.builder();
            for (NetworkLinkKey linkKey : teSubsNetworkLink.supportingLinkIds()) {
                slinks.add(spLinkBuilder.networkRef(NetworkId.fromString(
                                                    linkKey.networkId().toString()))
                                        .linkRef(LinkId.fromString(
                                                    linkKey.linkId().toString()))
                                        .build());
            }
            builder = builder.supportingLink(slinks);
        }
        if (teSubsNetworkLink.source() != null) {
            SourceBuilder sourceBuilder = DefaultSource
                                              .builder()
                                              .sourceNode(NodeId.fromString(
                                                   teSubsNetworkLink.source().nodeId().toString()))
                                              .sourceTp(TpId.fromString(
                                                   teSubsNetworkLink.source().tpId().toString()));
            builder = builder.source(sourceBuilder.build());
        }
        if (teSubsNetworkLink.destination() != null) {
            DestinationBuilder destBuilder = DefaultDestination
                                                 .builder()
                                                 .destNode(NodeId.fromString(
                                                      teSubsNetworkLink.destination().nodeId().toString()))
                                                 .destTp(TpId.fromString(
                                                      teSubsNetworkLink.destination().tpId().toString()));
            builder = builder.destination(destBuilder.build());
        }

        if (teSubsNetworkLink.teLink() != null) {
            TeLink teData = teSubsNetworkLink.teLink();
            TeBuilder yangTeBuilder = DefaultTe.builder()
                                               .config(teLink2YangConfig(teData))
                                               .state(teLink2YangState(teData));
            // ignoring supportingTeLinkId when converting from core to yang?
            // if (teData.supportingTeLinkId() != null) {
            // }
            AugmentedNtLinkBuilder linkAugmentBuilder =
                    DefaultAugmentedNtLink.builder()
                                          .te(yangTeBuilder.build());
            builder.addYangAugmentedInfo(linkAugmentBuilder.build(), AugmentedNtLink.class);
        }

        return builder.build();
    }

    private static UnderlayBuilder te2YangConfigUnderlayPrimaryPath(
            UnderlayBuilder yangBuilder,
            TeLink teLink) {

        org.onosproject.tetopology.management.api.link.UnderlayPrimaryPath tePath = teLink.primaryPath();

        UnderlayPrimaryPathBuilder pathBuilder =
                                       DefaultUnderlayPrimaryPath.builder();
        if (tePath.pathElements() != null) {
            for (PathElement pathElementTe : tePath.pathElements()) {
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                .ietftetopology.telinkunderlayattributes.underlayprimarypath.PathElement
                   .PathElementBuilder pathElementYangBuilder = DefaultPathElement.builder();

                pathElementYangBuilder = pathElementYangBuilder.pathElementId(pathElementTe.pathElementId());
                //TODO: add some Type cross converter
//              pathElementYangBuilder = pathElementYangBuilder.type(Type pathElementTe.type())

                pathBuilder = pathBuilder.addToPathElement(pathElementYangBuilder.build());
            }
        }

        pathBuilder = pathBuilder.clientIdRef(teLink.underlayTeTopologyId().clientId())
                                 .providerIdRef(teLink.underlayTeTopologyId().providerId())
                                 .teTopologyIdRef(teLink.underlayTeTopologyId().topologyId());

        // TODO: get networkRefId from service
//        pathBuilder = pathBuilder.networkIdRef(networkIdRef);

        return yangBuilder.underlayPrimaryPath(pathBuilder.build());
    }

    private static UnderlayBuilder te2YangConfigUnderlayBackupPaths(UnderlayBuilder yangBuilder,
                                                                    TeLink teLink) {
        List<UnderlayBackupPath> tePaths = teLink.backupPaths();

        for (UnderlayBackupPath tePath : tePaths) {
            UnderlayBackupPathBuilder pathBuilder = DefaultUnderlayBackupPath
                    .builder();
            pathBuilder = pathBuilder.index(tePath.index());
            pathBuilder = pathBuilder.clientIdRef(teLink.underlayTeTopologyId().clientId())
                                     .providerIdRef(teLink.underlayTeTopologyId().providerId())
                                     .teTopologyIdRef(teLink.underlayTeTopologyId().topologyId());
            // TODO: find networkIdRef from the service
//            pathBuilder = pathBuilder.networkIdRef(networkIdRef);

            for (PathElement backupPathElementTe : tePath.pathElements()) {
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                .ietftetopology.telinkunderlayattributes.underlaybackuppath.PathElement
                .PathElementBuilder elementBuilder =
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                .ietftetopology.telinkunderlayattributes.underlaybackuppath.DefaultPathElement
                        .builder();

                elementBuilder = elementBuilder.pathElementId(backupPathElementTe.pathElementId());
                // TODO: add some Type cross conversion
//                elementBuilder.type(type);

                pathBuilder = pathBuilder
                        .addToPathElement(elementBuilder.build());
            }
            yangBuilder = yangBuilder
                    .addToUnderlayBackupPath(pathBuilder.build());
        }

        return yangBuilder;
    }

    private static TeLink yang2TeLinkAttributes(TeLinkAttributes yangLinkAttr,
                                                State opState, Link yangLink,
                                                Network yangNetwork,
                                                Networks yangNetworks) {
        TeNodeId teNodeId = findTeNodeId(yangNetwork,
                                         yangLink.source().sourceNode(),
                                         yangLink.source().sourceTp());
        long teNodeIdLong = Ip4Address.valueOf(teNodeId.dottedQuad().string()).toInt();

        TeLinkTpKey teLinkKey = new TeLinkTpKey(teNodeIdLong,
                                                findTeTpId(yangNetwork,
                                                           yangLink.source().sourceNode(),
                                                           yangLink.source().sourceTp()));

        TeNodeId teNodeIdDest = findTeNodeId(yangNetwork,
                                             yangLink.destination().destNode(),
                                             yangLink.destination().destTp());
        long teNodeIdDestLong = Ip4Address.valueOf(teNodeIdDest.dottedQuad().string()).toInt();

        TeLinkTpKey peerTeLinkKey = new TeLinkTpKey(teNodeIdDestLong,
                                                    findTeTpId(yangNetwork,
                                                               yangLink.destination().destNode(),
                                                               yangLink.destination().destTp()));

        TeLinkTpGlobalKey supportTeLinkId = findSupportTeLinkId(yangNetworks, yangLink);

        org.onosproject.tetopology.management.api.TeStatus opStatus = null;
        if (opState != null && opState.operStatus() != null) {
            opStatus = EnumConverter.yang2TeSubsystemOpStatus(opState.operStatus());
        }


        TeLink teLink = yangLinkAttr2TeLinkAttributes(yangLinkAttr, opStatus, teNodeIdLong, teNodeIdDestLong, teLinkKey,
                                                      peerTeLinkKey, supportTeLinkId);

        return teLink;
    }

    private static TeLink yangLinkAttr2TeLinkAttributes(TeLinkAttributes yangLinkAtrr,
                                                        org.onosproject.tetopology.management.api.TeStatus opStatus,
                                                        long teNodeIdLong,
                                                        long teNodeIdDestLong,
                                                        TeLinkTpKey teLinkKey,
                                                        TeLinkTpKey peerTeLinkKey,
                                                        TeLinkTpGlobalKey supportTeLinkId) {
        org.onosproject.tetopology.management.api.TeStatus adminStatus = null;

        TeLinkTpGlobalKey sourceTeLinkId = null; // from yang to core, we can ignore sourceTeLinkId
        TeTopologyKey underlayTopologyId = null;
        CommonLinkData teData = null;

        underlayTopologyId = new TeTopologyKey((long) yangLinkAtrr.underlay().underlayPrimaryPath().providerIdRef(),
                                               (long) yangLinkAtrr.underlay().underlayPrimaryPath().clientIdRef(),
                                               (long) yangLinkAtrr.underlay().underlayPrimaryPath().teTopologyIdRef());

        if (yangLinkAtrr.adminStatus() != null) {
            adminStatus = EnumConverter.yang2TeSubsystemAdminStatus(yangLinkAtrr.adminStatus());
        }

        BitSet flags = new BitSet();
        if (yangLinkAtrr.isAbstract()) {
            flags.set(TeLink.BIT_ABSTRACT);
        }

        ExternalLink externalLink = new ExternalLink(null, yangLinkAtrr.externalDomain().plugId());

        UnderlayPath underlayPath = null;
        underlayPath = yang2TeSubsystemUnderlayPath(yangLinkAtrr, teNodeIdLong,
                                                    teNodeIdDestLong);
        Long adminGroup = Long.valueOf(yangLinkAtrr.administrativeGroup().toString());
        List<Long> interLayerLocks = null; // FIXME: how to find interLayerLocks?

        List<UnreservedBandwidth> listOfUnreservedBandwidth = yangLinkAtrr.unreservedBandwidth();
        float[] availBandwidth = new float[listOfUnreservedBandwidth.size()];
        int i = 0;
        for (UnreservedBandwidth urbw : listOfUnreservedBandwidth) {
            availBandwidth[i] = urbw.bandwidth().floatValue();
            i++;
        }

        float[] maxBandwidth = new float[1];
        maxBandwidth[0] = yangLinkAtrr.maxLinkBandwidth().floatValue();

        float[] maxAvailLspBandwidth = null; // FIXME: how to find this?
        float[] minAvailLspBandwidth = null; // FIXME: how to find this?
        LinkBandwidth bandwidth = new LinkBandwidth(
                maxBandwidth,
                availBandwidth,
                maxAvailLspBandwidth,
                minAvailLspBandwidth,
                null); // FIXME: how to find odu resource?
        List<Long> srlgs = Lists.newArrayList();
        for (Srlg srlg : yangLinkAtrr.teSrlgs().value()) {
            srlgs.add(srlg.uint32());
        }
        TePathAttributes teAttributes =
                new TePathAttributes(yangLinkAtrr.teDefaultMetric(),
                                     yangLinkAtrr.performanceMetric().measurement().unidirectionalDelay(),
                                     srlgs);
        teData = new CommonLinkData(adminStatus,
                                    opStatus,
                                    flags,
                                    null, // switchingLayer
                                    null, // encodingLayer
                                    externalLink,
                                    underlayPath,
                                    teAttributes,
                                    adminGroup,
                                    interLayerLocks,
                                    bandwidth);

        TeLink te = new DefaultTeLink(teLinkKey, peerTeLinkKey,
                                      underlayTopologyId, supportTeLinkId,
                                      sourceTeLinkId, teData);
        return te;
    }

    private static TeLinkTpGlobalKey findSupportTeLinkId(Networks yangNetworks,
                                            Link yangLink) {
        Network teNetworkFound = null;
        LinkId teLinkIdFound = null;
        for (Network ynetItem : yangNetworks.network()) {
            if (ynetItem.networkId().equals(yangLink.supportingLink().get(0).networkRef())) {
                teNetworkFound = ynetItem;
                teLinkIdFound = yangLink.supportingLink().get(0).linkRef();
                break;
            }
        }
        Link teLinkFound = null;
        if (teNetworkFound.yangAugmentedInfo(AugmentedNdNetwork.class) != null) {
            AugmentedNdNetwork augmentLink =
                    (AugmentedNdNetwork) teNetworkFound.yangAugmentedInfo(AugmentedNdNetwork.class);
            for (Link link : augmentLink.link()) {
                if (link.linkId().equals(teLinkIdFound)) {
                    teLinkFound = link;
                    break;
                }
            }
        }

        TeNodeId teSupportNodeId = findTeNodeId(teNetworkFound,
                                                teLinkFound.source().sourceNode(),
                                                teLinkFound.source().sourceTp());
        long tenIdLong = Ip4Address.valueOf(teSupportNodeId.dottedQuad().string()).toInt();
        long teSupportLinkTpId = findTeTpId(teNetworkFound,
                                            teLinkFound.source().sourceNode(),
                                            teLinkFound.source().sourceTp());

        org.onosproject.tetopology.management.api.TeTopologyId teTopologyId = null;
        if (teNetworkFound.yangAugmentedInfo(AugmentedNwNetwork.class) != null) {
            AugmentedNwNetwork augmentTeIds =
                    (AugmentedNwNetwork) teNetworkFound.yangAugmentedInfo(AugmentedNwNetwork.class);
            teTopologyId =
                    new org.onosproject.tetopology.management.api.TeTopologyId(
                            augmentTeIds.te().clientId().uint32(),
                            augmentTeIds.te().providerId().uint32(),
                            augmentTeIds.te().teTopologyId().string());
        }

        TeLinkTpGlobalKey supportTeLinkId = new TeLinkTpGlobalKey(teTopologyId.providerId(),
                                                teTopologyId.clientId(),
                                                Long.valueOf(teTopologyId
                                                        .topologyId()),
                                                tenIdLong, teSupportLinkTpId);

        return supportTeLinkId;
    }

    private static TeNodeId findTeNodeId(Network yangNetwork, NodeId yangNodeId, TpId yangTpId) {
        TeNodeId teNodeId = null;
        for (Node node : yangNetwork.node()) {
            if (node.nodeId().equals(yangNodeId)) {
                if (node.yangAugmentedInfoMap() != null
                        && !node.yangAugmentedInfoMap().isEmpty()) {
                    AugmentedNwNode yangNodeAugment = (AugmentedNwNode) node
                            .yangAugmentedInfo(AugmentedNwNode.class);
                    if (yangNodeAugment != null && yangNodeAugment.te() != null
                            && yangNodeAugment.te().teNodeId() != null) {
                        Te yangNodeAugTe = yangNodeAugment.te();
                        teNodeId = yangNodeAugTe.teNodeId();
                    }
                }
            }
        }
        return teNodeId;
    }

    private static long findTeTpId(Network yangNetwork, NodeId yangNodeId, TpId yangTpId) {
        long teTpId = 0;
        for (Node node : yangNetwork.node()) {
            if (node.nodeId().equals(yangNodeId)) {
                if (node.yangAugmentedInfoMap() != null
                        && !node.yangAugmentedInfoMap().isEmpty()) {

                    AugmentedNdNode yangTpNodeAugment = (AugmentedNdNode) node
                            .yangAugmentedInfo(AugmentedNdNode.class);
                    if (yangTpNodeAugment.terminationPoint() != null) {
                        for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                                .rev20151208.ietfnetworktopology.networks.network.node.augmentedndnode.TerminationPoint
                                yangTpnode : yangTpNodeAugment.terminationPoint()) {
                            if (yangTpnode.tpId().equals(yangTpId)) {
                                if (yangTpnode.yangAugmentedInfoMap() != null
                                        && !yangTpnode.yangAugmentedInfoMap()
                                                .isEmpty()) {
                                    AugmentedNtTerminationPoint yangTpAugment =
                                            (AugmentedNtTerminationPoint) yangTpnode
                                                    .yangAugmentedInfo(AugmentedNtTerminationPoint.class);
                                    if (yangTpAugment.te() != null && yangTpAugment.te().teTpId() != null) {
                                        teTpId = Long.valueOf(yangTpAugment.te().teTpId().toString());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return teTpId;
    }
    /**
     * Link object conversion from YANG to TE Topology subsystem.
     *
     * @param yangLink  YANG link
     * @param yangNetwork YANG network
     * @param yangNetworks YANG networks
     * @return TE subsystem link
     */
    public static org.onosproject.tetopology.management.api.link.NetworkLink
            yang2TeSubsystemLink(Link yangLink, Network yangNetwork,
                                 Networks yangNetworks) {
        NetworkId networkId = yangNetwork.networkId();
        checkNotNull(yangLink, E_NULL_YANG_TELINK);

        KeyId linkId = KeyId.keyId(yangLink.linkId().uri().toString());
        NodeTpKey sourceNodeTpKey = null;
        NodeTpKey destinationNodeTpKey = null;
        List<NetworkLinkKey> spLinkIds = null;
        TeLink te = null;

        if (yangLink.supportingLink() != null) {
            spLinkIds = Lists.newArrayList();
            for (SupportingLink yangSpLink : yangLink.supportingLink()) {
                NetworkLinkKey linkKey = new NetworkLinkKey(KeyId.keyId(yangSpLink.networkRef().uri().toString()),
                                                            KeyId.keyId(yangSpLink.linkRef().uri().toString()));
                spLinkIds.add(linkKey);
            }
        }

        if (yangLink.source() != null) {
            TerminationPointKey source = new TerminationPointKey(
                                                 KeyId.keyId(networkId.uri().toString()),
                                                 KeyId.keyId(yangLink.source().sourceNode().uri().toString()),
                                                 KeyId.keyId(yangLink.source().sourceTp().uri().toString()));
            sourceNodeTpKey = new NodeTpKey(source.nodeId(), source.tpId());
        }

        if (yangLink.destination() != null) {
            TerminationPointKey destination = new TerminationPointKey(
                                                      KeyId.keyId(networkId.uri().toString()),
                                                      KeyId.keyId(yangLink.destination().destNode().uri().toString()),
                                                      KeyId.keyId(yangLink.destination().destTp().uri().toString()));
            destinationNodeTpKey = new NodeTpKey(destination.nodeId(), destination.tpId());
        }

        if (yangLink.yangAugmentedInfoMap() != null && !yangLink.yangAugmentedInfoMap().isEmpty()) {

            AugmentedNtLink yangLinkAugment =
                    (AugmentedNtLink) yangLink.yangAugmentedInfo(AugmentedNtLink.class);
            if (yangLinkAugment != null &&
                    yangLinkAugment.te() != null &&
                    yangLinkAugment.te().config() != null) {
                TeLinkAttributes yangLinkAtrr =
                        yangLinkAugment.te().config().teLinkAttributes();
                if (yangLinkAtrr != null && yangLinkAtrr.linkIndex() != null) {
                    te = yang2TeLinkAttributes(yangLinkAtrr,
                                               yangLinkAugment.te().state(),
                                               yangLink, yangNetwork,
                                               yangNetworks);
                }
            }
        }

        org.onosproject.tetopology.management.api.link.DefaultNetworkLink link =
                new DefaultNetworkLink(linkId,
                                       sourceNodeTpKey,
                                       destinationNodeTpKey,
                                       spLinkIds,
                                       te);
        return link;
    }

    private static org.onosproject.tetopology.management.api.link.UnderlayPrimaryPath
                    yang2TeSubsystemUnderlayPrimaryPath(UnderlayPrimaryPath yangpath,
                                                        long teNodeId) {
        org.onosproject.tetopology.management.api.link.UnderlayPrimaryPath teUnderlayPrimaryPath = null;

        List<PathElement> pathElementList = Lists.newArrayList();
        for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology
                .telinkunderlayattributes.underlayprimarypath.
                PathElement pathElementConfigYang : yangpath.pathElement()) {

            // TODO: find the proper type (ElementType) from pathElementConfigYang.type()
            PathElement tePathElement = new PathElement(pathElementConfigYang.pathElementId(),
                                                        teNodeId,
                                                        null,
                                                        false); // FIXME: how to find the proper value for loose?
            pathElementList.add(tePathElement);
        }

        teUnderlayPrimaryPath = new org.onosproject.tetopology.management.api.link.
                UnderlayPrimaryPath(pathElementList, false); // FIXME: how to find the proper value for loose?

        return teUnderlayPrimaryPath;
    }

    private static List<UnderlayBackupPath>
       yang2TeSubsystemUnderlayBackupPaths(
                List<org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.
                    rev20160708.ietftetopology.telinkunderlayattributes.
                    UnderlayBackupPath> yangpaths,
                long teNodeId) {

        List<UnderlayBackupPath> underlayBackupPathsList = Lists.newArrayList();
        for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                .ietftetopology.telinkunderlayattributes.
                UnderlayBackupPath yangConfig : yangpaths) {
            UnderlayBackupPath ubp = null;
            List<PathElement> backupPathElementList = Lists.newArrayList();
            for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology
                    .telinkunderlayattributes.underlaybackuppath.
                    PathElement pathElementBackupYang : yangConfig.pathElement()) {

                PathElement tePathElementBackup = new PathElement(pathElementBackupYang.pathElementId(),
                                                                  teNodeId,
                                                                  null, // FIXME: find the proper ElementType
                                                                        // from pathElementBackupYang.type()
                                                                  false); // FIXME: find the proper value for loose
                backupPathElementList.add(tePathElementBackup);
            }
            ubp = new UnderlayBackupPath(yangConfig.index(),
                                         backupPathElementList,
                                         null); // FIXME: find the proper value for loose
            underlayBackupPathsList.add(ubp);
        }

        return underlayBackupPathsList;
    }

    /**
    * TE Link underlay path Config object conversion from YANG to TE Topology subsystem.
    *
    * @param yangLinkAtrr YANG link Config YANG object
    * @return teSubsystem TE underlay path object
    */
    private static org.onosproject.tetopology.management.api.link.UnderlayPath
               yang2TeSubsystemUnderlayPath(TeLinkAttributes yangLinkAtrr, long srcTeNodeId, long dstTeNodeId) {

        checkNotNull(yangLinkAtrr, E_NULL_YANG_TELINK_CONFIG);

        org.onosproject.tetopology.management.api.link.UnderlayPrimaryPath underlayPrimaryPath = null;
        List<UnderlayBackupPath> underlayBackupPathsList = null;
        TeTunnelId teTunnelId = new TeTunnelId(srcTeNodeId, dstTeNodeId, 0); // FIXME: how to find teTunnelId?

        if (yangLinkAtrr.underlay().underlayPrimaryPath() != null) {
            underlayPrimaryPath =
                    yang2TeSubsystemUnderlayPrimaryPath(yangLinkAtrr.underlay().underlayPrimaryPath(), srcTeNodeId);
        }

        if (yangLinkAtrr.underlay().underlayBackupPath() != null) {
            underlayBackupPathsList =
                    yang2TeSubsystemUnderlayBackupPaths(yangLinkAtrr.underlay().underlayBackupPath(), srcTeNodeId);
        }

        org.onosproject.tetopology.management.api.link.UnderlayPath teUnderlay =
                    new org.onosproject.tetopology.management.api.link.
                    UnderlayPath(underlayPrimaryPath,
                                 underlayBackupPathsList,
                                 TunnelProtectionType.of(yangLinkAtrr.underlay().underlayProtectionType()),
                                 (long) yangLinkAtrr.underlay().underlayTrailSrc().tpRef(), // Is this correct?
                                 (long) yangLinkAtrr.underlay().underlayTrailDes().tpRef(), // Is this correct?
                                 teTunnelId);

        return teUnderlay;
    }

    /**
     * Converts a TE Topology link event from the data format used in
     * the core to its corresponding YANG Object (YO) format.
     *
     * @param eventType Link event type
     * @param linkData  TE Topology link event data
     * @return YANG Object converted from linkData
     */
    public static TeLinkEvent teNetworkLink2yangTeLinkEvent(TeTopologyEventTypeEnum eventType,
                                                            NetworkLink linkData) {
        TeLinkEvent.TeLinkEventBuilder builder = new DefaultTeLinkEvent.TeLinkEventBuilder();

        TeTopologyEventType yangEventType = new TeTopologyEventType(eventType);
        builder.eventType(yangEventType);
        LinkId linkId = LinkId.fromString(linkData.linkId().toString());
        builder.linkRef(linkId);

        TeLinkAttributes teLinkAttributes = teLink2YangConfig(linkData.teLink()).teLinkAttributes();
        builder.teLinkAttributes(teLinkAttributes);

        return builder.build();
    }

    public static NetworkLinkKey yangLinkEvent2NetworkLinkKey(TeLinkEvent yangLinkEvent) {
        //TODO: Implementation will be submitted as a separate review
        NetworkId networkRef = (NetworkId) (yangLinkEvent.networkRef());
        LinkId linkRef = (LinkId) (yangLinkEvent.linkRef());
        KeyId networkId = KeyId.keyId(networkRef.uri().toString());
        KeyId linkId = KeyId.keyId(linkRef.uri().toString());

        NetworkLinkKey networkLinkKey = new NetworkLinkKey(networkId, linkId);

        return networkLinkKey;
    }

    public static NetworkLink yangLinkEvent2NetworkLink(TeLinkEvent yangLinkEvent,
                                                        TeTopologyService teTopologyService) {

        KeyId linkId = yangLinkEvent2NetworkLinkKey(yangLinkEvent).linkId();

        org.onosproject.tetopology.management.api.
                Network network = teTopologyService.network(
                yangLinkEvent2NetworkLinkKey(yangLinkEvent).networkId());
        if (network == null) {
            return null;
        }

        NetworkLink networkLink = network.link(linkId);
        if (networkLink == null) {
            return null;
        }

        NodeTpKey sourceTp = networkLink.source();
        NodeTpKey destTp = networkLink.destination();

        List<NetworkLinkKey> supportingLinkIds = networkLink.supportingLinkIds();
        TeLink teLink = networkLink.teLink();
        if (teLink == null) {
            return null;
        }

        TeOperStatus opState = yangLinkEvent.operStatus();
        org.onosproject.tetopology.management.api.
        TeStatus opStatus = EnumConverter.yang2TeSubsystemOpStatus(opState);

        TeLink updatedTeLink = yangLinkEvent2TeLinkAttributes(yangLinkEvent, teLink, opStatus);


        NetworkLink updatedNetworkLink = new DefaultNetworkLink(linkId, sourceTp, destTp, supportingLinkIds,
                                                                updatedTeLink);

        return updatedNetworkLink;
    }

    private static TeLink yangLinkEvent2TeLinkAttributes(TeLinkEvent yangLinkEvent, TeLink oldTeLink, TeStatus
            opStatus) {

        TeLinkAttributes yangTeLinkAttrs = yangLinkEvent.teLinkAttributes();

        TeLinkTpKey teLinkKey = oldTeLink.teLinkKey();


        long teNodeIdDest = oldTeLink.peerTeLinkKey().teNodeId();
        long teNodeId = oldTeLink.teLinkKey().teNodeId();
        TeLinkTpGlobalKey supportTeLinkId = oldTeLink.supportingTeLinkId();
        TeLinkTpKey peerTeLinkKey = oldTeLink.peerTeLinkKey();

        TeLink updatedTeLink = yangLinkAttr2TeLinkAttributes(yangTeLinkAttrs, opStatus, teNodeId, teNodeIdDest,
                                                             teLinkKey, peerTeLinkKey, supportTeLinkId);

        return updatedTeLink;
    }
}
