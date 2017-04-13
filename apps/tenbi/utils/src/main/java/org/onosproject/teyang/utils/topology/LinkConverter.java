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
import static org.onosproject.tetopology.management.api.TeConstants.MAX_PRIORITY;

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
import org.onosproject.tetopology.management.api.link.ElementType;
import org.onosproject.tetopology.management.api.link.ExternalLink;
import org.onosproject.tetopology.management.api.link.LinkBandwidth;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkEventSubject;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.link.PathElement;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.TeLinkId;
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
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev20130715.ietfinettypes.IpAddress;
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
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.DefaultTeLinkEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.TeBandwidth;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.TeLinkEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.informationsourceperlinkattributes.DefaultInformationSourceState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.informationsourceperlinkattributes.InformationSourceState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.informationsourceperlinkattributes.informationsourcestate.DefaultTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.informationsourceperlinkattributes.informationsourcestate.Topology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.networks.network.AugmentedNwNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
               .ietftetopology.networks.network.link.AugmentedNtLink;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
               .ietftetopology.networks.network.link.DefaultAugmentedNtLink;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
               .ietftetopology.networks.network.link.DefaultAugmentedNtLink.AugmentedNtLinkBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.networks.network.node.AugmentedNwNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.networks.network.node.terminationpoint.AugmentedNtTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkaugment.DefaultTe;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkaugment.DefaultTe.TeBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkaugment.te.Config;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkaugment.te.DefaultConfig;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkaugment.te.DefaultConfig.ConfigBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkaugment.te.DefaultState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkaugment.te.DefaultState.StateBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkaugment.te.State;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconfigattributes.DefaultTeLinkAttributes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconfigattributes.DefaultTeLinkAttributes.TeLinkAttributesBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconfigattributes.TeLinkAttributes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconfigattributes.telinkattributes.DefaultExternalDomain;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconfigattributes.telinkattributes.DefaultExternalDomain.ExternalDomainBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconfigattributes.telinkattributes.DefaultUnderlay;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconfigattributes.telinkattributes.DefaultUnderlay.UnderlayBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconfigattributes.telinkattributes.ExternalDomain;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconnectivityattributes.DefaultTeSrlgs;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconnectivityattributes.DefaultTeSrlgs.TeSrlgsBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconnectivityattributes.DefaultUnreservedBandwidth;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconnectivityattributes.DefaultUnreservedBandwidth.UnreservedBandwidthBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconnectivityattributes.TeSrlgs;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconnectivityattributes.UnreservedBandwidth;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkinfoattributes.LinkProtectionTypeEnum;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkunderlayattributes.DefaultBackupPath;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkunderlayattributes.DefaultBackupPath.BackupPathBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkunderlayattributes.DefaultPrimaryPath;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkunderlayattributes.DefaultPrimaryPath.PrimaryPathBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkunderlayattributes.PrimaryPath;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkunderlayattributes.primarypath.DefaultPathElement;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.Srlg;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeNodeId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeOperStatus;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeTopologyEventType;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeTpId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.explicitroutesubobject.type.DefaultUnnumberedLink;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.explicitroutesubobject.type.DefaultUnnumberedLink.UnnumberedLinkBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.explicitroutesubobject.type.UnnumberedLink;
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
    private static Config teLink2YangConfig(TeLink teLink, TeTopologyService teTopologyService) {
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
            for (short i = 0; i < teLink.availBandwidth().length; i++) {
                UnreservedBandwidthBuilder urBuilder =
                        DefaultUnreservedBandwidth.builder()
                                .bandwidth(TeBandwidth.fromString(String
                                        .valueOf(teLink.availBandwidth()[i])))
                                                  .priority(i);
                attrBuilder = attrBuilder.addToUnreservedBandwidth(urBuilder.build());
            }
        }

        if (teLink.maxBandwidth() != null) {
            // maxBandwidth is an array of float[], but in yang is just a big decimal
            attrBuilder = attrBuilder.maxLinkBandwidth(TeBandwidth
                    .fromString(String.valueOf(teLink.maxBandwidth()[0])));
        }
        // FIXME: how to retrieve maxResvLinkBandwidth from teLink
//        if (teLink.maxResvLinkBandwidth() != null) {
//            attrBuilder = attrBuilder.maxResvLinkBandwidth(teLink.maxResvLinkBandwidth());
//        }

        if (teLink.primaryPath() != null || teLink.backupPaths() != null) {
            UnderlayBuilder builder = DefaultUnderlay.builder();
            if (teLink.primaryPath() != null) {
                // TODO: what is underlayProtectionType in tePath?
                // builder =
                // builder.underlayProtectionType(tePath.protectionType());
                builder = te2YangConfigUnderlayPrimaryPath(builder, teLink,
                                                           teTopologyService);
            }

            if (teLink.backupPaths() != null) {
                builder = te2YangConfigUnderlayBackupPaths(builder, teLink,
                                                           teTopologyService);
            }

            attrBuilder = attrBuilder.underlay(builder.build());
        }


        ConfigBuilder yangConfigBuilder = DefaultConfig.builder()
                                                       .teLinkAttributes(attrBuilder.build());
        return yangConfigBuilder.build();
    }

    /**
     * TE Link State object conversion from TE Topology subsystem to YANG.
     *
     * @param teLink TE link object
     * @param teTopologyService TE Topology Service object
     * @return TE Link State YANG object
     */
    private static State teLink2YangState(TeLink teLink, TeTopologyService teTopologyService) {
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
        if (teLink.maxBandwidth() != null) {
            // maxBandwidth is an array of float[], but in yang is just a big decimal
            attrBuilder = attrBuilder.maxLinkBandwidth(TeBandwidth
                    .fromString(String.valueOf(teLink.maxBandwidth()[0])));
        }
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
                        .bandwidth(TeBandwidth.fromString(String.valueOf(f)))
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
            topologyBuilder = topologyBuilder
                    .linkRef(teTopologyService.linkKey(teLink.sourceTeLinkId())
                            .linkId())
                    .networkRef(teTopologyService
                            .linkKey(teLink.sourceTeLinkId()).networkId());

            issBuilder = issBuilder.topology(topologyBuilder.build());
            yangStateBuilder.informationSourceState(issBuilder.build());
        }

        // Once stateDerived underlay is available in yang and core TE Topology
        // object model, set the value properly
        // stateDerivedUnderlay = org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
        // .rev20170110.ietftetopology.telinkstatederived.Underlay
        //yangStateBuilder = yangStateBuilder.underlay(stateDerivedUnderlay);

        return yangStateBuilder.build();
    }

    /**
     * Link object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsNetworkLink TE subsystem link object
     * @param teTopologyService TE Topology Service object
     * @return YANG link object
     */
    public static Link teSubsystem2YangLink(
            org.onosproject.tetopology.management.api.link.NetworkLink teSubsNetworkLink,
            TeTopologyService teTopologyService) {
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
                                               .config(teLink2YangConfig(teData, teTopologyService))
                                               .state(teLink2YangState(teData, teTopologyService));

            AugmentedNtLinkBuilder linkAugmentBuilder =
                    DefaultAugmentedNtLink.builder()
                                          .te(yangTeBuilder.build());
            builder.addYangAugmentedInfo(linkAugmentBuilder.build(), AugmentedNtLink.class);
        }

        return builder.build();
    }

    private static UnderlayBuilder te2YangConfigUnderlayPrimaryPath(
            UnderlayBuilder yangBuilder,
            TeLink teLink, TeTopologyService teTopologyService) {

        org.onosproject.tetopology.management.api.link.UnderlayPrimaryPath tePath = teLink.primaryPath();

        PrimaryPathBuilder pathBuilder = DefaultPrimaryPath.builder();
        if (tePath.pathElements() != null) {
            for (PathElement pathElementTe : tePath.pathElements()) {
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
                .ietftetopology.telinkunderlayattributes.primarypath.PathElement
                   .PathElementBuilder pathElementYangBuilder = DefaultPathElement.builder();

                pathElementYangBuilder = pathElementYangBuilder.pathElementId(pathElementTe.pathElementId());
                //TODO: check more types here
                if (pathElementTe.type() instanceof TeLinkId) {
                    UnnumberedLinkBuilder unmBuilder = DefaultUnnumberedLink.builder()
                            .routerId(IpAddress.fromString(
                                             Ip4Address.valueOf((int) pathElementTe.teNodeId()).toString()))
                            .interfaceId(((TeLinkId) pathElementTe.type()).value());
                    pathElementYangBuilder = pathElementYangBuilder.type(unmBuilder.build());
                }

                pathBuilder = pathBuilder.addToPathElement(pathElementYangBuilder.build());
            }
        }

        pathBuilder = pathBuilder.networkRef(teTopologyService
                .networkId(teLink.underlayTeTopologyId()));

        return yangBuilder.primaryPath(pathBuilder.build());
    }

    private static UnderlayBuilder te2YangConfigUnderlayBackupPaths(UnderlayBuilder yangBuilder,
                                                                    TeLink teLink,
                                                                    TeTopologyService teTopologyService) {
        List<UnderlayBackupPath> tePaths = teLink.backupPaths();

        for (UnderlayBackupPath tePath : tePaths) {
            BackupPathBuilder pathBuilder = DefaultBackupPath
                    .builder();
            pathBuilder = pathBuilder.index(tePath.index());

            pathBuilder = pathBuilder.networkRef(teTopologyService
                    .networkId(teLink.underlayTeTopologyId()));

            for (PathElement backupPathElementTe : tePath.pathElements()) {
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
                .ietftetopology.telinkunderlayattributes.backuppath.PathElement
                .PathElementBuilder elementBuilder =
                                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
                .ietftetopology.telinkunderlayattributes.backuppath.DefaultPathElement
                        .builder();

                elementBuilder = elementBuilder.pathElementId(backupPathElementTe.pathElementId());
                //TODO: check more types here
                if (backupPathElementTe.type() instanceof TeLinkId) {
                    UnnumberedLinkBuilder unmBuilder = DefaultUnnumberedLink.builder()
                            .routerId(IpAddress.fromString(
                                             Ip4Address.valueOf((int) backupPathElementTe.teNodeId()).toString()))
                            .interfaceId(((TeLinkId) backupPathElementTe.type()).value());
                    elementBuilder = elementBuilder.type(unmBuilder.build());
                }

                pathBuilder = pathBuilder.addToPathElement(elementBuilder.build());
            }
            yangBuilder = yangBuilder.addToBackupPath(pathBuilder.build());
        }

        return yangBuilder;
    }

    private static TeLink yang2TeLinkAttributes(TeLinkAttributes yangLinkAttr,
                                                State opState, Link yangLink,
                                                Network yangNetwork,
                                                Networks yangNetworks) {
        TeNodeId teNodeId = findTeNodeId(yangNetwork,
                                         yangLink.source().sourceNode());
        long teNodeIdLong = -1;
        if (teNodeId != null && teNodeId.dottedQuad() != null) {
            teNodeIdLong = Ip4Address.valueOf(teNodeId.dottedQuad().string())
                    .toInt();
        }

        TeLinkTpKey teLinkKey = new TeLinkTpKey(teNodeIdLong,
                                                findTeTpId(yangNetwork,
                                                           yangLink.source().sourceNode(),
                                                           yangLink.source().sourceTp()));

        TeNodeId teNodeIdDest = null;
        if (yangLink.destination() != null) {
            teNodeIdDest = findTeNodeId(yangNetwork,
                                        yangLink.destination().destNode());
        }
        long teNodeIdDestLong = -1;
        if (teNodeIdDest != null && teNodeIdDest.dottedQuad() != null) {
            teNodeIdDestLong = Ip4Address.valueOf(teNodeIdDest.dottedQuad().string()).toInt();
        }

        TeLinkTpKey peerTeLinkKey = null;
        if (yangLink.destination() != null) {
            peerTeLinkKey = new TeLinkTpKey(teNodeIdDestLong,
                                                        findTeTpId(yangNetwork,
                                                                   yangLink.destination().destNode(),
                                                                   yangLink.destination().destTp()));
        }

        TeLinkTpGlobalKey supportTeLinkId = findSupportTeLinkId(yangNetworks, yangLink);

        org.onosproject.tetopology.management.api.TeStatus opStatus = null;
        if (opState != null && opState.operStatus() != null) {
            opStatus = EnumConverter.yang2TeSubsystemOpStatus(opState.operStatus());
        }

        TeTopologyKey underlayTopologyId = null;
        if (yangLinkAttr != null && yangLinkAttr.underlay() != null && yangLinkAttr.underlay().primaryPath() != null) {
            underlayTopologyId = findTopologyId(yangNetworks, yangLinkAttr.underlay().primaryPath().networkRef());
        }

        TeLink teLink = yangLinkAttr2TeLinkAttributes(yangLinkAttr, opStatus,
                                                      teNodeIdLong,
                                                      teNodeIdDestLong,
                                                      teLinkKey,
                                                      peerTeLinkKey,
                                                      supportTeLinkId,
                                                      underlayTopologyId);

        return teLink;
    }

    /**
     * Finds the TE TopologyKey from yangNetworks and a networkRef.
     *
     * @param yangNetworks YANG networks object
     * @param networkRef YANG network reference
     * @return TeTopologyKey the TE TopologyKey
     */
    public static TeTopologyKey findTopologyId(Networks yangNetworks, Object networkRef) {
        if (networkRef == null) {
            return null;
        }
        NetworkId networkId = NetworkId.fromString((String) networkRef);

        TeTopologyKey topologyId = null;
        Network targetTeNetwork = null;
        if (yangNetworks.network() != null
                && !yangNetworks.network().isEmpty() && networkId != null) {
            for (Network ynetItem : yangNetworks.network()) {
                if (ynetItem.networkId() != null) {
                    if (ynetItem.networkId().equals(networkId)) {
                        targetTeNetwork = ynetItem;
                        break;
                    }
                }
            }
        }
        if (targetTeNetwork != null && targetTeNetwork
                .yangAugmentedInfo(AugmentedNwNetwork.class) != null) {
            AugmentedNwNetwork augmentTeIds = (AugmentedNwNetwork) targetTeNetwork
                    .yangAugmentedInfo(AugmentedNwNetwork.class);
            topologyId = new TeTopologyKey(augmentTeIds.providerId().uint32(),
                                                   augmentTeIds.clientId().uint32(),
                                                   Long.valueOf(augmentTeIds.teTopologyId().string()));
        }
        return topologyId;
    }

    private static TeLink yangLinkAttr2TeLinkAttributes(TeLinkAttributes yangLinkAtrr,
                                                        org.onosproject.tetopology.management.api.TeStatus opStatus,
                                                        long teNodeIdLong,
                                                        long teNodeIdDestLong,
                                                        TeLinkTpKey teLinkKey,
                                                        TeLinkTpKey peerTeLinkKey,
                                                        TeLinkTpGlobalKey supportTeLinkId,
                                                        TeTopologyKey underlayTopologyId) {
        org.onosproject.tetopology.management.api.TeStatus adminStatus = null;

        TeLinkTpGlobalKey sourceTeLinkId = null; // from yang to core, we can ignore sourceTeLinkId

        CommonLinkData teData = null;

        if (yangLinkAtrr.adminStatus() != null) {
            adminStatus = EnumConverter.yang2TeSubsystemAdminStatus(yangLinkAtrr.adminStatus());
        }

        BitSet flags = new BitSet();
        if (yangLinkAtrr.isAbstract()) {
            flags.set(TeLink.BIT_ABSTRACT);
        }

        ExternalLink externalLink = null;
        if (yangLinkAtrr != null && yangLinkAtrr.externalDomain() != null) {
            externalLink = new ExternalLink(null, yangLinkAtrr.externalDomain().plugId());
        }

        UnderlayPath underlayPath = null;
        underlayPath = yang2TeSubsystemUnderlayPath(yangLinkAtrr, teNodeIdLong,
                                                    teNodeIdDestLong);
        Long adminGroup = null;
        if (yangLinkAtrr != null && yangLinkAtrr.administrativeGroup() != null) {
            adminGroup = Long.valueOf(yangLinkAtrr.administrativeGroup().toString());
        }
        List<Long> interLayerLocks = null; // FIXME: how to find interLayerLocks?

        List<UnreservedBandwidth> listOfUnreservedBandwidth = yangLinkAtrr.unreservedBandwidth();
        float[] availBandwidth = new float[MAX_PRIORITY + 1];
        for (UnreservedBandwidth urbw : listOfUnreservedBandwidth) {
            availBandwidth[urbw.priority()] = Float.valueOf(urbw.bandwidth().string());
        }

        float[] maxBandwidth = new float[MAX_PRIORITY + 1];
        if (yangLinkAtrr.maxLinkBandwidth() != null) {
            // Core TE has an array, but YANG is an integer
            for (short p = 0; p <= MAX_PRIORITY; p++) {
                maxBandwidth[p] = Float.valueOf(yangLinkAtrr.maxLinkBandwidth().string());
            }
        }

        float[] maxAvailLspBandwidth = availBandwidth; // FIXME: how to find this?
        float[] minAvailLspBandwidth = availBandwidth; // FIXME: how to find this?
        LinkBandwidth bandwidth = new LinkBandwidth(
                maxBandwidth,
                availBandwidth,
                maxAvailLspBandwidth,
                minAvailLspBandwidth,
                null); // FIXME: how to find odu resource?
        List<Long> srlgs = Lists.newArrayList();
        if (yangLinkAtrr.teSrlgs() != null
                && yangLinkAtrr.teSrlgs().value() != null
                && !yangLinkAtrr.teSrlgs().value().isEmpty()) {
            for (Srlg srlg : yangLinkAtrr.teSrlgs().value()) {
                srlgs.add(srlg.uint32());
            }
        }
        TePathAttributes teAttributes =
                new TePathAttributes(yangLinkAtrr.teDefaultMetric(), yangLinkAtrr.teDelayMetric(),
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
        TeLinkTpGlobalKey supportTeLinkId = null;

        if (yangLink != null && yangLink.supportingLink() != null && !yangLink.supportingLink().isEmpty()) {
            if (yangNetworks.network() != null && !yangNetworks.network().isEmpty()) {
                for (Network ynetItem : yangNetworks.network()) {
                    if (ynetItem.networkId() != null) {
                        if (ynetItem.networkId().equals(yangLink.supportingLink().get(0).networkRef())) {
                            teNetworkFound = ynetItem;
                            teLinkIdFound = yangLink.supportingLink().get(0).linkRef();
                            break;
                        }
                    }
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

            if (teLinkFound != null) {
                TeNodeId teSupportNodeId = findTeNodeId(teNetworkFound,
                                                    teLinkFound.source().sourceNode());
                long tenIdLong = -1;
                if (teSupportNodeId != null) {
                    tenIdLong = Ip4Address.valueOf(teSupportNodeId.dottedQuad().string()).toInt();
                }
                long teSupportLinkTpId = findTeTpId(teNetworkFound,
                                                    teLinkFound.source().sourceNode(),
                                                    teLinkFound.source().sourceTp());

                org.onosproject.tetopology.management.api.TeTopologyId teTopologyId = null;
                if (teNetworkFound.yangAugmentedInfo(AugmentedNwNetwork.class) != null) {
                    AugmentedNwNetwork augmentTeIds =
                            (AugmentedNwNetwork) teNetworkFound.yangAugmentedInfo(AugmentedNwNetwork.class);
                    teTopologyId =
                            new org.onosproject.tetopology.management.api.TeTopologyId(
                                    augmentTeIds.clientId().uint32(),
                                    augmentTeIds.providerId().uint32(),
                                    augmentTeIds.teTopologyId().string());
                }

                supportTeLinkId = new TeLinkTpGlobalKey(teTopologyId.providerId(),
                                                        teTopologyId.clientId(),
                                                        Long.valueOf(teTopologyId
                                                                .topologyId()),
                                                        tenIdLong, teSupportLinkTpId);
            }
        }

        return supportTeLinkId;
    }

    /**
     * Finds TeNodeId from a yangNetwork and yangNodeId.
     *
     * @param yangNetwork YANG network object
     * @param yangNodeId YANG node Id
     * @return teNodeId teNodeId
     */
    public static TeNodeId findTeNodeId(Network yangNetwork, NodeId yangNodeId) {
        TeNodeId teNodeId = null;
        for (Node node : yangNetwork.node()) {
            if (node.nodeId().equals(yangNodeId)) {
                if (node.yangAugmentedInfoMap() != null
                        && !node.yangAugmentedInfoMap().isEmpty()) {
                    AugmentedNwNode yangNodeAugment = (AugmentedNwNode) node
                            .yangAugmentedInfo(AugmentedNwNode.class);
                    if (yangNodeAugment != null
                            && yangNodeAugment.teNodeId() != null) {
                        teNodeId = yangNodeAugment.teNodeId();
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
                                    if (yangTpAugment.teTpId() != null) {
                                        teTpId = Long.valueOf(yangTpAugment.teTpId().toString());
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
                    yang2TeSubsystemUnderlayPrimaryPath(PrimaryPath yangpath,
                                                        long teNodeId) {
        org.onosproject.tetopology.management.api.link.UnderlayPrimaryPath teUnderlayPrimaryPath = null;

        List<PathElement> pathElementList = Lists.newArrayList();
        for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology
                .telinkunderlayattributes.primarypath.
                PathElement pathElementYang : yangpath.pathElement()) {

            long nodeId = 0;
            ElementType type = null;
            // TODO: check more types
            if (pathElementYang.type() instanceof UnnumberedLink) {
                nodeId = Long.valueOf(((UnnumberedLink) pathElementYang.type()).routerId().toString());
                type = new TeLinkId(((UnnumberedLink) pathElementYang.type()).interfaceId());
            }
            PathElement tePathElement = new PathElement(pathElementYang.pathElementId(),
                                                        nodeId,
                                                        type,
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
                                                        rev20170110.ietftetopology.telinkunderlayattributes.
                    BackupPath> yangpaths,
                long teNodeId) {

        List<UnderlayBackupPath> underlayBackupPathsList = Lists.newArrayList();
        for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
                .ietftetopology.telinkunderlayattributes.
                BackupPath yangConfig : yangpaths) {
            UnderlayBackupPath ubp = null;
            List<PathElement> backupPathElementList = Lists.newArrayList();
            long nodeId = 0;
            ElementType type = null;
            for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology
                    .telinkunderlayattributes.backuppath.
                    PathElement backupYang : yangConfig.pathElement()) {
                // TODO: check more types
                if (backupYang.type() instanceof UnnumberedLink) {
                    nodeId = Long.valueOf(((UnnumberedLink) backupYang.type()).routerId().toString());
                    type = new TeLinkId(((UnnumberedLink) backupYang.type()).interfaceId());
                }
                PathElement tePathElementBackup = new PathElement(backupYang.pathElementId(),
                                                                  nodeId,
                                                                  type,
                                                                  false);
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

        if (yangLinkAtrr.underlay() != null && yangLinkAtrr.underlay().primaryPath() != null) {
            underlayPrimaryPath =
                    yang2TeSubsystemUnderlayPrimaryPath(yangLinkAtrr.underlay().primaryPath(), srcTeNodeId);
        }

        if (yangLinkAtrr.underlay() != null && yangLinkAtrr.underlay().backupPath() != null) {
            underlayBackupPathsList =
                    yang2TeSubsystemUnderlayBackupPaths(yangLinkAtrr.underlay().backupPath(), srcTeNodeId);
        }

        org.onosproject.tetopology.management.api.link.UnderlayPath teUnderlay = null;
        if (yangLinkAtrr.underlay() != null) {
            teUnderlay = new org.onosproject.tetopology.management.api.link.
                        UnderlayPath(underlayPrimaryPath,
                                     underlayBackupPathsList,
                                     TunnelProtectionType.of(yangLinkAtrr.underlay().protectionType()),
                                     (Long) yangLinkAtrr.underlay().tunnelSrc().tunnelTpRef(), // null safe?
                                     (Long) yangLinkAtrr.underlay().tunnelDes().tunnelTpRef(), // null safe?
                                     teTunnelId);
        }

        return teUnderlay;
    }

    /**
     * Converts a TE Topology link event from the data format used in
     * the core to its corresponding YANG Object (YO) format.
     *
     * @param eventType Link event type
     * @param linkData  TE Topology link event data
     * @param teTopologyService TE Topology Service object
     * @return YANG Object converted from linkData
     */
    public static TeLinkEvent teNetworkLink2yangTeLinkEvent(TeTopologyEventTypeEnum eventType,
                                                            NetworkLinkEventSubject linkData,
                                                            TeTopologyService teTopologyService) {
        checkNotNull(linkData);
        TeLinkEvent.TeLinkEventBuilder teLinkEventBuilder = new DefaultTeLinkEvent.TeLinkEventBuilder();

        TeTopologyEventType yangEventType = new TeTopologyEventType(eventType);
        teLinkEventBuilder.eventType(yangEventType);
        NetworkId networkId = NetworkId.fromString(linkData.key().networkId().toString());
        teLinkEventBuilder.networkRef(networkId);
        LinkId linkId = LinkId.fromString(linkData.key().linkId().toString());
        teLinkEventBuilder.linkRef(linkId);

        if (linkData != null && linkData.networkLink() != null) {
            NetworkLink link = linkData.networkLink();
            State yangTeLinkState = teLink2YangState(link.teLink(), teTopologyService);

            teLinkEventBuilder.operStatus(yangTeLinkState.operStatus());
            teLinkEventBuilder.informationSource(yangTeLinkState.informationSource());
            teLinkEventBuilder.informationSourceEntry(yangTeLinkState.informationSourceEntry());
            teLinkEventBuilder.informationSourceState(yangTeLinkState.informationSourceState());
            teLinkEventBuilder.isTransitional(yangTeLinkState.isTransitional());
            teLinkEventBuilder.recovery(yangTeLinkState.recovery());
            teLinkEventBuilder.teLinkAttributes(yangTeLinkState.teLinkAttributes());
            teLinkEventBuilder.underlay(yangTeLinkState.underlay());
        }

        return teLinkEventBuilder.build();
    }

    /**
     * Retrieves the TE network link key from a given YANG network link
     * notification event.
     *
     * @param yangLinkEvent YANG network link notification
     * @return TE network link key
     */
    public static NetworkLinkKey yangLinkEvent2NetworkLinkKey(TeLinkEvent yangLinkEvent) {
        NetworkId networkRef = NetworkId.fromString(yangLinkEvent.networkRef().toString());
        LinkId linkRef = LinkId.fromString(yangLinkEvent.linkRef().toString());
        KeyId networkId = KeyId.keyId(networkRef.uri().toString());
        KeyId linkId = KeyId.keyId(linkRef.uri().toString());

        NetworkLinkKey networkLinkKey = new NetworkLinkKey(networkId, linkId);

        return networkLinkKey;

    }

    /**
     * Converts a YANG network link notification event into a TE network link.
     *
     * @param yangLinkEvent YANG network link notification
     * @param teTopologyService TE Topology service used to help the conversion
     * @return TE network link
     */
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
        if (sourceTp == null) {
            return null;
        }
        NodeTpKey destTp = networkLink.destination();

        List<NetworkLinkKey> supportingLinkIds = networkLink.supportingLinkIds();
        TeLink teLink = networkLink.teLink();
        if (teLink == null) {
            return null;
        }

        TeOperStatus opState = yangLinkEvent.operStatus();
        org.onosproject.tetopology.management.api.
        TeStatus opStatus = EnumConverter.yang2TeSubsystemOpStatus(opState);

        TeLink updatedTeLink = yangLinkEvent2TeLinkAttributes(yangLinkEvent,
                                                              teLink, opStatus, teTopologyService);


        NetworkLink updatedNetworkLink = new DefaultNetworkLink(linkId, sourceTp, destTp, supportingLinkIds,
                                                                updatedTeLink);

        return updatedNetworkLink;
    }

    private static TeLink yangLinkEvent2TeLinkAttributes(TeLinkEvent yangLinkEvent, TeLink oldTeLink, TeStatus
                                                         opStatus, TeTopologyService teTopologyService) {

        TeLinkAttributes yangTeLinkAttrs = yangLinkEvent.teLinkAttributes();

        TeLinkTpKey teLinkKey = oldTeLink.teLinkKey();

        long teNodeIdDest = 0;
        long teNodeIdSrc = 0;

        TeLinkTpGlobalKey supportTeLinkId = oldTeLink.supportingTeLinkId();
        TeLinkTpKey peerTeLinkKey = oldTeLink.peerTeLinkKey();

        TeTopologyKey underlayTopologyId = null;
        KeyId networkRef = null;
        if (yangTeLinkAttrs.underlay() != null &&
                yangTeLinkAttrs.underlay().primaryPath() != null &&
                yangTeLinkAttrs.underlay().primaryPath().networkRef() != null) {
            networkRef = (KeyId) yangTeLinkAttrs.underlay().primaryPath().networkRef();
        }

        if (networkRef != null && teTopologyService.network(networkRef) != null
                && teTopologyService.network(networkRef).teTopologyId() != null) {
            long clientId = teTopologyService.network(networkRef).teTopologyId().clientId();
            long providerId = teTopologyService.network(networkRef).teTopologyId().providerId();
            long topologyId = Long.valueOf(teTopologyService.network(networkRef).teTopologyId().topologyId());
            underlayTopologyId = new TeTopologyKey(providerId, clientId, topologyId);
        }

        TeLink updatedTeLink = yangLinkAttr2TeLinkAttributes(yangTeLinkAttrs, opStatus, teNodeIdSrc, teNodeIdDest,
                                                             teLinkKey,
                                                             peerTeLinkKey,
                                                             supportTeLinkId,
                                                             underlayTopologyId);

        return updatedTeLink;
    }
}
