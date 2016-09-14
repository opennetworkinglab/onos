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

import java.util.List;

import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.link.ExternalDomain;
import org.onosproject.tetopology.management.api.link.LinkProtectionType;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.link.PathElement;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.UnderlayBackupPath;
import org.onosproject.tetopology.management.api.link.UnderlayPath;
import org.onosproject.tetopology.management.api.node.TeNetworkTopologyId;
import org.onosproject.tetopology.management.api.node.TerminationPointKey;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NetworkId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NodeId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.LinkId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.TpId;
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
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.augmentednwnetworks.te.templates.linktemplate
               .telinkattributes.LinkProtectionTypeEnum;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.AugmentedNtLink;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.DefaultAugmentedNtLink;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.DefaultAugmentedNtLink.AugmentedNtLinkBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.DefaultTe;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.DefaultTe.TeBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.Config;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
.ietftetopology.networks.network.link.augmentedntlink.te.Config.ConfigBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.DefaultConfig;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.DefaultState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.State;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
.ietftetopology.networks.network.link.augmentedntlink.te.State.StateBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.DefaultTeLinkAttributes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.TeLinkAttributes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.TeLinkAttributes
               .TeLinkAttributesBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes
               .DefaultExternalDomain;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes
               .DefaultExternalDomain.ExternalDomainBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes.DefaultTeSrlgs;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes.DefaultUnderlay;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes
               .DefaultUnreservedBandwidth;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes.TeSrlgs
               .TeSrlgsBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes.Underlay;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes.Underlay
               .UnderlayBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes
               .UnreservedBandwidth;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes
               .UnreservedBandwidth.UnreservedBandwidthBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes
               .underlay.DefaultUnderlayBackupPath;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology
               .networks.network.link.augmentedntlink.te.config.telinkattributes.underlay
               .DefaultUnderlayBackupPath.UnderlayBackupPathBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes
               .underlay.DefaultUnderlayPrimaryPath;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes
               .underlay.DefaultUnderlayTrailDes.UnderlayTrailDesBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes
               .underlay.DefaultUnderlayTrailSrc.UnderlayTrailSrcBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes
               .underlay.UnderlayPrimaryPath;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes
               .underlay.UnderlayPrimaryPath.UnderlayPrimaryPathBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes
               .underlay.underlayprimarypath.DefaultPathElement;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.Srlg;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeLinkAccessType;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeNodeId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeTopologyId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeTpId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705
               .ietftetypes.telinkaccesstype.TeLinkAccessTypeEnum;

import com.google.common.collect.Lists;

/**
 * The conversion functions.
 */
public final class LinkConverter {
    private static final String
        E_NULL_TELINK_UNDERLAY_PATH = "TeSubsystem link underlayPath object cannot be null";
    private static final String
        E_NULL_TELINK_DATA = "TeSubsystem teLink data cannot be null";
    private static final String
        E_NULL_TELINK = "TeSubsystem teLink object cannot be null";
    private static final String
        E_NULL_YANG_TELINK_CONFIG = "YANG telink config object cannot be null";
    private static final String
        E_NULL_YANG_TELINK = "YANG Link object cannot be null";

    // no instantiation
    private LinkConverter() {
    }

    private static UnderlayBuilder te2YangConfigUnderlayPrimaryPath(
            UnderlayBuilder yangBuilder,
            org.onosproject.tetopology.management.api.link.UnderlayPrimaryPath tePath) {
        UnderlayPrimaryPathBuilder pathBuilder =
                                       DefaultUnderlayPrimaryPath.builder();
        if (tePath.pathElements() != null) {
            for (PathElement pathElementTe : tePath.pathElements()) {
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
                   .rev20160708.ietftetopology.networks.network.link.augmentedntlink.te
                   .config.telinkattributes.underlay.underlayprimarypath.PathElement
                   .PathElementBuilder pathElementYangBuilder = DefaultPathElement.builder();
                // FIXME: pathElementTe does not have any getter or
                // setter at the moment --> pathElementYangBuilder =
                // pathElementYangBuilder.pathElementId(pathElementTe.getPathElementId());
                pathBuilder = pathBuilder.addToPathElement(pathElementYangBuilder.build());
            }
        }
        pathBuilder = pathBuilder.networkIdRef(NetworkId.fromString(
                                               tePath.ref().getNetworkId().toString()))
                                 .teTopologyIdRef(TeTopologyId.fromString(
                                               tePath.ref().getTopologyId().topologyId()));
        return yangBuilder.underlayPrimaryPath(pathBuilder.build());
    }

    private static UnderlayBuilder te2YangConfigUnderlayBackupPaths(
            UnderlayBuilder yangBuilder,
            List<org.onosproject.tetopology.management.api.link.UnderlayBackupPath> tePaths) {

        for (UnderlayBackupPath tePath : tePaths) {
            UnderlayBackupPathBuilder pathBuilder = DefaultUnderlayBackupPath.builder();
            pathBuilder = pathBuilder.index(tePath.index());
            pathBuilder = pathBuilder.networkIdRef(NetworkId.fromString(
                                                        tePath.ref().getNetworkId().toString()))
                                     .teTopologyIdRef(TeTopologyId.fromString(
                                                        tePath.ref().getTopologyId().topologyId()));
            for (PathElement backupPathElementTe : tePath.pathElements()) {
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
                   .rev20160708.ietftetopology.networks.network.link.augmentedntlink.te
                   .config.telinkattributes.underlay.underlaybackuppath.PathElement
                   .PathElementBuilder elementBuilder =
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
                   .rev20160708.ietftetopology.networks.network.link.augmentedntlink.te
                   .config.telinkattributes.underlay.underlaybackuppath.DefaultPathElement.builder();
                // FIXME: backupPathElementTe does not have any
                // getter or setter at the moment -->
                // elementBuilder =
                // elementBuilder.pathElementId(backupPathElementTe.getPathElementId());
                pathBuilder = pathBuilder.addToPathElement(elementBuilder.build());
            }
            yangBuilder = yangBuilder.addToUnderlayBackupPath(pathBuilder.build());
        }

        return yangBuilder;
    }

    /**
     * TE Link underlay path Config object conversion from TE Topology subsystem to YANG.
     *
     * @param  tePath TE underlay path object
     * @return Link underlay path Config YANG object
     */
    private static Underlay teSubsystem2YangConfigUnderlayPath(UnderlayPath tePath) {
        checkNotNull(tePath, E_NULL_TELINK_UNDERLAY_PATH);
        UnderlayBuilder builder =
                DefaultUnderlay.builder().underlayProtectionType(tePath.protectionType());

        if (tePath.primaryPath() != null) {
            builder = te2YangConfigUnderlayPrimaryPath(builder, tePath.primaryPath());
        }
        if (tePath.trailSrc() != null) {
            builder = builder.underlayTrailSrc(
                                  new UnderlayTrailSrcBuilder()
                                          .networkRef(NetworkId.fromString(
                                                  tePath.trailSrc().networkId().toString()))
                                          .nodeRef(NodeId.fromString(
                                                  tePath.trailSrc().nodeId().toString()))
                                          .tpRef(TpId.fromString(
                                                  tePath.trailSrc().tpId().toString()))
                                          .build());
        }
        if (tePath.trailDes() != null) {
            builder = builder.underlayTrailDes(
                                  new UnderlayTrailDesBuilder()
                                          .networkRef(NetworkId.fromString(
                                                  tePath.trailDes().networkId().toString()))
                                          .nodeRef(NodeId.fromString(
                                                  tePath.trailDes().nodeId().toString()))
                                          .tpRef(TpId.fromString(
                                                  tePath.trailDes().tpId().toString()))
                                          .build());
        }
        if (tePath.backupPaths() != null) {
            builder = te2YangConfigUnderlayBackupPaths(builder, tePath.backupPaths());
        }

        return builder.build();
    }

    private static org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
                      .rev20160708.ietftetopology.networks.network.link.augmentedntlink.te
                      .state.telinkattributes.Underlay.UnderlayBuilder
            te2YangStateUnderlayPrimaryPath(
                   org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
                      .rev20160708.ietftetopology.networks.network.link.augmentedntlink.te
                      .state.telinkattributes.Underlay.UnderlayBuilder yangBuilder,
                   org.onosproject.tetopology.management.api.link.UnderlayPrimaryPath tePath) {
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
            .ietftetopology.networks.network.link.augmentedntlink.te.state.telinkattributes
            .underlay.UnderlayPrimaryPath.UnderlayPrimaryPathBuilder pathBuilder =
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
            .ietftetopology.networks.network.link.augmentedntlink.te.state.telinkattributes
            .underlay.DefaultUnderlayPrimaryPath.builder();
        if (tePath.pathElements() != null) {
            for (PathElement pathElementTe : tePath.pathElements()) {
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
                   .rev20160708.ietftetopology.networks.network.link.augmentedntlink.te
                   .state.telinkattributes.underlay.underlayprimarypath.PathElement
                   .PathElementBuilder pathElementYangBuilder =
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
                   .rev20160708.ietftetopology.networks.network.link.augmentedntlink.te
                   .state.telinkattributes.underlay.underlayprimarypath.DefaultPathElement
                   .builder();
                // FIXME: pathElementTe does not have any getter or
                // setter at the moment --> pathElementYangBuilder =
                // pathElementYangBuilder.pathElementId(pathElementTe.getPathElementId());
                pathBuilder = pathBuilder.addToPathElement(pathElementYangBuilder.build());
            }
        }
        pathBuilder = pathBuilder.networkIdRef(NetworkId.fromString(
                                               tePath.ref().getNetworkId().toString()))
                                 .teTopologyIdRef(TeTopologyId.fromString(
                                               tePath.ref().getTopologyId().topologyId()));
        return yangBuilder.underlayPrimaryPath(pathBuilder.build());
    }

    private static org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
                      .rev20160708.ietftetopology.networks.network.link.augmentedntlink.te
                      .state.telinkattributes.Underlay.UnderlayBuilder
           te2YangStateUnderlayBackupPaths(
                   org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
                   .rev20160708.ietftetopology.networks.network.link.augmentedntlink.te
                   .state.telinkattributes.Underlay.UnderlayBuilder yangBuilder,
            List<org.onosproject.tetopology.management.api.link.UnderlayBackupPath> tePaths) {

        for (UnderlayBackupPath tePath : tePaths) {
            org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
            .ietftetopology.networks.network.link.augmentedntlink.te.state.telinkattributes
            .underlay.UnderlayBackupPath.UnderlayBackupPathBuilder
            pathBuilder = org.onosproject.yang.gen.v1.urn.ietf.params.xml
            .ns.yang.ietf.te.topology.rev20160708.ietftetopology.networks.network.link.augmentedntlink
            .te.state.telinkattributes.underlay.DefaultUnderlayBackupPath
                    .builder();

            pathBuilder = pathBuilder.index(tePath.index());
            pathBuilder = pathBuilder.networkIdRef(NetworkId.fromString(
                                                        tePath.ref().getNetworkId().toString()))
                                     .teTopologyIdRef(TeTopologyId.fromString(
                                                        tePath.ref().getTopologyId().topologyId()));
            for (PathElement backupPathElementTe : tePath.pathElements()) {
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
                   .rev20160708.ietftetopology.networks.network.link.augmentedntlink.te
                   .state.telinkattributes.underlay.underlaybackuppath.PathElement
                   .PathElementBuilder elementBuilder =
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
                   .rev20160708.ietftetopology.networks.network.link.augmentedntlink.te
                   .state.telinkattributes.underlay.underlaybackuppath.DefaultPathElement.builder();
                // FIXME: backupPathElementTe does not have any
                // getter or setter at the moment -->
                // elementBuilder =
                // elementBuilder.pathElementId(backupPathElementTe.getPathElementId());
                pathBuilder = pathBuilder.addToPathElement(elementBuilder.build());
            }
            yangBuilder = yangBuilder.addToUnderlayBackupPath(pathBuilder.build());
        }

        return yangBuilder;
    }

    /**
     * TE Link underlay path State object conversion from TE Topology subsystem to YANG.
     *
     * @param tePath TE underlay object
     * @return Link underlay path State YANG object
     */
    private static org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
                       .rev20160708.ietftetopology.networks.network.link.augmentedntlink.te
                       .state.telinkattributes.Underlay
                   teSubsystem2YangStateUnderlayPath(UnderlayPath tePath) {
        checkNotNull(tePath, E_NULL_TELINK_UNDERLAY_PATH);

        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
            .ietftetopology.networks.network.link.augmentedntlink.te.state.telinkattributes
            .Underlay.UnderlayBuilder builder =
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
            .ietftetopology.networks.network.link.augmentedntlink.te.state.telinkattributes
            .DefaultUnderlay.builder().underlayProtectionType(tePath.protectionType());

        if (tePath.primaryPath() != null) {
            builder = te2YangStateUnderlayPrimaryPath(builder, tePath.primaryPath());

        }
        if (tePath.trailSrc() != null) {
            builder = builder.underlayTrailSrc(
                          new org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te
                                  .topology.rev20160708.ietftetopology.networks.network.link
                                  .augmentedntlink.te.state.telinkattributes.underlay
                                  .DefaultUnderlayTrailSrc.UnderlayTrailSrcBuilder()
                              .networkRef(NetworkId.fromString(
                                              tePath.trailSrc().networkId().toString()))
                              .nodeRef(NodeId.fromString(
                                              tePath.trailSrc().nodeId().toString()))
                              .tpRef(TpId.fromString(
                                              tePath.trailSrc().tpId().toString()))
                              .build());
        }
        if (tePath.trailDes() != null) {
            builder = builder.underlayTrailDes(
                          new org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te
                                   .topology.rev20160708.ietftetopology.networks.network.link
                                   .augmentedntlink.te.state.telinkattributes.underlay
                                   .DefaultUnderlayTrailDes.UnderlayTrailDesBuilder()
                              .networkRef(NetworkId.fromString(
                                              tePath.trailDes().networkId().toString()))
                              .nodeRef(NodeId.fromString(
                                              tePath.trailDes().nodeId().toString()))
                              .tpRef(TpId.fromString(
                                              tePath.trailDes().tpId().toString()))
                              .build());
        }
        if (tePath.backupPaths() != null) {
            builder = te2YangStateUnderlayBackupPaths(builder, tePath.backupPaths());
        }

        return builder.build();
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
        if (teLink.linkIndex() != null) {
            attrBuilder = attrBuilder.linkIndex(teLink.linkIndex());
        }
        if (teLink.name() != null) {
            attrBuilder = attrBuilder.name(teLink.name());
        }
        if (teLink.adminStatus() != null) {
            attrBuilder = attrBuilder
                    .adminStatus(EnumConverter
                            .teSubsystem2YangAdminStatus(teLink
                            .adminStatus()));
        }
        if (teLink.accessType() != null) {
            attrBuilder = attrBuilder
                    .accessType(teSubsystem2YangTeLinkAccess(teLink
                            .accessType()));
        }
        if (teLink.linkProtectionType() != null) {
            attrBuilder = attrBuilder
                    .linkProtectionType(teSubsystem2YangLinkProtectionType(teLink
                            .linkProtectionType()));
        }
        if (teLink.maxLinkBandwidth() != null) {
            attrBuilder = attrBuilder.maxLinkBandwidth(teLink.maxLinkBandwidth());
        }
        if (teLink.maxResvLinkBandwidth() != null) {
            attrBuilder = attrBuilder.maxResvLinkBandwidth(teLink.maxResvLinkBandwidth());
        }
        attrBuilder = attrBuilder.teDefaultMetric(teLink.teDefaultMetric());
        if (teLink.teSrlgs() != null) {
            TeSrlgsBuilder teSrlgsBuilder = DefaultTeSrlgs.builder();
            for (Long srlgLongVal : teLink.teSrlgs()) {
                teSrlgsBuilder = teSrlgsBuilder.addToValue(new Srlg(srlgLongVal));
            }
            attrBuilder = attrBuilder.teSrlgs(teSrlgsBuilder.build());
        }
        attrBuilder = attrBuilder.isAbstract(teLink.isAbstract());
        if (teLink.underlayPath() != null) {
            attrBuilder = attrBuilder.underlay(
                              teSubsystem2YangConfigUnderlayPath(teLink.underlayPath()));
        }
        if (teLink.externalDomain() != null) {
            ExternalDomainBuilder edBuilder =
                    DefaultExternalDomain.builder()
                                         .plugId(teLink.externalDomain().plugId())
                                         .remoteTeLinkTpId(TeTpId.fromString(
                                                  teLink.externalDomain().remoteTeLinkTpId().toString()))
                                         .remoteTeNodeId(TeNodeId.fromString(
                                                  teLink.externalDomain().remoteTeNodeId().toString()));
            attrBuilder = attrBuilder.externalDomain(edBuilder.build());
        }

        if (teLink.unreservedBandwidths() != null) {
            for (org.onosproject.tetopology.management.api.link.UnreservedBandwidth unResBwTe :
                        teLink.unreservedBandwidths()) {
                UnreservedBandwidthBuilder urBuilder =
                        DefaultUnreservedBandwidth.builder()
                                                  .bandwidth(unResBwTe.bandwidth())
                                                  .priority(unResBwTe.priority());
                attrBuilder = attrBuilder.addToUnreservedBandwidth(urBuilder.build());
            }
        }

        ConfigBuilder yangConfigBuilder = DefaultConfig.builder()
                                                       .teLinkAttributes(attrBuilder.build());
        return yangConfigBuilder.build();
    }

    private static LinkProtectionTypeEnum
                        teSubsystem2YangLinkProtectionType(LinkProtectionType linkProtectionType) {
        switch (linkProtectionType) {
        case ENHANCED:
            return LinkProtectionTypeEnum.ENHANCED;
        case EXTRA_TRAFFIC:
            return LinkProtectionTypeEnum.EXTRA_TRAFFIC;
        case SHARED:
            return LinkProtectionTypeEnum.SHARED;
        case UNPROTECTED:
            return LinkProtectionTypeEnum.UNPROTECTED;
        case YANGAUTOPREFIX1_FOR_1:
            return LinkProtectionTypeEnum.YANGAUTOPREFIX1_FOR_1;
        case YANGAUTOPREFIX1_PLUS_1:
            return LinkProtectionTypeEnum.YANGAUTOPREFIX1_PLUS_1;
        default:
            return null;
        }
    }

    private static TeLinkAccessType teSubsystem2YangTeLinkAccess(
                       org.onosproject.tetopology.management.api.link.TeLinkAccessType accessType) {
        switch (accessType) {
        case MULTI_ACCESS:
            return TeLinkAccessType.of(TeLinkAccessTypeEnum.MULTI_ACCESS);
        case POINT_TO_POINT:
            return TeLinkAccessType.of(TeLinkAccessTypeEnum.POINT_TO_POINT);
        default:
            return null;
        }
    }

    /**
     * TE Link State object conversion from TE Topology subsystem to YANG.
     *
     * @param teLink TE link object
     * @return TE Link State YANG object
     */
    private static State teLink2YangState(TeLink teLink) {
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
            .ietftetopology.networks.network.link.augmentedntlink.te.state.TeLinkAttributes
            .TeLinkAttributesBuilder attrBuilder =
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
            .ietftetopology.networks.network.link.augmentedntlink.te.state.DefaultTeLinkAttributes
            .builder()
            .teDefaultMetric(teLink.teDefaultMetric())
            .isAbstract(teLink.isAbstract());
        if (teLink.linkIndex() != null) {
            attrBuilder = attrBuilder.linkIndex(teLink.linkIndex());
        }
        if (teLink.name() != null) {
            attrBuilder = attrBuilder.name(teLink.name());
        }
        if (teLink.adminStatus() != null) {
            attrBuilder = attrBuilder
                    .adminStatus(EnumConverter
                            .teSubsystem2YangAdminStatus(teLink
                            .adminStatus()));
        }
        if (teLink.accessType() != null) {
            attrBuilder = attrBuilder
                    .accessType(teSubsystem2YangTeLinkAccess(teLink
                            .accessType()));
        }
        if (teLink.linkProtectionType() != null) {
            attrBuilder = attrBuilder
                    .linkProtectionType(teSubsystem2YangStateLinkProtectionType(teLink
                            .linkProtectionType()));
        }
        if (teLink.maxLinkBandwidth() != null) {
            attrBuilder = attrBuilder.maxLinkBandwidth(teLink.maxLinkBandwidth());
        }
        if (teLink.maxResvLinkBandwidth() != null) {
            attrBuilder = attrBuilder.maxResvLinkBandwidth(teLink.maxResvLinkBandwidth());
        }
        if (teLink.teSrlgs() != null) {
            org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                .ietftetopology.networks.network.link.augmentedntlink.te.state.telinkattributes
                .TeSrlgs.TeSrlgsBuilder srlgsBuilder =
            org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                .ietftetopology.networks.network.link.augmentedntlink.te.state.telinkattributes
                .DefaultTeSrlgs.builder();
            for (Long srlgLongVal : teLink.teSrlgs()) {
                srlgsBuilder = srlgsBuilder.addToValue(new Srlg(srlgLongVal));
            }
            attrBuilder = attrBuilder.teSrlgs(srlgsBuilder.build());
        }
        if (teLink.underlayPath() != null) {
            attrBuilder = attrBuilder.underlay(teSubsystem2YangStateUnderlayPath(teLink.underlayPath()));
        }
        if (teLink.externalDomain() != null) {
            org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                    .ietftetopology.networks.network.link.augmentedntlink.te.state.telinkattributes
                    .ExternalDomain.ExternalDomainBuilder edBuilder =
            org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                    .ietftetopology.networks.network.link.augmentedntlink.te.state.telinkattributes
                    .DefaultExternalDomain.builder()
                                          .plugId(teLink.externalDomain().plugId())
                                          .remoteTeLinkTpId(TeTpId.fromString(
                                                  teLink.externalDomain().remoteTeLinkTpId().toString()))
                                          .remoteTeNodeId(TeNodeId.fromString(
                                                  teLink.externalDomain().remoteTeNodeId().toString()));
            attrBuilder = attrBuilder.externalDomain(edBuilder.build());
        }
        if (teLink.unreservedBandwidths() != null) {
            for (org.onosproject.tetopology.management.api.link.UnreservedBandwidth unResBwTe :
                    teLink.unreservedBandwidths()) {
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                    .ietftetopology.networks.network.link.augmentedntlink.te.state.telinkattributes
                    .UnreservedBandwidth.UnreservedBandwidthBuilder urBuilder =
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                    .ietftetopology.networks.network.link.augmentedntlink.te.state.telinkattributes
                    .DefaultUnreservedBandwidth.builder()
                                               .bandwidth(unResBwTe.bandwidth())
                                               .priority(unResBwTe.priority());
                attrBuilder = attrBuilder.addToUnreservedBandwidth(urBuilder.build());
            }
        }

        StateBuilder yangStateBuilder = DefaultState.builder()
                                                    .teLinkAttributes(attrBuilder.build());
        if (teLink.opStatus() != null) {
            yangStateBuilder = yangStateBuilder
                    .operStatus(EnumConverter
                            .teSubsystem2YangOperStatus(teLink
                            .opStatus()));
        }

        return yangStateBuilder.build();
    }

    private static org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.
        rev20160708.ietftetopology.networks.network.link.augmentedntlink.te.state.informationsourceentry.
        LinkProtectionTypeEnum teSubsystem2YangStateLinkProtectionType(LinkProtectionType linkProtectionType) {
        switch (linkProtectionType) {
        case ENHANCED:
            return org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.
                    rev20160708.ietftetopology.networks.network.link.augmentedntlink.te.state.informationsourceentry.
                    LinkProtectionTypeEnum.ENHANCED;
        case EXTRA_TRAFFIC:
            return org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.
                    rev20160708.ietftetopology.networks.network.link.augmentedntlink.te.state.informationsourceentry.
                    LinkProtectionTypeEnum.EXTRA_TRAFFIC;
        case SHARED:
            return org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.
                    rev20160708.ietftetopology.networks.network.link.augmentedntlink.te.state.informationsourceentry.
                    LinkProtectionTypeEnum.SHARED;
        case UNPROTECTED:
            return org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.
                    rev20160708.ietftetopology.networks.network.link.augmentedntlink.te.state.informationsourceentry.
                    LinkProtectionTypeEnum.UNPROTECTED;
        case YANGAUTOPREFIX1_FOR_1:
            return org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.
                    rev20160708.ietftetopology.networks.network.link.augmentedntlink.te.state.informationsourceentry.
                    LinkProtectionTypeEnum.YANGAUTOPREFIX1_FOR_1;
        case YANGAUTOPREFIX1_PLUS_1:
            return org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.
                    rev20160708.ietftetopology.networks.network.link.augmentedntlink.te.state.informationsourceentry.
                    LinkProtectionTypeEnum.YANGAUTOPREFIX1_PLUS_1;
        default:
            return null;
        }
    }

    /**
     * Link object conversion from TE Topology subsystem to YANG.
     *
     * @param teLink TE subsystem link object
     * @return YANG link object
     */
    public static Link teSubsystem2YangLink(
            org.onosproject.tetopology.management.api.link.NetworkLink teLink) {
        checkNotNull(teLink, E_NULL_TELINK);

        LinkId linkId = LinkId.fromString(teLink.linkId().toString());
        LinkBuilder builder = DefaultLink.builder().linkId(linkId);
        if (teLink.getSupportingLinkIds() != null) {
            List<SupportingLink> slinks = Lists.newArrayList();
            SupportingLinkBuilder spLinkBuilder = DefaultSupportingLink.builder();
            for (NetworkLinkKey linkKey : teLink.getSupportingLinkIds()) {
                slinks.add(spLinkBuilder.networkRef(NetworkId.fromString(
                                                    linkKey.networkId().toString()))
                                        .linkRef(LinkId.fromString(
                                                    linkKey.linkId().toString()))
                                        .build());
            }
            builder = builder.supportingLink(slinks);
        }
        if (teLink.getSource() != null) {
            SourceBuilder sourceBuilder = DefaultSource
                                              .builder()
                                              .sourceNode(NodeId.fromString(
                                                   teLink.getSource().nodeId().toString()))
                                              .sourceTp(TpId.fromString(
                                                   teLink.getSource().tpId().toString()));
            builder = builder.source(sourceBuilder.build());
        }
        if (teLink.getDestination() != null) {
            DestinationBuilder destBuilder = DefaultDestination
                                                 .builder()
                                                 .destNode(NodeId.fromString(
                                                      teLink.getDestination().nodeId().toString()))
                                                 .destTp(TpId.fromString(
                                                      teLink.getDestination().tpId().toString()));
            builder = builder.destination(destBuilder.build());
        }

        if (teLink.getTe() != null) {
            TeLink teData = teLink.getTe();
            TeBuilder yangTeBuilder = DefaultTe.builder()
                                               .config(teLink2YangConfig(teData))
                                               .state(teLink2YangState(teData));
            AugmentedNtLinkBuilder linkAugmentBuilder =
                    DefaultAugmentedNtLink.builder()
                                          .te(yangTeBuilder.build());
            builder.addYangAugmentedInfo(linkAugmentBuilder.build(), AugmentedNtLink.class);
        }

        return builder.build();
    }

    private static org.onosproject.tetopology.management.api.link.UnderlayPath
           yang2TeSubsystemUnderlayPrimaryPath(
                            org.onosproject.tetopology.management.api.link.UnderlayPath teUnderlay,
                            UnderlayPrimaryPath yangpath) {
        org.onosproject.tetopology.management.api.link.UnderlayPrimaryPath teUnderlayPrimaryPath =
                new org.onosproject.tetopology.management.api.link.UnderlayPrimaryPath();
        teUnderlayPrimaryPath.setRef(new TeNetworkTopologyId(KeyId.keyId(
                                             yangpath.networkIdRef().toString()),
                                     new org.onosproject.tetopology.management.api.TeTopologyId(
                                             yangpath.providerIdRef().uint32(),
                                             yangpath.clientIdRef().uint32(),
                                             yangpath.teTopologyIdRef().toString())));

        List<PathElement> pathElementList = Lists.newArrayList();
        for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes
                .underlay.underlayprimarypath.PathElement pathElementConfigYang :
                    yangpath.pathElement()) {
            //PathElement tePathElement = new PathElement();
            // FIXME: tePathElement does not have any getter
            // or setter at the moment -->
            // tePathElement.set...
            // TODO: construct the tePathElement object
            // properly using pathElementConfigYang
            //pathElementList.add(tePathElement);
        }
        teUnderlayPrimaryPath.setPathElement(pathElementList);
        teUnderlay.setPrimaryPath(teUnderlayPrimaryPath);

        return teUnderlay;
    }

    private static org.onosproject.tetopology.management.api.link.UnderlayPath
            yang2TeSubsystemUnderlayBackupPaths(
                     org.onosproject.tetopology.management.api.link.UnderlayPath teUnderlay,
                     List<org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te
                             .topology.rev20160708.ietftetopology.networks.network.link
                             .augmentedntlink.te.config.telinkattributes.underlay
                             .UnderlayBackupPath> yangpaths) {
        List<UnderlayBackupPath> underlayBackupPathsList = Lists.newArrayList();
        for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                .ietftetopology.networks.network.link.augmentedntlink.te.config.telinkattributes
                    .underlay.UnderlayBackupPath yangConfig : yangpaths) {
            UnderlayBackupPath ubp = new UnderlayBackupPath();
            ubp.setIndex(yangConfig.index());
            ubp.setRef(new TeNetworkTopologyId(KeyId.keyId(yangConfig.networkIdRef().toString()),
                                               new org.onosproject.tetopology.management.api.TeTopologyId(
                                                       yangConfig.providerIdRef().uint32(),
                                                       yangConfig.clientIdRef().uint32(),
                                                       yangConfig.teTopologyIdRef().toString()
                                              )));
            List<PathElement> backupPathElementList = Lists.newArrayList();
            for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
                    .rev20160708.ietftetopology.networks.network.link.augmentedntlink.te.config
                    .telinkattributes.underlay.underlaybackuppath.PathElement
                        pathElementBackupYang : yangConfig.pathElement()) {
                //PathElement tePathElementBackup = new PathElement();
                // FIXME: tePathElement does not have any
                // getter or setter at the moment -->
                // tePathElement.set...
                // TODO: construct the tePathElement object
                // properly using pathElementBackupYang
                //backupPathElementList.add(tePathElementBackup);
            }
            ubp.setPathElement(backupPathElementList);
        }
        teUnderlay.setBackupPath(underlayBackupPathsList);

        return teUnderlay;
    }

    /**
     * TE Link underlay path Config object conversion from YANG to TE Topology subsystem.
     *
     * @param yangLinkAtrr YANG link Config YANG object
     * @return teSubsystem TE underlay path object
     */
    private static org.onosproject.tetopology.management.api.link.UnderlayPath
                    yang2TeSubsystemUnderlayPath(TeLinkAttributes yangLinkAtrr) {
        checkNotNull(yangLinkAtrr, E_NULL_YANG_TELINK_CONFIG);

        org.onosproject.tetopology.management.api.link.UnderlayPath teUnderlay =
                new org.onosproject.tetopology.management.api.link.UnderlayPath();

        teUnderlay.setProtectionType(yangLinkAtrr.underlay().underlayProtectionType());

        if (yangLinkAtrr.underlay().underlayPrimaryPath() != null) {
            teUnderlay = yang2TeSubsystemUnderlayPrimaryPath(teUnderlay,
                                    yangLinkAtrr.underlay().underlayPrimaryPath());
        }

        if (yangLinkAtrr.underlay().underlayBackupPath() != null) {
            teUnderlay = yang2TeSubsystemUnderlayBackupPaths(teUnderlay,
                    yangLinkAtrr.underlay().underlayBackupPath());
        }

        if (yangLinkAtrr.underlay().underlayTrailSrc() != null) {
            teUnderlay.setTrailSrc(new TerminationPointKey(
                                            KeyId.keyId(yangLinkAtrr.underlay().underlayTrailSrc()
                                                            .networkRef().uri().toString()),
                                            KeyId.keyId(yangLinkAtrr.underlay().underlayTrailSrc()
                                                            .nodeRef().uri().toString()),
                                            KeyId.keyId(yangLinkAtrr.underlay().underlayTrailSrc()
                                                            .tpRef().uri().toString())));
        }

        if (yangLinkAtrr.underlay().underlayTrailDes() != null) {
            teUnderlay.setTrailDes(new TerminationPointKey(
                                            KeyId.keyId(yangLinkAtrr.underlay().underlayTrailDes()
                                                            .networkRef().uri().toString()),
                                            KeyId.keyId(yangLinkAtrr.underlay().underlayTrailDes()
                                                            .nodeRef().uri().toString()),
                                            KeyId.keyId(yangLinkAtrr.underlay().underlayTrailDes()
                                                            .tpRef().uri().toString())));
        }

        return teUnderlay;
    }

    private static TeLink yang2TeLinkAttributes(TeLinkAttributes yangLinkAtrr,
                                                State opState) {
        TeLink te = new TeLink(yangLinkAtrr.linkIndex());
        if (yangLinkAtrr.name() != null) {
            te.setName(yangLinkAtrr.name());
        }
        if (yangLinkAtrr.adminStatus() != null) {
            te.setAdminStatus(EnumConverter.yang2TeSubsystemAdminStatus(
                                                yangLinkAtrr.adminStatus()));
        }
        if (opState != null && opState.operStatus() != null) {
            te.setOpStatus(EnumConverter.yang2TeSubsystemOpStatus(
                                             opState.operStatus()));
        }
        if (yangLinkAtrr.accessType() != null) {
            te.setAccessType(yang2TeSubsystemAccessType(
                                 yangLinkAtrr.accessType()));
        }
        if (yangLinkAtrr.linkProtectionType() != null) {
            te.setLinkProtectionType(yang2TeSubsystemLinkProtectionType(
                                         yangLinkAtrr.linkProtectionType()));
        }
        if (yangLinkAtrr.maxLinkBandwidth() != null) {
            te.setMaxLinkBandwidth(yangLinkAtrr.maxLinkBandwidth());
        }
        if (yangLinkAtrr.maxResvLinkBandwidth() != null) {
            te.setMaxResvLinkBandwidth(yangLinkAtrr.maxResvLinkBandwidth());
        }
        te.setTeDefaultMetric(yangLinkAtrr.teDefaultMetric());
        te.setIsAbstract(yangLinkAtrr.isAbstract());
        if (yangLinkAtrr.teSrlgs() != null) {
            List<Long> srlgs = Lists.newArrayList();
            for (Srlg srlgConfigYang : yangLinkAtrr.teSrlgs().value()) {
                srlgs.add(srlgConfigYang.uint32());
            }
            te.setTeSrlgs(srlgs);
        }
        if (yangLinkAtrr.externalDomain() != null) {
            te.setExternalDomain(new ExternalDomain(
                    KeyId.keyId(yangLinkAtrr.externalDomain()
                                    .remoteTeNodeId().toString()),
                    KeyId.keyId(yangLinkAtrr.externalDomain()
                                    .remoteTeLinkTpId().toString()),
                    yangLinkAtrr.externalDomain().plugId()));
        }
        if (yangLinkAtrr.underlay() != null) {
            te.setUnderlayPath(yang2TeSubsystemUnderlayPath(yangLinkAtrr));
        }
        if (yangLinkAtrr.unreservedBandwidth() != null) {
            List<org.onosproject.tetopology.management.api.link.UnreservedBandwidth>
                    unreservedBandwidths = Lists.newArrayList();
            for (UnreservedBandwidth urBwYang : yangLinkAtrr.unreservedBandwidth()) {
                org.onosproject.tetopology.management.api.link.UnreservedBandwidth unResBw =
                        new org.onosproject.tetopology.management.api.link.UnreservedBandwidth(
                                urBwYang.priority(),
                                urBwYang.bandwidth());
                unreservedBandwidths.add(unResBw);
            }
            te.setUnreservedBandwidths(unreservedBandwidths);
        }
        return te;
    }

    private static LinkProtectionType
        yang2TeSubsystemLinkProtectionType(LinkProtectionTypeEnum linkProtectionType) {
        switch (linkProtectionType) {
        case ENHANCED:
            return LinkProtectionType.ENHANCED;
        case EXTRA_TRAFFIC:
            return LinkProtectionType.EXTRA_TRAFFIC;
        case SHARED:
            return LinkProtectionType.SHARED;
        case UNPROTECTED:
            return LinkProtectionType.UNPROTECTED;
        case YANGAUTOPREFIX1_FOR_1:
            return LinkProtectionType.YANGAUTOPREFIX1_FOR_1;
        case YANGAUTOPREFIX1_PLUS_1:
            return LinkProtectionType.YANGAUTOPREFIX1_PLUS_1;
        default:
            return null;
        }
    }

    private static org.onosproject.tetopology.management.api.link.TeLinkAccessType
        yang2TeSubsystemAccessType(TeLinkAccessType accessType) {
        switch (accessType.enumeration()) {
        case MULTI_ACCESS:
            return org.onosproject.tetopology.management.api.link.TeLinkAccessType.MULTI_ACCESS;
        case POINT_TO_POINT:
            return org.onosproject.tetopology.management.api.link.TeLinkAccessType.POINT_TO_POINT;
        default:
            return null;
        }
    }

    /**
     * Link object conversion from YANG to TE Topology subsystem.
     *
     * @param yangLink YANG link object
     * @param networkId YANG networkId object
     * @return TE subsystem link object
     */
    public static org.onosproject.tetopology.management.api.link.NetworkLink
            yang2TeSubsystemLink(Link yangLink, NetworkId networkId) {
        checkNotNull(yangLink, E_NULL_YANG_TELINK);

        org.onosproject.tetopology.management.api.link.DefaultNetworkLink link =
                new org.onosproject.tetopology.management.api.link.DefaultNetworkLink(
                        KeyId.keyId(yangLink.linkId().uri().toString()));

        if (yangLink.supportingLink() != null) {
            List<NetworkLinkKey> spLinkIds = Lists.newArrayList();
            for (SupportingLink yangSpLink : yangLink.supportingLink()) {
                NetworkLinkKey linkKey = new NetworkLinkKey(KeyId.keyId(yangSpLink.networkRef().uri().toString()),
                                                            KeyId.keyId(yangSpLink.linkRef().uri().toString()));
                spLinkIds.add(linkKey);
            }
            link.setSupportingLinkIds(spLinkIds);
        }

        if (yangLink.source() != null) {
            TerminationPointKey source = new TerminationPointKey(
                                                 KeyId.keyId(networkId.uri().toString()),
                                                 KeyId.keyId(yangLink.source().sourceNode().uri().toString()),
                                                 KeyId.keyId(yangLink.source().sourceTp().uri().toString()));
            link.setSource(source);
        }

        if (yangLink.destination() != null) {
            TerminationPointKey destination = new TerminationPointKey(
                                                      KeyId.keyId(networkId.uri().toString()),
                                                      KeyId.keyId(yangLink.destination().destNode().uri().toString()),
                                                      KeyId.keyId(yangLink.destination().destTp().uri().toString()));
            link.setDestination(destination);
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
                    TeLink te = yang2TeLinkAttributes(yangLinkAtrr, yangLinkAugment.te().state());
                    link.setTe(te);
                }
            }
        }
        return link;
    }

}
