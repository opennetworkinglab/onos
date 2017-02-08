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

import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.onlab.packet.Ip4Address;
import org.onosproject.tetopology.management.api.EncodingType;
import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.SwitchingType;
import org.onosproject.tetopology.management.api.TeConstants;
import org.onosproject.tetopology.management.api.TeStatus;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.TeTopologyService;
import org.onosproject.tetopology.management.api.link.ElementType;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.link.TeIpv4;
import org.onosproject.tetopology.management.api.link.TeLinkId;
import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.link.TePathAttributes;
import org.onosproject.tetopology.management.api.link.UnderlayAbstractPath;
import org.onosproject.tetopology.management.api.node.CommonNodeData;
import org.onosproject.tetopology.management.api.node.ConnectivityMatrix;
import org.onosproject.tetopology.management.api.node.DefaultNetworkNode;
import org.onosproject.tetopology.management.api.node.DefaultTeNode;
import org.onosproject.tetopology.management.api.node.LocalLinkConnectivity;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.NetworkNodeEventSubject;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TerminationPoint;
import org.onosproject.tetopology.management.api.node.TtpKey;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev20130715.ietfinettypes.DomainName;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev20130715.ietfinettypes.IpAddress;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NetworkId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.Networks;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NodeId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.networks.Network;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
               .ietfnetwork.networks.network.DefaultNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
               .ietfnetwork.networks.network.Node;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
               .ietfnetwork.networks.network.node.DefaultSupportingNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
               .ietfnetwork.networks.network.node.SupportingNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.node.AugmentedNdNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.node.DefaultAugmentedNdNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.DefaultTeNodeEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.TeNodeEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.connectivitymatrixentryattributes.DefaultUnderlay;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.connectivitymatrixentryattributes.Underlay;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.informationsourcepernodeattributes.DefaultInformationSourceState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.informationsourcepernodeattributes.InformationSourceState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.informationsourcepernodeattributes.informationsourcestate.DefaultTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.informationsourcepernodeattributes.informationsourcestate.Topology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
               .ietftetopology.networks.network.node.AugmentedNwNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
               .ietftetopology.networks.network.node.DefaultAugmentedNwNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.networks.network.node.terminationpoint.AugmentedNtTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconnectivityattributes.DefaultTeSrlgs;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkconnectivityattributes.TeSrlgs;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkunderlayattributes.DefaultPrimaryPath;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkunderlayattributes.PrimaryPath;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkunderlayattributes.primarypath.DefaultPathElement;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkunderlayattributes.primarypath.PathElement;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeaugment.DefaultTe;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeaugment.DefaultTe.TeBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeaugment.Te;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeaugment.te.Config;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeaugment.te.DefaultConfig;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeaugment.te.DefaultState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeaugment.te.DefaultTunnelTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeaugment.te.State;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeaugment.te.TunnelTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeaugment.te.tunnelterminationpoint.DefaultSupportingTunnelTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeaugment.te.tunnelterminationpoint.SupportingTunnelTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeconfigattributes.DefaultTeNodeAttributes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeconfigattributes.TeNodeAttributes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeconfigattributes.TeNodeAttributes.TeNodeAttributesBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeconnectivitymatrix.ConnectivityMatrices;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeconnectivitymatrix.DefaultConnectivityMatrices;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeconnectivitymatrix.DefaultConnectivityMatrices.ConnectivityMatricesBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeconnectivitymatrix.connectivitymatrices.DefaultConnectivityMatrix;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeconnectivitymatrix.connectivitymatrices.DefaultConnectivityMatrix.ConnectivityMatrixBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeconnectivitymatrix.connectivitymatrices.connectivitymatrix.DefaultFrom;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeconnectivitymatrix.connectivitymatrices.connectivitymatrix.DefaultTo;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeinfoattributes.DefaultUnderlayTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeinfoattributes.UnderlayTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodetunnelterminationattributes.DefaultLocalLinkConnectivities;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodetunnelterminationattributes.LocalLinkConnectivities;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodetunnelterminationattributes.locallinkconnectivities.DefaultLocalLinkConnectivity;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodetunnelterminationattributes.locallinkconnectivities.DefaultLocalLinkConnectivity.LocalLinkConnectivityBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.Srlg;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeAdminStatus;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeNodeId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeTopologyEventType;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.explicitroutesubobject.type.AsNumber;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.explicitroutesubobject.type.DefaultAsNumber;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.explicitroutesubobject.type.DefaultUnnumberedLink;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.explicitroutesubobject.type.Ipv4Address;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.explicitroutesubobject.type.UnnumberedLink;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.tetopologyeventtype.TeTopologyEventTypeEnum;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev20130715.ietfyangtypes.DottedQuad;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Node conversion functions.
 */
public final class NodeConverter {

    private static final String E_NULL_TE_SUBSYSTEM_TE_NODE =
            "TeSubsystem teNode object cannot be null";
    private static final String E_NULL_TE_SUBSYSTEM_TE_TUNNEL_TP =
            "TeSubsystem teTunnelTp object cannot be null";
    private static final String E_NULL_TE_SUBSYSTEM_NODE =
            "TeSubsystem ndoe object cannot be null";
    private static final String E_NULL_YANG_NODE =
            "Yang node object cannot be null";

    // no instantiation
    private NodeConverter() {
    }

    /**
     * TE Node Config object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsystemTeNode TE node object
     * @param teTopologyService teTopolog core service
     * @return TE Node Config YANG object
     */
    private static Config teNode2YangConfig(TeNode teSubsystemTeNode, TeTopologyService teTopologyService) {
        checkNotNull(teSubsystemTeNode, E_NULL_TE_SUBSYSTEM_TE_NODE);

        TeNodeAttributes.TeNodeAttributesBuilder teNodeAttributesConfigBuilder = DefaultTeNodeAttributes
                .builder();
        teNodeAttributesConfigBuilder = teNodeAttributesConfigBuilder
                .isAbstract(teSubsystemTeNode.flags()
                        .get(TeNode.BIT_ABSTRACT));

        if (teSubsystemTeNode.adminStatus() != null) {
            teNodeAttributesConfigBuilder = teNodeAttributesConfigBuilder
                    .adminStatus(EnumConverter.teSubsystem2YangAdminStatus(teSubsystemTeNode.adminStatus()));
        }

        if (teSubsystemTeNode.name() != null) {
            teNodeAttributesConfigBuilder = teNodeAttributesConfigBuilder
                    .name(DomainName.fromString(teSubsystemTeNode.name()));
        }

        if (teSubsystemTeNode.underlayTeTopologyId() != null) {
            teNodeAttributesConfigBuilder = teNodeAttributesConfigBuilder
                    .underlayTopology(teNode2YangUnderlay(teSubsystemTeNode
                            .underlayTeTopologyId(), teTopologyService));
        }

        // TODO: should we ignore this from te to yang? as we've already set yang supportingNode
        //if (teSubsystemTeNode.supportingTeNodeId() != null) {
        //}

        if (teSubsystemTeNode.connectivityMatrices() != null) {
            teNodeAttributesConfigBuilder = te2YangConnectivityMatrix(teNodeAttributesConfigBuilder,
                                                                      teSubsystemTeNode.connectivityMatrices());
        }


        Config.ConfigBuilder yangConfigBuilder = DefaultConfig.builder();
        yangConfigBuilder = yangConfigBuilder.teNodeAttributes(teNodeAttributesConfigBuilder.build());

        return yangConfigBuilder.build();
    }

    private static TeNodeAttributesBuilder
                    te2YangConnectivityMatrix(TeNodeAttributesBuilder teNodeAttributesConfigBuilder,
                                              Map<Long, ConnectivityMatrix> connectivityMatrices) {
        ConnectivityMatrixBuilder connectivityMatrixConfigBuilder = DefaultConnectivityMatrix.builder();
        ConnectivityMatricesBuilder yangConnectivityMatricesBuilder = DefaultConnectivityMatrices.builder();
        for (Map.Entry<Long, ConnectivityMatrix> teCmEntry :
            connectivityMatrices.entrySet()) {
            connectivityMatrixConfigBuilder = connectivityMatrixConfigBuilder
                    .id(teCmEntry.getKey())
                    .isAllowed(!teCmEntry.getValue().flags()
                               .get(ConnectivityMatrix.BIT_DISALLOWED))
                    .from(new DefaultFrom.FromBuilder() // TODO: for now, assuming that there is
                                                        // only one 'from', and mergingList is empty
                          .tpRef(((TeLinkId) teCmEntry.getValue().from()).value())
                                         .build())
                    .to(new DefaultTo.ToBuilder() // TODO: for now, assuming that there is only
                                                  // one item in constrainingElements list
                          .tpRef(((TeLinkId) teCmEntry.getValue().constrainingElements().get(0)).value())
                                         .build());
            if (teCmEntry.getValue().teAttributes() != null) {
                connectivityMatrixConfigBuilder = connectivityMatrixConfigBuilder
                        .teDefaultMetric(teCmEntry.getValue().teAttributes().cost())
                        .teDelayMetric(teCmEntry.getValue().teAttributes().delay());
            }

            TeSrlgs.TeSrlgsBuilder teSrlgsBuilder = DefaultTeSrlgs.builder();
            if (teCmEntry.getValue().teAttributes().srlgs() != null) {
                for (Long val : teCmEntry.getValue().teAttributes().srlgs()) {
                    Srlg srlg = new Srlg(val);
                    teSrlgsBuilder = teSrlgsBuilder.addToValue(srlg);
                }
                connectivityMatrixConfigBuilder = connectivityMatrixConfigBuilder
                        .teSrlgs(teSrlgsBuilder.build());
            }

            Underlay.UnderlayBuilder underlayBuilder = DefaultUnderlay.builder();
            PrimaryPath.PrimaryPathBuilder primaryPathBuilder = DefaultPrimaryPath.builder();

            PathElement.PathElementBuilder pathElementBuilder = DefaultPathElement.builder();
            if (teCmEntry.getValue().underlayPath() != null &&
                    teCmEntry.getValue().underlayPath().pathElements() != null &&
                    !teCmEntry.getValue().underlayPath().pathElements().isEmpty()) {
                for (org.onosproject.tetopology.management.api.link.PathElement patel : teCmEntry.getValue()
                        .underlayPath().pathElements()) {
                    pathElementBuilder = pathElementBuilder.pathElementId(patel.pathElementId());
                    if (patel.type() instanceof AsNumber) {
                        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes
                            .explicitroutesubobject.type.AsNumber
                            .AsNumberBuilder asNumberBuilder = new DefaultAsNumber.AsNumberBuilder();
                        asNumberBuilder = asNumberBuilder.asNumber(((AsNumber) patel.type()).asNumber());
                        pathElementBuilder = pathElementBuilder.type(asNumberBuilder.build());
                    } else if (patel.type() instanceof UnnumberedLink) {
                        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes
                            .explicitroutesubobject.type.UnnumberedLink
                            .UnnumberedLinkBuilder unNumberBuilder = DefaultUnnumberedLink.builder();
                        unNumberBuilder = unNumberBuilder.interfaceId(((UnnumberedLink) patel.type()).interfaceId());
                        unNumberBuilder = unNumberBuilder.routerId(IpAddress.fromString(((UnnumberedLink) patel.type())
                                                                                        .routerId().toString()));
                        pathElementBuilder = pathElementBuilder.type(unNumberBuilder.build());
                    }
                    primaryPathBuilder = primaryPathBuilder
                            .addToPathElement(pathElementBuilder.build());
                }
            }

            underlayBuilder = underlayBuilder.primaryPath(primaryPathBuilder.build());
            connectivityMatrixConfigBuilder = connectivityMatrixConfigBuilder
                    .underlay(underlayBuilder.build());

            yangConnectivityMatricesBuilder = yangConnectivityMatricesBuilder
                    .addToConnectivityMatrix(connectivityMatrixConfigBuilder.build());
        }
        teNodeAttributesConfigBuilder = teNodeAttributesConfigBuilder
                .connectivityMatrices(yangConnectivityMatricesBuilder.build());
        return teNodeAttributesConfigBuilder;
    }

    private static UnderlayTopology teNode2YangUnderlay(TeTopologyKey underlayTopology,
                                                        TeTopologyService teTopologyService) {
        UnderlayTopology.UnderlayTopologyBuilder underlayBuilder = DefaultUnderlayTopology.builder();

        underlayBuilder = underlayBuilder.networkRef(teTopologyService.networkId(underlayTopology));

        return underlayBuilder.build();
    }

    /**
     * TE Node State object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsystemTeNode TE node object
     * @param teTopologyService teTopology core service
     * @return TE Node State YANG object
     */
    private static State teNode2YangState(TeNode teSubsystemTeNode, TeTopologyService teTopologyService) {
        checkNotNull(teSubsystemTeNode, E_NULL_TE_SUBSYSTEM_TE_NODE);

        TeNodeAttributes
                        .TeNodeAttributesBuilder teNodeAttributesStateBuilder =
        DefaultTeNodeAttributes.builder()
                                        .isAbstract(teSubsystemTeNode.flags()
                                                .get(TeNode.BIT_ABSTRACT));

        if (teSubsystemTeNode.adminStatus() != null) {
            teNodeAttributesStateBuilder = teNodeAttributesStateBuilder
                    .adminStatus(EnumConverter.teSubsystem2YangAdminStatus(teSubsystemTeNode.adminStatus()));
        }

        if (teSubsystemTeNode.name() != null) {
            teNodeAttributesStateBuilder = teNodeAttributesStateBuilder
                    .name(DomainName.fromString(teSubsystemTeNode.name()));
        }

        if (teSubsystemTeNode.underlayTeTopologyId() != null) {
            teNodeAttributesStateBuilder = teNodeAttributesStateBuilder
                    .underlayTopology(teNode2YangUnderlay(teSubsystemTeNode
                            .underlayTeTopologyId(), teTopologyService));
        }

        if (teSubsystemTeNode.connectivityMatrices() != null) {
            teNodeAttributesStateBuilder = te2YangConnectivityMatrix(teNodeAttributesStateBuilder,
                                                                      teSubsystemTeNode.connectivityMatrices());
        }

        // TODO: should we ignore this from te to yang? as we've already set yang supportingNode
        //if (teSubsystemTeNode.supportingTeNodeId() != null) {
        //}

        State.StateBuilder yangStateBuilder = DefaultState.builder();
        yangStateBuilder = yangStateBuilder.teNodeAttributes(teNodeAttributesStateBuilder.build());

        if (teSubsystemTeNode.opStatus() != null) {
            yangStateBuilder = yangStateBuilder.operStatus(EnumConverter
                                                           .teSubsystem2YangOperStatus(teSubsystemTeNode.opStatus()));
        }

        if (teSubsystemTeNode.sourceTeNodeId() != null) {
            InformationSourceState.InformationSourceStateBuilder issBuilder = DefaultInformationSourceState.builder();

            Topology.TopologyBuilder topologyBuilder = DefaultTopology.builder();
            topologyBuilder = topologyBuilder.nodeRef(teTopologyService
                    .nodeKey(teSubsystemTeNode.sourceTeNodeId()).nodeId())
                    .networkRef(teTopologyService
                            .nodeKey(teSubsystemTeNode.sourceTeNodeId())
                            .networkId());

            issBuilder = issBuilder.topology(topologyBuilder.build());
            yangStateBuilder.informationSourceState(issBuilder.build());
        }

        return yangStateBuilder.build();
    }

    private static class ByteUtils {

        public static byte[] longToBytes(long x) {
            long temp = x;
            byte[] b = new byte[8];
            for (int i = 0; i < b.length; i++) {
                b[i] = new Long(temp & 0xff).byteValue();
                temp = temp >> 8;
            }
            return b;
        }

        public static long bytesToLong(byte[] bytes) {
            return (bytes[7] & 255L) << 56
                    | (bytes[6] & 255L) << 48
                    | (bytes[5] & 255L) << 40
                    | (bytes[4] & 255L) << 32
                    | (bytes[3] & 255L) << 24
                    | (bytes[2] & 255L) << 16
                    | (bytes[1] & 255L) << 8 | bytes[0] & 255L;
        }
    }

    /**
     * TE Node TunnelTerminationPoint object conversion from TE Topology subsystem to YANG.
     *
     * @param teTunnelTp TE TunnelTerminationPoint object
     * @param teTpId
     * @param teTopologyService
     * @param teNodeKey
     * @return TunnelTerminationPoint YANG object
     */
    private static TunnelTerminationPoint teSubsystem2YangTtp(
                           org.onosproject.tetopology.management.api.node
                           .TunnelTerminationPoint teTunnelTp, Long teTpId,
                           TeTopologyService teTopologyService,
                           TeNodeKey teNodeKey) {
        checkNotNull(teTunnelTp, E_NULL_TE_SUBSYSTEM_TE_TUNNEL_TP);

        TunnelTerminationPoint.TunnelTerminationPointBuilder tunnelTpBuilder =
                DefaultTunnelTerminationPoint.builder().tunnelTpId(ByteUtils.longToBytes(teTpId.longValue()));

        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
            .ietftetopology.tenodeaugment.te.tunnelterminationpoint.Config.ConfigBuilder ttpConfigBuilder =
                        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
            .ietftetopology.tenodeaugment.te.tunnelterminationpoint.DefaultConfig.builder();
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
            .ietftetopology.tenodeaugment.te.tunnelterminationpoint.State.StateBuilder ttpStateBuilder =
                        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
            .ietftetopology.tenodeaugment.te.tunnelterminationpoint.DefaultState.builder();

        // Assuming teTunnelTp only has one interLayerLock
        if (teTunnelTp.interLayerLockList() != null && !teTunnelTp.interLayerLockList().isEmpty()) {
            ttpConfigBuilder = ttpConfigBuilder.interLayerLockId(teTunnelTp.interLayerLockList().get(0));
            ttpStateBuilder  = ttpStateBuilder.interLayerLockId(teTunnelTp.interLayerLockList().get(0));
        }

        // TODO: retrieve teTunnelTp.switchingLayer() and set it to  ttpConfigBuilder and ttpStateBuilder
        // TODO: retrieve more attributes from teTunnelTp and assign to ttpConfigBuilder and ttpStateBuilder
        // For which ones we can do the conversion?

        LocalLinkConnectivities.LocalLinkConnectivitiesBuilder
            localLinkConnectivitiesBuilder = DefaultLocalLinkConnectivities.builder();
        if (teTunnelTp.localLinkConnectivityList() != null && !teTunnelTp.localLinkConnectivityList().isEmpty()) {
            for (LocalLinkConnectivity llcn : teTunnelTp.localLinkConnectivityList()) {
                // convert teLinkId to networkLinkKey
                if (llcn != null && llcn.constrainingElements() != null
                        && !llcn.constrainingElements().isEmpty()) {
                    TeLinkId telinkId = (TeLinkId) llcn.constrainingElements().get(0);
                    TeLinkTpGlobalKey globalKey = new TeLinkTpGlobalKey(teNodeKey, telinkId.value());
                    NetworkLinkKey netLinkKey = teTopologyService.linkKey(globalKey);
                    NetworkLink networkLink = teTopologyService
                            .network(netLinkKey.networkId()).links().get(netLinkKey.linkId());
                    LocalLinkConnectivityBuilder llcBuilder = DefaultLocalLinkConnectivity.builder();
                    llcBuilder = llcBuilder.linkTpRef(networkLink.source().tpId());
                    localLinkConnectivitiesBuilder.addToLocalLinkConnectivity(llcBuilder.build());
                }
            }


            ttpConfigBuilder = ttpConfigBuilder.localLinkConnectivities(localLinkConnectivitiesBuilder.build());
            ttpStateBuilder = ttpStateBuilder.localLinkConnectivities(localLinkConnectivitiesBuilder.build());
        }

        tunnelTpBuilder = tunnelTpBuilder.config(ttpConfigBuilder.build())
                                         .state(ttpStateBuilder.build());
        SupportingTunnelTerminationPoint.SupportingTunnelTerminationPointBuilder
        supportTtpBuilder = DefaultSupportingTunnelTerminationPoint.builder();
        if (teTunnelTp.supportingTtpId() != null) {
            TeTopologyKey teTopologyKey = new TeTopologyKey(teTunnelTp.supportingTtpId().providerId(),
                                                            teTunnelTp.supportingTtpId().clientId(),
                                                            teTunnelTp.supportingTtpId().topologyId());

            TeNodeKey teNodeKeySup = new TeNodeKey(teTopologyKey, teTunnelTp.supportingTtpId().teNodeId());
            NetworkNodeKey networkNodeKey = teTopologyService.nodeKey(teNodeKeySup);
            NetworkId netId = NetworkId.fromString(networkNodeKey.networkId().toString());

            supportTtpBuilder = supportTtpBuilder
                    .nodeRef(NodeId.fromString(networkNodeKey.nodeId().toString()))
                    .networkRef(netId)
                    .tunnelTpRef(ByteUtils.longToBytes(teTunnelTp.supportingTtpId().ttpId()));

            tunnelTpBuilder = tunnelTpBuilder.addToSupportingTunnelTerminationPoint(supportTtpBuilder.build());
        }


        return tunnelTpBuilder.build();
    }

    /**
     * Node object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsystem TE subsystem node
     * @param teTopologyService teTopology core service
     * @param teTopologyKey teTopologyKey
     * @return YANG node
     */
    public static Node teSubsystem2YangNode(org.onosproject.tetopology.management.api.node.NetworkNode teSubsystem,
                                            TeTopologyService teTopologyService,
                                            TeTopologyKey teTopologyKey) {
        checkNotNull(teSubsystem, E_NULL_TE_SUBSYSTEM_NODE);

        NodeId nodeId = NodeId.fromString(teSubsystem.nodeId().toString());

        Node.NodeBuilder builder = DefaultNode.builder().nodeId(nodeId);

        if (teSubsystem.supportingNodeIds() != null) {
            List<SupportingNode> sNodes = Lists.newArrayList();
            SupportingNode.SupportingNodeBuilder spNodeBuilder = DefaultSupportingNode
                    .builder();
            for (NetworkNodeKey nodeKey : teSubsystem.supportingNodeIds()) {
                sNodes.add(spNodeBuilder
                        .networkRef(NetworkId
                                .fromString(nodeKey.networkId().toString()))
                        .nodeRef(NodeId.fromString(nodeKey.nodeId().toString()))
                        .build());
            }
            builder = builder.supportingNode(sNodes);
        }

        if (teSubsystem.terminationPoints() != null) {
            AugmentedNdNode.AugmentedNdNodeBuilder tpAugmentBuilder = DefaultAugmentedNdNode
                    .builder();
            Map<KeyId, TerminationPoint> teSubsystemTeTp = teSubsystem
                    .terminationPoints();

            for (TerminationPoint teTp : teSubsystemTeTp.values()) {
                tpAugmentBuilder.addToTerminationPoint(TerminationPointConverter
                        .teSubsystem2YangTerminationPoint(teTp));
            }
            builder.addYangAugmentedInfo(tpAugmentBuilder.build(),
                                         AugmentedNdNode.class);
        }

        if (teSubsystem.teNode() != null) {
            AugmentedNwNode.AugmentedNwNodeBuilder nodeAugmentBuilder = DefaultAugmentedNwNode
                    .builder();

            TeNode teSubsystemTeNode = teSubsystem.teNode();

            TeBuilder yangTeBuilder = DefaultTe.builder();

            nodeAugmentBuilder = nodeAugmentBuilder
                    .teNodeId(TeNodeId.of(DottedQuad.of(Ip4Address
                            .valueOf((int) teSubsystemTeNode.teNodeId())
                            .toString())));

            // Set configuration data
            // Set state data
            yangTeBuilder = yangTeBuilder.config(teNode2YangConfig(teSubsystemTeNode, teTopologyService))
                                         .state(teNode2YangState(teSubsystemTeNode, teTopologyService));

            if (teSubsystemTeNode.tunnelTerminationPoints() != null) {
                for (Map.Entry<Long, org.onosproject.tetopology.management.api.node.TunnelTerminationPoint> entry :
                        teSubsystemTeNode.tunnelTerminationPoints().entrySet()) {
                    yangTeBuilder = yangTeBuilder
                            .addToTunnelTerminationPoint(teSubsystem2YangTtp(entry
                                    .getValue(), entry.getKey(), teTopologyService,
                                    new TeNodeKey(teTopologyKey, teSubsystemTeNode.teNodeId())));
                }
            }

            nodeAugmentBuilder = nodeAugmentBuilder.te(yangTeBuilder.build());
            builder.addYangAugmentedInfo(nodeAugmentBuilder.build(),
                                         AugmentedNwNode.class);
        }
        return builder.build();
    }

    /**
     * Node object conversion from YANG to TE Topology subsystem.
     *
     * @param yangNode Node in YANG model
     * @param yangNetwork YANG network
     * @param yangNetworks YANG networks
     * @return TE subsystem node
     */
    public static org.onosproject.tetopology.management.api.node.NetworkNode
            yang2TeSubsystemNode(Node yangNode, Network yangNetwork,
                                 Networks yangNetworks) {
        checkNotNull(yangNode, E_NULL_YANG_NODE);

        org.onosproject.tetopology.management.api.node.DefaultNetworkNode node;
        List<NetworkNodeKey> spNodes = null;
        TeNode teNode = null;
        Map<KeyId, TerminationPoint> tps = null;

        if (yangNode.supportingNode() != null) {
            spNodes = Lists.newArrayList();
            for (SupportingNode yangSpNode : yangNode.supportingNode()) {
                NetworkNodeKey nodeKey = new NetworkNodeKey(KeyId.keyId(yangSpNode.nodeRef().uri().toString()),
                                                            KeyId.keyId(yangSpNode.networkRef().uri().toString()));
                spNodes.add(nodeKey);
            }
        }

        if (yangNode.yangAugmentedInfoMap() != null
                && !yangNode.yangAugmentedInfoMap().isEmpty()) {

            AugmentedNdNode yangTpNodeAugment = (AugmentedNdNode) yangNode
                    .yangAugmentedInfo(AugmentedNdNode.class);
            if (yang2TeSubsystemTpNodeAugment(yangTpNodeAugment) != null) {
                tps = yang2TeSubsystemTpNodeAugment(yangTpNodeAugment);
            }

            AugmentedNwNode yangNodeAugment = (AugmentedNwNode) yangNode
                    .yangAugmentedInfo(AugmentedNwNode.class);
            if (yangNodeAugment != null && yangNodeAugment.te() != null && yangNodeAugment.teNodeId() != null) {
                TeNodeId teNodeId = yangNodeAugment.teNodeId();
                Te yangNodeAugTe = yangNodeAugment.te();
                teNode = yang2TeSubsystemNodeAugment(yangNodeAugTe, teNodeId,
                                                     yangNetwork,
                                                     yangNetworks,
                                                     yangNode,
                                                     tps);
            }
        }

        node = new org.onosproject.tetopology.management.api.node
                .DefaultNetworkNode(KeyId.keyId(yangNode.nodeId().uri().string()), spNodes, teNode, tps);
        return node;
    }

    // TODO: convert connectivity matrix from yang to te
    private static Map<Long, ConnectivityMatrix>
                    yang2TeSubsystemNodeConnectivityMatrix(String networkId,
                                                           String nodeId,
                                                           ConnectivityMatrices yangMatrices) {

        Map<Long, ConnectivityMatrix> teCmList = Maps.newHashMap();

        List<org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology
            .tenodeconnectivitymatrix.connectivitymatrices.ConnectivityMatrix>
                yangMatrix = yangMatrices.connectivityMatrix();
        for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te
                .topology.rev20170110.ietftetopology.tenodeconnectivitymatrix.connectivitymatrices.
                ConnectivityMatrix cmYang : yangMatrix) {

            ElementType from = new TeLinkId(Long.valueOf(((String) cmYang.from().tpRef()))); // is this correct?

            UnderlayAbstractPath underlayPath = null; // ignore
            List<org.onosproject.tetopology.management.api.link.PathElement> pathElements = Lists.newArrayList();
            Boolean loose = false;
            long longTeNodeId = TeConstants.NIL_LONG_VALUE;
            if (cmYang != null && cmYang.underlay() != null && cmYang.underlay().primaryPath() != null
                    && cmYang.underlay().primaryPath().pathElement() != null
                    && !cmYang.underlay().primaryPath().pathElement().isEmpty()) {
                for (PathElement yangPathEl : cmYang.underlay().primaryPath().pathElement()) {
                    ElementType type = null;
                    if (yangPathEl.type() instanceof UnnumberedLink) {
                        String rS = ((UnnumberedLink) (yangPathEl.type())).routerId().toString();
                        org.onlab.packet.IpAddress routerId = org.onlab.packet.IpAddress.valueOf(rS);
                        long interfaceId = ((UnnumberedLink) yangPathEl.type()).interfaceId();
                        type = new org.onosproject.tetopology.management.api.link.UnnumberedLink(routerId, interfaceId);
                        longTeNodeId = Long.valueOf(((UnnumberedLink) yangPathEl.type()).routerId().toString());
                    } else if (yangPathEl.type() instanceof Ipv4Address) {
                        short v4PrefixLength = ((Ipv4Address) yangPathEl.type())
                                .v4PrefixLength();

                        Ip4Address v4Address = Ip4Address
                                .valueOf(((Ipv4Address) yangPathEl.type())
                                        .v4Address().string());

                        loose = ((Ipv4Address) yangPathEl.type()).v4Loose();
                        type = new TeIpv4(v4Address, v4PrefixLength);
                    }
                    org.onosproject.tetopology.management.api.link.PathElement
                        patel = new org.onosproject.tetopology.management.api.link
                                    .PathElement(yangPathEl.pathElementId(),
                                                 longTeNodeId,
                                                 type,
                                                 loose);
                    pathElements.add(patel);
                }
            }
            underlayPath = new UnderlayAbstractPath(pathElements, loose);

            List<ElementType> mergingList = Lists.newArrayList(); // empty merging list for now

            List<ElementType> constrainingElements = Lists.newArrayList();
            ElementType to = new TeLinkId(Long.valueOf(((String) cmYang.to().tpRef()))); // is this correct?
            constrainingElements.add(to);

            BitSet flags = new BitSet(); // what are the flags in cmYang?

            List<Long> srlgs = Lists.newArrayList();
            if (cmYang.teSrlgs() != null) {
                for (Srlg srlg : cmYang.teSrlgs().value()) {
                    srlgs.add(srlg.uint32());
                }
            }
            TePathAttributes teAttributes = new
                    TePathAttributes(cmYang.teDefaultMetric(),
                                     cmYang.teDelayMetric(),
                                     srlgs);
            ConnectivityMatrix coreCm = new ConnectivityMatrix(cmYang.id(),
                                                               from,
                                                               mergingList,
                                                               constrainingElements,
                                                               flags,
                                                               teAttributes,
                                                               underlayPath);

            teCmList.put(cmYang.id(), coreCm);
        }

        return teCmList;
    }

    private static TeTopologyKey yang2TeSubsystemNodeUnderlayTopology(UnderlayTopology ut, Networks yangNetworks) {
        TeTopologyKey tetopokey = LinkConverter.findTopologyId(yangNetworks,
                                                               ut.networkRef());
        return tetopokey;
    }

    // TODO: retrieve the details of tunnel termiantion points from yang to te
    private static Map<Long, org.onosproject.tetopology.management.api.node.
                      TunnelTerminationPoint> yang2TeSubsystemTtp(List<TunnelTerminationPoint> ttps, Node yangNode,
                                                                  Networks yangNetworks) {
        Map<Long, org.onosproject.tetopology.management.api.node.TunnelTerminationPoint> ttpsMap = Maps
                .newHashMap();
        for (TunnelTerminationPoint ttpYang : ttps) {

            SwitchingType switchingLayer = null; // how to find switching type?
            EncodingType encodingLayer = null; // TODO: find proper encoding type from ttpYang.config().encoding();
            BitSet flags = new BitSet(); // how to set flags?
            List<Long> interLayerLockList = Lists.newArrayList();

            if (ttpYang.config() != null) {
                interLayerLockList.add(ttpYang.config().interLayerLockId()); // interLayerLock in yang is not a list
            }

            List<LocalLinkConnectivity> localLinkConnectivityList = Lists.newArrayList();
            // FIXME: once new yang model is used, we can make llc
            ElementType elt = null;
            List<ElementType> eltList = Lists.newArrayList();
            if (ttpYang.config() != null &&
                    ttpYang.config().localLinkConnectivities() != null &&
                    CollectionUtils.isNotEmpty(ttpYang.config().localLinkConnectivities().localLinkConnectivity())) {
                for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
                        .ietftetopology.tenodetunnelterminationattributes.locallinkconnectivities
                        .LocalLinkConnectivity yangLlc : ttpYang.config()
                                                            .localLinkConnectivities().localLinkConnectivity()) {
                    if (MapUtils.isNotEmpty(yangNode.yangAugmentedInfoMap())) {
                        AugmentedNdNode yangTpNodeAugment = (AugmentedNdNode) yangNode
                                .yangAugmentedInfo(AugmentedNdNode.class);
                        for (org.onosproject.yang.gen.v1.urn.ietf.params.xml
                                .ns.yang.ietf.network.topology.rev20151208.ietfnetworktopology
                                .networks.network.node.augmentedndnode
                                .TerminationPoint tpItem : yangTpNodeAugment.terminationPoint()) {
                            if (tpItem.tpId().uri().string().equals(yangLlc.linkTpRef().toString())) {
                                if (tpItem.yangAugmentedInfoMap() != null
                                        && !tpItem.yangAugmentedInfoMap().isEmpty()) {
                                    AugmentedNtTerminationPoint yangTpAugment =
                                            (AugmentedNtTerminationPoint) tpItem
                                            .yangAugmentedInfo(AugmentedNtTerminationPoint.class);
                                    if (yangTpAugment.teTpId() != null) {
                                        elt = new TeLinkId(Long.valueOf(yangTpAugment.teTpId().toString()));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    eltList.add(elt);
//                    tercap.linkTp().toString() //tpId -> tp -> te-tp-id (long)
                }
            }

            TePathAttributes teAttributes = null; // how to find these
                                                  // attributes from ttpYang?
            UnderlayAbstractPath underlayPath = null; // how to find underlayAbstractPath from ttpYang?
            LocalLinkConnectivity llc = new LocalLinkConnectivity(eltList,
                                                                  flags,
                                                                  teAttributes,
                                                                  underlayPath);
            localLinkConnectivityList.add(llc);

            float[] availAdaptBandwidth = null; // how to find availableBandwidth?

            TeTopologyKey teTopologyKey = null;

            Object networkRefObj = null;
            NetworkId networkId = null;
            if (ttpYang != null && ttpYang.supportingTunnelTerminationPoint() != null
                    && !ttpYang.supportingTunnelTerminationPoint().isEmpty()
                    && ttpYang.supportingTunnelTerminationPoint().get(0) != null) {
                networkRefObj = ttpYang.supportingTunnelTerminationPoint()
                        .get(0).networkRef();
            }
            if (networkRefObj != null) {
                teTopologyKey = LinkConverter.findTopologyId(yangNetworks,
                                                             networkRefObj);
                networkId = NetworkId.fromString((String) networkRefObj);
            }

            Network teNetworkFound = null;
            if (yangNetworks.network() != null
                    && !yangNetworks.network().isEmpty()
                    && networkId != null) {
                for (Network ynetItem : yangNetworks.network()) {
                    if (ynetItem.networkId() != null) {
                        if (ynetItem.networkId().equals(networkId)) {
                            teNetworkFound = ynetItem;
                            break;
                        }
                    }
                }
            }
            TeNodeId teNodeIdSupport = null;
            if (teNetworkFound != null
                    && ttpYang != null
                    && ttpYang.supportingTunnelTerminationPoint() != null
                    && ttpYang.supportingTunnelTerminationPoint().get(0) != null) {

                String s = ((String) ttpYang.supportingTunnelTerminationPoint().get(0).nodeRef());
                int integ = Integer.valueOf(s);
                NodeId nodeId = NodeId.fromString(DottedQuad.of(Ip4Address.valueOf(integ).toString()).toString());
                teNodeIdSupport = LinkConverter
                        .findTeNodeId(teNetworkFound,
                                      nodeId);
            }

            long tenIdLong = -1;
            if (teNodeIdSupport != null) {
                tenIdLong = Ip4Address
                        .valueOf(teNodeIdSupport.dottedQuad().string()).toInt();
            }

            TeNodeKey teNodeKey = null;
            if (teTopologyKey != null && tenIdLong != -1) {
                teNodeKey = new TeNodeKey(teTopologyKey, tenIdLong);
            }
            TtpKey supportTtpKey = null;
            if (teNodeKey != null && ttpYang != null
                    && ttpYang.supportingTunnelTerminationPoint() != null
                    && !ttpYang.supportingTunnelTerminationPoint().isEmpty()
                    && ttpYang.supportingTunnelTerminationPoint().get(0) != null) {
                supportTtpKey = new TtpKey(teNodeKey,
                                           ByteUtils.bytesToLong((byte[]) ttpYang
                                                                        .supportingTunnelTerminationPoint().get(0)
                                                                        .tunnelTpRef()));
            }

            org.onosproject.tetopology.management.api.node.
                TunnelTerminationPoint ttpTe = new
                org.onosproject.tetopology.management.api.node.
                DefaultTunnelTerminationPoint(ByteUtils.bytesToLong(ttpYang.tunnelTpId()),
                                              switchingLayer,
                                              encodingLayer,
                                              flags,
                                              interLayerLockList,
                                              localLinkConnectivityList,
                                              availAdaptBandwidth,
                                              supportTtpKey);

            ttpsMap.put(ByteUtils.bytesToLong(ttpYang.tunnelTpId()), ttpTe);
        }

        return ttpsMap;
    }

    private static TeNode yang2TeSubsystemNodeAugment(Te yangNodeAugTe,
                                                      TeNodeId teNodeId,
                                                      Network yangNetwork,
                                                      Networks yangNetworks,
                                                      Node yangNode,
                                                      Map<KeyId, TerminationPoint> teTps) {


        NodeId yangNodeId = yangNode.nodeId();

        NetworkId yangNetworkId = yangNetwork.networkId();

        long teNodeIdLong = Ip4Address.valueOf(teNodeId.dottedQuad().string()).toInt();

        TeTopologyKey underlayTopologyIdId = null;

        // FIXME: yang has a list of supporting nodes, but TeNode only has one
        // supportTeNodeId. How ro retrieve providerId, clientId, topologyId, teNodeId?
        TeNodeKey supportTeNodeId = null;
//        supportTeNodeId = new TeNodeKey(providerId, clientId, topologyId, teNodeId)
//        yangSupportNodes.get(0).

        TeNodeKey sourceTeNodeId = null; //ignore
        CommonNodeData teData = null;
        Map<Long, ConnectivityMatrix> connMatrices = null;
        Map<Long, org.onosproject.tetopology.management.api.node.TunnelTerminationPoint> ttps = null;
        List<Long> teLinkIds = Lists.newArrayList();
        List<Long> teTpIds = Lists.newArrayList();

        // ********************************************** to find teLinkIds
        // teLinkIds should be supposed to get from YANG Link and TP data.
        // For now, assuming each teTp is sourcing a TE link .
        if (MapUtils.isNotEmpty(teTps)) {
            for (Map.Entry<KeyId, TerminationPoint> entry : teTps.entrySet()) {
                if (entry.getValue().teTpId() != null) {
                    teTpIds.add(entry.getValue().teTpId());
                    teLinkIds.add(entry.getValue().teTpId());
                }
            }
        }
        // ********************************************** to find teTpIds
//        if (yangNode.yangAugmentedInfoMap() != null
//                && !yangNode.yangAugmentedInfoMap().isEmpty()) {
//
//            AugmentedNdNode yangTpNodeAugment = (AugmentedNdNode) yangNode
//                    .yangAugmentedInfo(AugmentedNdNode.class);
//
//            if (yangTpNodeAugment.terminationPoint() != null) {
//                for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
//                        .rev20151208.ietfnetworktopology.networks.network.node.augmentedndnode.TerminationPoint
//                        yangTpnode : yangTpNodeAugment.terminationPoint()) {
//                    teTpIds.add(Long.valueOf(yangTpnode.tpId().uri().string()));
//                }
//            }
//        }
        // **********************************************

        Config ynodeAugCfg = yangNodeAugTe.config();

        if (ynodeAugCfg != null) {
            TeNodeAttributes teNodeAttr = ynodeAugCfg.teNodeAttributes();
            if (teNodeAttr != null) {

                if (teNodeAttr.underlayTopology() != null) {

                    underlayTopologyIdId = yang2TeSubsystemNodeUnderlayTopology(teNodeAttr
                            .underlayTopology(), yangNetworks);
                }
                BitSet flags = new BitSet();
                if (teNodeAttr.isAbstract()) {
                    flags.set(TeNode.BIT_ABSTRACT);
                }
                teData = new CommonNodeData(
                                            null, // teNodeAttr.name().string(),
                                            EnumConverter.yang2TeSubsystemAdminStatus(teNodeAttr.adminStatus()),
                                            EnumConverter.yang2TeSubsystemOpStatus(yangNodeAugTe.state().operStatus()),
                                            flags);

//                if (teNodeAttr.connectivityMatrix() != null) {
                if (teNodeAttr.connectivityMatrices() != null) {
                    connMatrices = yang2TeSubsystemNodeConnectivityMatrix(yangNetworkId.uri().toString(),
                                                                    yangNodeId.uri().toString(),
                                                                    teNodeAttr.connectivityMatrices());
                }

            }
        }

        if (yangNodeAugTe.tunnelTerminationPoint() != null) {
            ttps = yang2TeSubsystemTtp(yangNodeAugTe.tunnelTerminationPoint(),
                                       yangNode,
                                       yangNetworks);
        }

        TeNode teNode = new DefaultTeNode(teNodeIdLong,
                                          underlayTopologyIdId,
                                          supportTeNodeId,
                                          sourceTeNodeId,
                                          teData,
                                          connMatrices,
                                          teLinkIds,
                                          ttps,
                                          teTpIds);
        return teNode;
    }

    private static Map<KeyId, TerminationPoint> yang2TeSubsystemTpNodeAugment(AugmentedNdNode yangTpNodeAugment) {
        Map<KeyId, TerminationPoint> tps;
        if (yangTpNodeAugment.terminationPoint() != null) {
            tps = Maps.newHashMap();
            for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                    .rev20151208.ietfnetworktopology.networks.network.node.augmentedndnode.TerminationPoint
                    yangTpnode : yangTpNodeAugment.terminationPoint()) {
                tps.put(KeyId.keyId(yangTpnode.tpId().uri().toString()),
                        TerminationPointConverter.yang2teSubsystemTerminationPoint(yangTpnode));
            }
            return tps;
        }
        return null;
    }

    /**
     * Converts a TE Topology node event from the data format used in the core
     * to its corresponding YANG Object (YO) format.
     *
     * @param eventType Node event type
     * @param eventData  TE Topology node event data
     * @return YANG Object converted from nodeData
     */
    public static TeNodeEvent teNetworkNode2yangTeNodeEvent(TeTopologyEventTypeEnum eventType,
                                                            NetworkNodeEventSubject eventData) {
        TeNodeEvent.TeNodeEventBuilder builder = new DefaultTeNodeEvent.TeNodeEventBuilder();

        TeTopologyEventType yangEventType = new TeTopologyEventType(eventType);
        builder.eventType(yangEventType);

        NetworkId newtorkId = NetworkId.fromString(eventData.key().networkId().toString());
        builder.networkRef(newtorkId);
        NodeId nodeId = NodeId.fromString(eventData.key().nodeId().toString());
        builder.nodeRef(nodeId);

        NetworkNode node = eventData.neworkNode();
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.
                rev20170110.ietftetopology.tenodeconfigattributesnotification.
                TeNodeAttributes teNodeAttributes = node == null ? null
                                                                 : teNode2YangTeNodeAttributes(node.teNode());
        builder.teNodeAttributes(teNodeAttributes);

        return builder.build();
    }

    private static org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.
            ietf.te.topology.rev20170110.ietftetopology.
            tenodeconfigattributesnotification.
            TeNodeAttributes teNode2YangTeNodeAttributes(TeNode teNode) {

        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.
                rev20170110.ietftetopology.tenodeconfigattributesnotification.
                TeNodeAttributes.TeNodeAttributesBuilder attrBuilder =
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.
                                rev20170110.ietftetopology.tenodeconfigattributesnotification.
                        DefaultTeNodeAttributes.builder();

        if (teNode.connectivityMatrices() != null) {

            org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology
            .tenodeconnectivitymatrixabs.DefaultConnectivityMatrix
            .ConnectivityMatrixBuilder connectivityMatrixConfigBuilder = org.onosproject.yang.gen.v1.urn.ietf.
                            params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tenodeconnectivitymatrixabs.
            DefaultConnectivityMatrix.builder();
            for (Map.Entry<Long, ConnectivityMatrix> teCmEntry :
                teNode.connectivityMatrices().entrySet()) {
                connectivityMatrixConfigBuilder = connectivityMatrixConfigBuilder
                        .id(teCmEntry.getKey())
                        .isAllowed(!teCmEntry.getValue().flags()
                                   .get(ConnectivityMatrix.BIT_DISALLOWED))
                        .from(new org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
                              .ietftetopology.tenodeconnectivitymatrixabs.connectivitymatrix.DefaultFrom
                              .FromBuilder() // TODO: for now, assuming that there is
                                                            // only one 'from', and mergingList is empty
                              .tpRef(teCmEntry.getValue().from())
                                             .build())
                        .to(new org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
                            .ietftetopology.tenodeconnectivitymatrixabs.connectivitymatrix
                            .DefaultTo.ToBuilder() // TODO: for now, assuming that there is only
                                                      // one item in constrainingElements list
                              .tpRef(teCmEntry.getValue().constrainingElements().get(0))
                                             .build());
                attrBuilder = attrBuilder
                        .addToConnectivityMatrix(connectivityMatrixConfigBuilder
                                .build());
            }
        }

        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.
                rev20170110.ietftetopology.tenodeconfigattributesnotification.
                TeNodeAttributes teNodeAttributes = attrBuilder.build();

        return teNodeAttributes;
    }

    public static NetworkNodeKey yangNodeEvent2NetworkNodeKey(TeNodeEvent yangNodeEvent) {
        NetworkId networkRef = NetworkId.fromString(yangNodeEvent.networkRef().toString());
        NodeId nodeRef = NodeId.fromString(yangNodeEvent.nodeRef().toString());
        KeyId networkId = KeyId.keyId(networkRef.uri().toString());
        KeyId nodeId = KeyId.keyId(nodeRef.uri().toString());

        NetworkNodeKey networkNodeKey = new NetworkNodeKey(networkId, nodeId);

        return networkNodeKey;
    }

    /**
     * Converts YangNode event to NetworkNode.
     *
     * @param yangNodeEvent yangNodeEvent
     * @param teTopologyService teTopologyService
     * @return NetworkNode
     */
    public static NetworkNode yangNodeEvent2NetworkNode(TeNodeEvent yangNodeEvent,
                                                        TeTopologyService teTopologyService) {
        KeyId networkNodeId = yangNodeEvent2NetworkNodeKey(yangNodeEvent).nodeId();

        org.onosproject.tetopology.management.api.Network network = teTopologyService.network(
                yangNodeEvent2NetworkNodeKey(yangNodeEvent).networkId());
        if (network == null) {
            return null;
        }

        NetworkNode networkNode = network.node(networkNodeId);
        if (networkNode == null) {
            return null;
        }

        List<NetworkNodeKey> supportingNodeIds = networkNode.supportingNodeIds();
        Map<KeyId, TerminationPoint> tps = networkNode.terminationPoints();

        TeNode teNode = networkNode.teNode();
        if (teNode == null) {
            return null;
        }

        TeNode updatedTeNode = yangNodeEvent2TeNode(yangNodeEvent, teNode);

        NetworkNode updatedNetworkNode = new DefaultNetworkNode(networkNodeId, supportingNodeIds, updatedTeNode, tps);

        return updatedNetworkNode;
    }

    private static TeNode yangNodeEvent2TeNode(TeNodeEvent yangNodeEvent, TeNode oldTeNode) {

        long teNodeId = oldTeNode.teNodeId();
        TeTopologyKey underlayTopoId = oldTeNode.underlayTeTopologyId();
        TeNodeKey supportTeNodeId = oldTeNode.sourceTeNodeId();
        TeNodeKey sourceTeNodeId = oldTeNode.sourceTeNodeId();
        Map<Long, ConnectivityMatrix> connMatrices = oldTeNode.connectivityMatrices();
        List<Long> teLinkIds = oldTeNode.teLinkIds();
        Map<Long, org.onosproject.tetopology.management.
                api.node.TunnelTerminationPoint> ttps = oldTeNode.tunnelTerminationPoints();
        List<Long> teTpIds = oldTeNode.teLinkIds();
        String name = oldTeNode.name();
        TeStatus adminStatus = oldTeNode.adminStatus();
        TeStatus opStatus = oldTeNode.opStatus();
        BitSet flags = oldTeNode.flags();

        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology
                .tenodeconfigattributesnotification
                .TeNodeAttributes yangTeNodeAttrs = yangNodeEvent.teNodeAttributes();

        if (yangTeNodeAttrs != null) {
            TeAdminStatus yangAdminStatus = yangTeNodeAttrs.adminStatus();
            if (yangAdminStatus != null) {
                adminStatus = EnumConverter.yang2TeSubsystemAdminStatus(yangAdminStatus);
            }

            BitSet yangFlags = yangTeNodeAttrs.selectLeafFlags();
            if (yangFlags != null) {
                flags = yangFlags;
            }

            List<org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.
                    ietf.te.topology.rev20170110.ietftetopology
                    .tenodeconnectivitymatrixabs.ConnectivityMatrix> yangConnMatrices = yangTeNodeAttrs
                    .connectivityMatrix();
            if (yangConnMatrices != null) {
                for (org.onosproject.yang.gen.v1.
                        urn.ietf.params.xml.ns.yang.
                        ietf.te.topology
                        .rev20170110.ietftetopology
                        .tenodeconnectivitymatrixabs
                        .ConnectivityMatrix yangConnMatrix : yangConnMatrices) {
                    Long cmId = new Long(yangConnMatrix.id());
                    ConnectivityMatrix oldConnMatrix = connMatrices.get(new Long(yangConnMatrix.id()));
                    if (oldConnMatrix != null) {
                        ConnectivityMatrix newConnMatrix = yangNodeEvent2TeConnectivityMatrix(yangConnMatrix,
                                                                                              oldConnMatrix);
                        connMatrices.remove(cmId);
                        connMatrices.put(cmId, newConnMatrix);
                    }
                }
            }
        }

        CommonNodeData teData = new CommonNodeData(name, adminStatus, opStatus, flags);
        TeNode updatedTeNode = new DefaultTeNode(teNodeId, underlayTopoId, supportTeNodeId, sourceTeNodeId, teData,
                                                 connMatrices, teLinkIds, ttps, teTpIds);

        return updatedTeNode;
    }

    private static ConnectivityMatrix yangNodeEvent2TeConnectivityMatrix(org.onosproject.yang.gen.v1.
                                                                         urn.ietf.params.xml.ns.yang.
                                                                         ietf.te.topology
            .rev20170110.ietftetopology
                                                                         .tenodeconnectivitymatrixabs
                                                                         .ConnectivityMatrix yangConnMatrix,
                                                                 ConnectivityMatrix oldTeConnMatrix) {

        long id = yangConnMatrix.id();
        ElementType from = new TeLinkId((long) (yangConnMatrix.from().tpRef()));
        UnderlayAbstractPath underlayPath = null;
        List<ElementType> mergingList = Lists.newArrayList();

        List<ElementType> constrainingElements = Lists.newArrayList();
        ElementType to = new TeLinkId((long) (yangConnMatrix.to().tpRef()));
        constrainingElements.add(to);

        BitSet flags = oldTeConnMatrix.flags();

        TePathAttributes teAttributes = oldTeConnMatrix.teAttributes();

        ConnectivityMatrix updatedConnMatrix = new ConnectivityMatrix(id,
                                                                      from,
                                                                      mergingList,
                                                                      constrainingElements,
                                                                      flags,
                                                                      teAttributes,
                                                                      underlayPath);
        return updatedConnMatrix;
    }
}
