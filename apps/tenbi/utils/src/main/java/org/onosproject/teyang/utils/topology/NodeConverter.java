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

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.onlab.packet.Ip4Address;
import org.onosproject.tetopology.management.api.EncodingType;
import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.SwitchingType;
import org.onosproject.tetopology.management.api.TeStatus;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.TeTopologyService;
import org.onosproject.tetopology.management.api.link.ElementType;
import org.onosproject.tetopology.management.api.link.TeLinkId;
import org.onosproject.tetopology.management.api.link.TePathAttributes;
import org.onosproject.tetopology.management.api.link.UnderlayAbstractPath;
import org.onosproject.tetopology.management.api.node.CommonNodeData;
import org.onosproject.tetopology.management.api.node.ConnectivityMatrix;
import org.onosproject.tetopology.management.api.node.DefaultNetworkNode;
import org.onosproject.tetopology.management.api.node.DefaultTeNode;
import org.onosproject.tetopology.management.api.node.LocalLinkConnectivity;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev20130715.ietfinettypes.DomainName;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NetworkId;
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
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.ietfnetworktopology.networks.network.AugmentedNdNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.ietfnetworktopology.networks.network.augmentedndnetwork.Link;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.node.AugmentedNdNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.node.DefaultAugmentedNdNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.DefaultTeNodeEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.TeNodeEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.informationsourceattributes.DefaultInformationSourceState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.informationsourceattributes.InformationSourceState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.informationsourceattributes.informationsourcestate.DefaultTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.informationsourceattributes.informationsourcestate.Topology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.node.AugmentedNwNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.node.DefaultAugmentedNwNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeaugment.DefaultTe;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeaugment.DefaultTe.TeBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeaugment.Te;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeaugment.te.Config;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeaugment.te.DefaultConfig;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeaugment.te.DefaultState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeaugment.te.DefaultTunnelTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeaugment.te.State;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeaugment.te.TunnelTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeconfigattributes.DefaultTeNodeAttributes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeconfigattributes.TeNodeAttributes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeconfigattributes.TeNodeAttributes.TeNodeAttributesBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeconnectivitymatrix.DefaultConnectivityMatrix;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeconnectivitymatrix.DefaultConnectivityMatrix.ConnectivityMatrixBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeconnectivitymatrix.connectivitymatrix.DefaultFrom;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeconnectivitymatrix.connectivitymatrix.DefaultTo;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeinfoattributes.DefaultUnderlayTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeinfoattributes.UnderlayTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodetunnelterminationcapability.DefaultTerminationCapability;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodetunnelterminationcapability.TerminationCapability;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.Srlg;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeAdminStatus;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeNodeId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeTopologyEventType;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeTopologyId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.tetopologyeventtype.TeTopologyEventTypeEnum;

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
     * @return TE Node Config YANG object
     */
    private static Config teNode2YangConfig(TeNode teSubsystemTeNode) {
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
                            .underlayTeTopologyId()));
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
        for (Map.Entry<Long, ConnectivityMatrix> teCmEntry :
            connectivityMatrices.entrySet()) {
            connectivityMatrixConfigBuilder = connectivityMatrixConfigBuilder
                    .id(teCmEntry.getKey())
                    .isAllowed(!teCmEntry.getValue().flags()
                               .get(ConnectivityMatrix.BIT_DISALLOWED))
                    .from(new DefaultFrom.FromBuilder() // TODO: for now, assuming that there is
                                                        // only one 'from', and mergingList is empty
                          .tpRef(teCmEntry.getValue().from())
                                         .build())
                    .to(new DefaultTo.ToBuilder() // TODO: for now, assuming that there is only
                                                  // one item in constrainingElements list
                          .tpRef(teCmEntry.getValue().constrainingElements().get(0))
                                         .build());
            teNodeAttributesConfigBuilder = teNodeAttributesConfigBuilder
                    .addToConnectivityMatrix(connectivityMatrixConfigBuilder
                            .build());
        }
        return teNodeAttributesConfigBuilder;
    }

    private static UnderlayTopology teNode2YangUnderlay(TeTopologyKey underlayTopology) {
        UnderlayTopology.UnderlayTopologyBuilder underlayBuilder = DefaultUnderlayTopology
                .builder()
                .teTopologyIdRef(TeTopologyId
                        .fromString(String.valueOf(underlayTopology.topologyId())))
                .clientIdRef(underlayTopology.clientId())
                .providerIdRef(underlayTopology.providerId());
        // TODO: find networkId from the service
//                .networkIdRef(networkIdRef)
        return underlayBuilder.build();
    }

    /**
     * TE Node State object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsystemTeNode TE node object
     * @return TE Node State YANG object
     */
    private static State teNode2YangState(TeNode teSubsystemTeNode) {
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
                            .underlayTeTopologyId()));
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
            topologyBuilder =
                    topologyBuilder.clientIdRef(teSubsystemTeNode.sourceTeNodeId().clientId())
                                   .providerIdRef(teSubsystemTeNode.sourceTeNodeId().providerId())
                                   // is this correct? Why not sourceTeNodeId().teTopologyKey()?
                                   .teTopologyIdRef(teSubsystemTeNode
                                                    .sourceTeNodeId().topologyId());
            issBuilder = issBuilder.topology(topologyBuilder.build());
            yangStateBuilder.informationSourceState(issBuilder.build());
        }

        return yangStateBuilder.build();
    }

    private static class ByteUtils {
        private static ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

        public static byte[] longToBytes(long x) {
            buffer.putLong(0, x);
            return buffer.array();
        }

        public static long bytesToLong(byte[] bytes) {
            buffer.put(bytes, 0, bytes.length);
            buffer.flip(); // need flip
            return buffer.getLong();
        }
    }

    /**
     * TE Node TunnelTerminationPoint object conversion from TE Topology subsystem to YANG.
     *
     * @param teTunnelTp TE TunnelTerminationPoint object
     * @return TunnelTerminationPoint YANG object
     */
    private static TunnelTerminationPoint teSubsystem2YangTtp(
                           org.onosproject.tetopology.management.api.node
                           .TunnelTerminationPoint teTunnelTp, Long teTpId) {
        checkNotNull(teTunnelTp, E_NULL_TE_SUBSYSTEM_TE_TUNNEL_TP);

        TunnelTerminationPoint.TunnelTerminationPointBuilder tunnelTpBuilder =
                DefaultTunnelTerminationPoint.builder().tunnelTpId(ByteUtils.longToBytes(teTpId.longValue()));

        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
            .ietftetopology.tenodeaugment.te.tunnelterminationpoint.Config.ConfigBuilder ttpConfigBuilder =
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
            .ietftetopology.tenodeaugment.te.tunnelterminationpoint.DefaultConfig.builder();
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
            .ietftetopology.tenodeaugment.te.tunnelterminationpoint.State.StateBuilder ttpStateBuilder =
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
            .ietftetopology.tenodeaugment.te.tunnelterminationpoint.DefaultState.builder();

        // Assuming teTunnelTp only has one interLayerLock
        if (teTunnelTp.interLayerLockList() != null && !teTunnelTp.interLayerLockList().isEmpty()) {
            ttpConfigBuilder = ttpConfigBuilder.interLayerLockId(teTunnelTp.interLayerLockList().get(0));
            ttpStateBuilder  = ttpStateBuilder.interLayerLockId(teTunnelTp.interLayerLockList().get(0));
        }

        TerminationCapability.TerminationCapabilityBuilder
                            tcapConfigBuilder = DefaultTerminationCapability.builder();
        // TODO: retrieve more attributes from teTunnelTp and assign to tcapConfigBuilder.
        // For which ones we can do the conversion?
        // FIXME: once new yang model is used, we can make llc from teTunnelTp.localLinkConnectivityList()
        ttpConfigBuilder = ttpConfigBuilder.addToTerminationCapability(tcapConfigBuilder.build());

        TerminationCapability.TerminationCapabilityBuilder tcapStateBuilder =
                DefaultTerminationCapability.builder();
        // TODO: retrieve more attributes from teTunnelTp and assign to tcapStateBuilder
        // For which ones we can do the conversion?
        ttpStateBuilder = ttpStateBuilder.addToTerminationCapability(tcapStateBuilder.build());

        tunnelTpBuilder = tunnelTpBuilder.config(ttpConfigBuilder.build())
                                         .state(ttpStateBuilder.build());

        return tunnelTpBuilder.build();
    }

    /**
     * Node object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsystem TE subsystem node
     * @return YANG node
     */
    public static Node teSubsystem2YangNode(org.onosproject.tetopology.management.api.node.NetworkNode teSubsystem) {
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

            yangTeBuilder = yangTeBuilder.teNodeId(TeNodeId
                    .fromString(String.valueOf(teSubsystemTeNode.teNodeId())));

            // Set configuration data
            // Set state data
            yangTeBuilder = yangTeBuilder.config(teNode2YangConfig(teSubsystemTeNode))
                                         .state(teNode2YangState(teSubsystemTeNode));

            if (teSubsystemTeNode.tunnelTerminationPoints() != null) {
                for (Map.Entry<Long, org.onosproject.tetopology.management.api.node.TunnelTerminationPoint> entry :
                        teSubsystemTeNode.tunnelTerminationPoints().entrySet()) {
                    yangTeBuilder = yangTeBuilder
                            .addToTunnelTerminationPoint(teSubsystem2YangTtp(entry
                                    .getValue(), entry.getKey()));
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
     * @return TE subsystem node
     */
    public static org.onosproject.tetopology.management.api.node.NetworkNode
                    yang2TeSubsystemNode(Node yangNode, Network yangNetwork) {
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
            if (yangNodeAugment != null && yangNodeAugment.te() != null && yangNodeAugment.te().teNodeId() != null) {
                Te yangNodeAugTe = yangNodeAugment.te();
                teNode = yang2TeSubsystemNodeAugment(yangNodeAugTe,
                                                     yangNetwork,
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
                                                           List<org.onosproject.yang.gen.v1.urn.ietf
                                                           .params.xml.ns.yang.ietf.te.topology.rev20160708
                                                           .ietftetopology.tenodeconnectivitymatrix.
                                                           ConnectivityMatrix> yangMatrix) {

        Map<Long, ConnectivityMatrix> teCmList = Maps.newHashMap();


        for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te
                .topology.rev20160708.ietftetopology.tenodeconnectivitymatrix.
                ConnectivityMatrix cmYang : yangMatrix) {

            ElementType from = new TeLinkId((long) cmYang.from().tpRef()); // is this correct?

            UnderlayAbstractPath underlayPath = null; // ignore

            List<ElementType> mergingList = Lists.newArrayList(); // empty merging list for now

            List<ElementType> constrainingElements = Lists.newArrayList();
            ElementType to = new TeLinkId((long) cmYang.to().tpRef()); // is this correct?
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
                                     cmYang.performanceMetric().measurement().unidirectionalDelay(),
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

    private static TeTopologyKey yang2TeSubsystemNodeUnderlayTopology(UnderlayTopology ut) {
        TeTopologyKey tetopokey = new TeTopologyKey((Long) ut.providerIdRef(),
                                                    (Long) ut.clientIdRef(),
                                                    (Long) ut.teTopologyIdRef());
        return tetopokey;
    }

    // TODO: retrieve the details of tunnel termiantion points from yang to te
    private static Map<Long, org.onosproject.tetopology.management.api.node.
                      TunnelTerminationPoint> yang2TeSubsystemTtp(List<TunnelTerminationPoint> ttps) {
        Map<Long, org.onosproject.tetopology.management.api.node.TunnelTerminationPoint> ttpsMap = Maps
                .newHashMap();
        for (TunnelTerminationPoint ttpYang : ttps) {

            SwitchingType switchingLayer = null; // how to find switching type?
            EncodingType encodingLayer = null; // TODO: find proper encoding type from ttpYang.config().encoding();
            BitSet flags = new BitSet(); // how to set flags?
            List<Long> interLayerLockList = Lists.newArrayList();
            interLayerLockList.add(ttpYang.config().interLayerLockId()); // interLayerLock in yang is not a list

            List<LocalLinkConnectivity> localLinkConnectivityList = Lists.newArrayList();
            // FIXME: once new yang model is used, we can make llc
//            LocalLinkConnectivity llc = new LocalLinkConnectivity(constrainingElements,
//                                                                  flags,
//                                                                  teAttributes,
//                                                                  underlayPath)

            float[] availAdaptBandwidth = null; // how to find availableBandwidth?

            org.onosproject.tetopology.management.api.node.
                TunnelTerminationPoint ttpTe = new
                org.onosproject.tetopology.management.api.node.
                DefaultTunnelTerminationPoint(ByteUtils.bytesToLong(ttpYang.tunnelTpId()),
                                              switchingLayer,
                                              encodingLayer,
                                              flags,
                                              interLayerLockList,
                                              localLinkConnectivityList,
                                              availAdaptBandwidth);

            ttpsMap.put(ByteUtils.bytesToLong(ttpYang.tunnelTpId()), ttpTe);
        }

        return ttpsMap;
    }

    private static TeNode yang2TeSubsystemNodeAugment(Te yangNodeAugTe,
                                                      Network yangNetwork,
                                                      Node yangNode,
                                                      Map<KeyId, TerminationPoint> yangTps) {


        NodeId yangNodeId = yangNode.nodeId();
        List<SupportingNode> yangSupportNodes = yangNode.supportingNode();

        NetworkId yangNetworkId = yangNetwork.networkId();

        long teNodeId = Ip4Address.valueOf(yangNodeAugTe.teNodeId().dottedQuad().string()).toInt();

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
        if (yangNetwork.yangAugmentedInfo(AugmentedNdNetwork.class) != null) {
            AugmentedNdNetwork augmentLink =
                    (AugmentedNdNetwork) yangNetwork.yangAugmentedInfo(AugmentedNdNetwork.class);
            for (Link link : augmentLink.link()) {
                if (link.source().sourceNode().equals(yangNodeAugTe.teNodeId())) {
                    teLinkIds.add(Long.valueOf(link.linkId().uri().string()));
                }
            }
        }
        // ********************************************** to find teTpIds
        if (yangNode.yangAugmentedInfoMap() != null
                && !yangNode.yangAugmentedInfoMap().isEmpty()) {

            AugmentedNdNode yangTpNodeAugment = (AugmentedNdNode) yangNode
                    .yangAugmentedInfo(AugmentedNdNode.class);

            if (yangTpNodeAugment.terminationPoint() != null) {
                for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                        .rev20151208.ietfnetworktopology.networks.network.node.augmentedndnode.TerminationPoint
                        yangTpnode : yangTpNodeAugment.terminationPoint()) {
                    teTpIds.add(Long.valueOf(yangTpnode.tpId().uri().string()));
                }
            }
        }
        // **********************************************

        Config ynodeAugCfg = yangNodeAugTe.config();

        if (ynodeAugCfg != null) {
            TeNodeAttributes teNodeAttr = ynodeAugCfg.teNodeAttributes();
            if (teNodeAttr != null) {

                if (teNodeAttr.underlayTopology() != null) {

                    underlayTopologyIdId = yang2TeSubsystemNodeUnderlayTopology(teNodeAttr
                            .underlayTopology());
                }
                BitSet flags = new BitSet();
                if (teNodeAttr.isAbstract()) {
                    flags.set(TeNode.BIT_ABSTRACT);
                }
                teData = new CommonNodeData(
                                            teNodeAttr.name().string(),
                                            EnumConverter.yang2TeSubsystemAdminStatus(teNodeAttr.adminStatus()),
                                            EnumConverter.yang2TeSubsystemOpStatus(yangNodeAugTe.state().operStatus()),
                                            flags);

                if (teNodeAttr.connectivityMatrix() != null) {
                    connMatrices = yang2TeSubsystemNodeConnectivityMatrix(yangNetworkId.uri().toString(),
                                                                    yangNodeId.uri().toString(),
                                                                    teNodeAttr.connectivityMatrix());
                }

            }
        }

        if (yangNodeAugTe.tunnelTerminationPoint() != null) {
            ttps = yang2TeSubsystemTtp(yangNodeAugTe.tunnelTerminationPoint());
        }

        TeNode teNode = new DefaultTeNode(teNodeId,
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
     * Converts a TE Topology node event from the data format used in
     * the core to its corresponding YANG Object (YO) format.
     *
     * @param eventType Node event type
     * @param nodeData  TE Topology node event data
     * @return YANG Object converted from nodeData
     */
    public static TeNodeEvent teNetworkNode2yangTeNodeEvent(TeTopologyEventTypeEnum eventType,
                                                            NetworkNode nodeData) {
        TeNodeEvent.TeNodeEventBuilder builder = new DefaultTeNodeEvent.TeNodeEventBuilder();

        TeTopologyEventType yangEventType = new TeTopologyEventType(eventType);
        builder.eventType(yangEventType);

        NodeId nodeId = NodeId.fromString(nodeData.nodeId().toString());
        builder.nodeRef(nodeId);

        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.
                rev20160708.ietftetopology.tenodeconfigattributesnotification.
                TeNodeAttributes teNodeAttributes = teNode2YangTeNodeAttributes(nodeData.teNode());
        builder.teNodeAttributes(teNodeAttributes);

        return builder.build();
    }

    private static org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.
            ietf.te.topology.rev20160708.ietftetopology.
            tenodeconfigattributesnotification.
            TeNodeAttributes teNode2YangTeNodeAttributes(TeNode teNode) {

        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.
                rev20160708.ietftetopology.tenodeconfigattributesnotification.
                TeNodeAttributes.TeNodeAttributesBuilder attrBuilder =
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.
                        rev20160708.ietftetopology.tenodeconfigattributesnotification.
                        DefaultTeNodeAttributes.builder();

        if (teNode.connectivityMatrices() != null) {

            org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology
            .tenodeconnectivitymatrixabs.DefaultConnectivityMatrix
            .ConnectivityMatrixBuilder connectivityMatrixConfigBuilder = org.onosproject.yang.gen.v1.urn.ietf.
            params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeconnectivitymatrixabs.
            DefaultConnectivityMatrix.builder();
            for (Map.Entry<Long, ConnectivityMatrix> teCmEntry :
                teNode.connectivityMatrices().entrySet()) {
                connectivityMatrixConfigBuilder = connectivityMatrixConfigBuilder
                        .id(teCmEntry.getKey())
                        .isAllowed(!teCmEntry.getValue().flags()
                                   .get(ConnectivityMatrix.BIT_DISALLOWED))
                        .from(new org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                              .ietftetopology.tenodeconnectivitymatrixabs.connectivitymatrix.DefaultFrom
                              .FromBuilder() // TODO: for now, assuming that there is
                                                            // only one 'from', and mergingList is empty
                              .tpRef(teCmEntry.getValue().from())
                                             .build())
                        .to(new org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
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
                rev20160708.ietftetopology.tenodeconfigattributesnotification.
                TeNodeAttributes teNodeAttributes = attrBuilder.build();

        return teNodeAttributes;
    }

    public static NetworkNodeKey yangNodeEvent2NetworkNodeKey(TeNodeEvent yangNodeEvent) {
        //TODO: implementation to be submitted as separate review

        NetworkId networkRef = (NetworkId) (yangNodeEvent.networkRef());
        NodeId nodeRef = (NodeId) (yangNodeEvent.nodeRef());
        KeyId networkId = KeyId.keyId(networkRef.uri().toString());
        KeyId nodeId = KeyId.keyId(nodeRef.uri().toString());

        NetworkNodeKey networkNodeKey = new NetworkNodeKey(networkId, nodeId);

        return networkNodeKey;
    }

    public static NetworkNode yangNodeEvent2NetworkNode(TeNodeEvent yangNodeEvent,
                                                        TeTopologyService teTopologyService) {
        //TODO: implementation to be submitted as separate review

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

        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology
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
                    ietf.te.topology.rev20160708.ietftetopology
                    .tenodeconnectivitymatrixabs.ConnectivityMatrix> yangConnMatrices = yangTeNodeAttrs
                    .connectivityMatrix();
            if (yangConnMatrices != null) {
                for (org.onosproject.yang.gen.v1.
                        urn.ietf.params.xml.ns.yang.
                        ietf.te.topology
                        .rev20160708.ietftetopology
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
                                                                         .rev20160708.ietftetopology
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
