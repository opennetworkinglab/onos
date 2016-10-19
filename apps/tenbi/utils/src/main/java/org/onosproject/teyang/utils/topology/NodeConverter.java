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
import org.onosproject.tetopology.management.api.node.InterfaceSwitchingCapability;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.tetopology.management.api.node.TeNetworkTopologyId;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TerminationCapability;
import org.onosproject.tetopology.management.api.node.TerminationPoint;
import org.onosproject.tetopology.management.api.node.TerminationPointKey;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev20130715.ietfinettypes.DomainName;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NetworkId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NodeId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
               .ietfnetwork.networks.network.DefaultNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
               .ietfnetwork.networks.network.Node;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
               .ietfnetwork.networks.network.node.DefaultSupportingNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
               .ietfnetwork.networks.network.node.SupportingNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.TpId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.node.AugmentedNdNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.node.DefaultAugmentedNdNode;
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
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeconnectivitymatrix.ConnectivityMatrix;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeconnectivitymatrix.DefaultConnectivityMatrix;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeconnectivitymatrix.connectivitymatrix.DefaultFrom;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeconnectivitymatrix.connectivitymatrix.DefaultTo;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeinfoattributes.DefaultUnderlayTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodeinfoattributes.UnderlayTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodetunnelterminationcapability.DefaultTerminationCapability;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tenodetunnelterminationcapability.DefaultTerminationCapability.TerminationCapabilityBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeNodeId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeTopologyId;

import com.google.common.collect.Lists;

/**
 * Node conversion functions.
 */
public final class NodeConverter {

    private static final String E_NULL_TE_SUBSYSTEM_TE_NODE = "TeSubsystem teNode object cannot be null";
    private static final String E_NULL_TE_SUBSYSTEM_TE_TUNNEL_TP = "TeSubsystem teTunnelTp object cannot be null";
    private static final String E_NULL_TE_SUBSYSTEM_NODE = "TeSubsystem ndoe object cannot be null";
    private static final String E_NULL_YANG_NODE = "Yang node object cannot be null";

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
        TeNodeAttributes.TeNodeAttributesBuilder teNodeAttributesConfigBuilder =
                DefaultTeNodeAttributes.builder()
                                       .isAbstract(teSubsystemTeNode.isAbstract());
        if (teSubsystemTeNode.adminStatus() != null) {
            teNodeAttributesConfigBuilder = teNodeAttributesConfigBuilder
                    .adminStatus(EnumConverter
                            .teSubsystem2YangAdminStatus(teSubsystemTeNode
                            .adminStatus()));
        }

        if (teSubsystemTeNode.name() != null) {
            teNodeAttributesConfigBuilder = teNodeAttributesConfigBuilder
                    .name(DomainName.fromString(teSubsystemTeNode.name()));
        }

        if (teSubsystemTeNode.underlayTopology() != null) {
            teNodeAttributesConfigBuilder = teNodeAttributesConfigBuilder
                    .underlayTopology(teNode2YangUnderlayConfig(teSubsystemTeNode.underlayTopology()));
        }

        if (teSubsystemTeNode.connectivityMatrices() != null) {
            ConnectivityMatrix.ConnectivityMatrixBuilder connectivityMatrixConfigBuilder =
                    DefaultConnectivityMatrix.builder();
            for (org.onosproject.tetopology.management.api.node.ConnectivityMatrix teCm : teSubsystemTeNode.
                    connectivityMatrices()) {
                connectivityMatrixConfigBuilder = connectivityMatrixConfigBuilder
                        .id(teCm.id())
                        .isAllowed(teCm.isAllowed())
                        .from(new DefaultFrom.FromBuilder()
                                .tpRef(TpId.fromString(teCm.from().tpId()
                                        .toString()))
                                .build())
                        .to(new DefaultTo.ToBuilder()
                                .tpRef(TpId.fromString(teCm.to().tpId()
                                        .toString()))
                                .build());
                teNodeAttributesConfigBuilder = teNodeAttributesConfigBuilder
                        .addToConnectivityMatrix(connectivityMatrixConfigBuilder
                                .build());
            }
        }


        Config.ConfigBuilder yangConfigBuilder = DefaultConfig.builder();
        yangConfigBuilder = yangConfigBuilder.teNodeAttributes(teNodeAttributesConfigBuilder.build());

        return yangConfigBuilder.build();
    }

    private static UnderlayTopology teNode2YangUnderlayConfig(TeNetworkTopologyId underlayTopology) {
        UnderlayTopology.UnderlayTopologyBuilder underlayConfigBuilder = DefaultUnderlayTopology
                .builder()
                .networkIdRef(NetworkId.fromString(underlayTopology.getNetworkId().toString()))
                .teTopologyIdRef(TeTopologyId
                        .fromString(underlayTopology
                                .getTopologyId().topologyId()));
        return underlayConfigBuilder.build();
    }

    /**
     * TE Node State object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsystemTeNode TE node object
     * @return TE Node State YANG object
     */
    private static State teNode2YangState(TeNode teSubsystemTeNode) {
        checkNotNull(teSubsystemTeNode, E_NULL_TE_SUBSYSTEM_TE_NODE);

        TeNodeAttributes.TeNodeAttributesBuilder teNodeAttributesStateBuilder = DefaultTeNodeAttributes
                .builder()
                .isAbstract(teSubsystemTeNode.isAbstract());

        if (teSubsystemTeNode.adminStatus() != null) {
            teNodeAttributesStateBuilder = teNodeAttributesStateBuilder
                    .adminStatus(EnumConverter
                            .teSubsystem2YangAdminStatus(teSubsystemTeNode
                            .adminStatus()));
        }

        if (teSubsystemTeNode.name() != null) {
            teNodeAttributesStateBuilder = teNodeAttributesStateBuilder
                    .name(DomainName.fromString(teSubsystemTeNode.name()));
        }

        if (teSubsystemTeNode.underlayTopology() != null) {
            UnderlayTopology.UnderlayTopologyBuilder underlayStateBuilder = DefaultUnderlayTopology
                    .builder();
            underlayStateBuilder = underlayStateBuilder
                    .networkIdRef(NetworkId.fromString(teSubsystemTeNode
                                                       .underlayTopology().getNetworkId().toString()))
                    .teTopologyIdRef(TeTopologyId.fromString(teSubsystemTeNode
                                                             .underlayTopology().getTopologyId().topologyId()));
            teNodeAttributesStateBuilder = teNodeAttributesStateBuilder
                    .underlayTopology(underlayStateBuilder.build());
        }

        if (teSubsystemTeNode.connectivityMatrices() != null) {
            ConnectivityMatrix.ConnectivityMatrixBuilder connectivityMatrixStateBuilder = DefaultConnectivityMatrix
                    .builder();
            for (org.onosproject.tetopology.management.api.node.ConnectivityMatrix teCm : teSubsystemTeNode
                    .connectivityMatrices()) {
                connectivityMatrixStateBuilder = connectivityMatrixStateBuilder
                        .id(teCm.id())
                        .isAllowed(teCm.isAllowed())
                        .from(new DefaultFrom.FromBuilder()
                                .tpRef(TpId.fromString(teCm.from().tpId()
                                        .toString()))
                                .build())
                        .to(new DefaultTo.ToBuilder()
                                .tpRef(TpId.fromString(teCm.to().tpId()
                                        .toString()))
                                .build());
                teNodeAttributesStateBuilder = teNodeAttributesStateBuilder
                        .addToConnectivityMatrix(connectivityMatrixStateBuilder
                                .build());
            }
        }

        State.StateBuilder yangStateBuilder = DefaultState.builder();
        yangStateBuilder = yangStateBuilder.teNodeAttributes(teNodeAttributesStateBuilder.build());

        if (teSubsystemTeNode.opStatus() != null) {
            yangStateBuilder = yangStateBuilder.operStatus(EnumConverter
                    .teSubsystem2YangOperStatus(teSubsystemTeNode
                            .opStatus()));
        }

        return yangStateBuilder.build();
    }

    /**
     * TE Node TunnelTerminationPoint object conversion from TE Topology subsystem to YANG.
     *
     * @param teTunnelTp TE TunnelTerminationPoint object
     * @return TunnelTerminationPoint YANG object
     */
    private static TunnelTerminationPoint teSubsystem2YangTtp(
                           org.onosproject.tetopology.management.api.node.TunnelTerminationPoint teTunnelTp) {
        checkNotNull(teTunnelTp, E_NULL_TE_SUBSYSTEM_TE_TUNNEL_TP);

        TunnelTerminationPoint.TunnelTerminationPointBuilder tunnelTpBuilder =
                DefaultTunnelTerminationPoint.builder().tunnelTpId(teTunnelTp.getTunnelTpId());

        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                .ietftetopology.tenodeaugment.te.tunnelterminationpoint.
                Config.ConfigBuilder ttpConfigBuilder =
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                .ietftetopology.tenodeaugment.te.tunnelterminationpoint.DefaultConfig.builder();
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                .ietftetopology.tenodeaugment.te.tunnelterminationpoint.
                State.StateBuilder ttpStateBuilder =
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
                .ietftetopology.tenodeaugment.te.tunnelterminationpoint.DefaultState.builder();

        if (Long.valueOf(teTunnelTp.getInterLayerLockId()) != null) {
            ttpConfigBuilder = ttpConfigBuilder.interLayerLockId(teTunnelTp.getInterLayerLockId());
            ttpStateBuilder  = ttpStateBuilder.interLayerLockId(teTunnelTp.getInterLayerLockId());
        }

        if (teTunnelTp.getSwitchingCapabilities() != null) {
            // FIXME: switchingCapabilities is a list in
            // teSubsystem, but is not a list in yang. how to handle
            // this?
            for (InterfaceSwitchingCapability iscTe : teTunnelTp.getSwitchingCapabilities()) {
                // ttpConfigBuilder =
                // ttpConfigBuilder.switchingCapability(switchingCapability)
                // ttpStateBuilder =
                // ttpStateBuilder.switchingCapability(switchingCapability)
            }
        }

        if (teTunnelTp.getTerminationCapabilities() != null) {
            for (TerminationCapability tcap : teTunnelTp.getTerminationCapabilities()) {
                TerminationCapabilityBuilder tcapBuilder = DefaultTerminationCapability
                        .builder();
                // FIXME: at this moment, tcap does not have any getter or setter.
                // add the following getLinkTpId possibly other attributes to Core data structure
                // tcapConfigBuilder =
                // tcapConfigBuilder.linkTp(TpId.fromString(tcap.getLinkTpId.toString()));
                ttpConfigBuilder = ttpConfigBuilder
                        .addToTerminationCapability(tcapBuilder.build());
                ttpStateBuilder = ttpStateBuilder
                        .addToTerminationCapability(tcapBuilder.build());
            }
        }

        tunnelTpBuilder = tunnelTpBuilder.config(ttpConfigBuilder.build())
                                         .state(ttpStateBuilder.build());

        return tunnelTpBuilder.build();
    }

    /**
     * Node object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsystem TE subsystem node object
     * @return YANG node object
     */
    public static Node teSubsystem2YangNode(org.onosproject.tetopology.management.api.node.NetworkNode teSubsystem) {
        checkNotNull(teSubsystem, E_NULL_TE_SUBSYSTEM_NODE);

        NodeId nodeId = NodeId.fromString(teSubsystem.nodeId().toString());
        Node.NodeBuilder builder = DefaultNode.builder().nodeId(nodeId);

        if (teSubsystem.getSupportingNodeIds() != null) {
            List<SupportingNode> sNodes = Lists.newArrayList();
            SupportingNode.SupportingNodeBuilder spNodeBuilder = DefaultSupportingNode
                    .builder();
            for (NetworkNodeKey nodeKey : teSubsystem.getSupportingNodeIds()) {
                sNodes.add(spNodeBuilder
                        .networkRef(NetworkId
                                .fromString(nodeKey.networkId().toString()))
                        .nodeRef(NodeId.fromString(nodeKey.nodeId().toString()))
                        .build());
            }
            builder = builder.supportingNode(sNodes);
        }

        if (teSubsystem.getTerminationPoints() != null) {
            AugmentedNdNode.AugmentedNdNodeBuilder tpAugmentBuilder = DefaultAugmentedNdNode
                    .builder();
            List<TerminationPoint> teSubsystemTeTp = teSubsystem
                    .getTerminationPoints();
            for (TerminationPoint teTp : teSubsystemTeTp) {
                tpAugmentBuilder.addToTerminationPoint(TerminationPointConverter
                        .teSubsystem2YangTerminationPoint(teTp));
            }
            builder.addYangAugmentedInfo(tpAugmentBuilder.build(),
                                         AugmentedNdNode.class);
        }

        if (teSubsystem.getTe() != null) {
            AugmentedNwNode.AugmentedNwNodeBuilder nodeAugmentBuilder = DefaultAugmentedNwNode
                    .builder();

            TeNode teSubsystemTeNode = teSubsystem.getTe();

            TeBuilder yangTeBuilder = DefaultTe.builder();

            if (teSubsystemTeNode.teNodeId() != null) {
                yangTeBuilder = yangTeBuilder.teNodeId(TeNodeId
                        .fromString(teSubsystemTeNode.teNodeId().toString()));
            }

            // Set configuration data
            // Set state data
            yangTeBuilder = yangTeBuilder.config(teNode2YangConfig(teSubsystemTeNode))
                                         .state(teNode2YangState(teSubsystemTeNode));

            if (teSubsystemTeNode.tunnelTerminationPoints() != null) {
                for (org.onosproject.tetopology.management.api.node.TunnelTerminationPoint
                        teTunnelTp : teSubsystemTeNode.tunnelTerminationPoints()) {
                    yangTeBuilder = yangTeBuilder.addToTunnelTerminationPoint(teSubsystem2YangTtp(teTunnelTp));
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
     * @param yangNode YANG node object
     * @param yangNetworkId YANG networkId object
     * @return TE subsystem node object
     */
    public static org.onosproject.tetopology.management.api.node.NetworkNode
                      yang2TeSubsystemNode(Node yangNode, NetworkId yangNetworkId) {
        checkNotNull(yangNode, E_NULL_YANG_NODE);

        org.onosproject.tetopology.management.api.node.DefaultNetworkNode node =
                new org.onosproject.tetopology.management.api.node.DefaultNetworkNode(
                       KeyId.keyId(yangNode.nodeId().uri().string()));

        if (yangNode.supportingNode() != null) {
            List<NetworkNodeKey> spNodes = Lists.newArrayList();
            for (SupportingNode yangSpNode : yangNode.supportingNode()) {
                NetworkNodeKey nodeKey = new NetworkNodeKey(KeyId.keyId(yangSpNode.nodeRef().uri().toString()),
                                                            KeyId.keyId(yangSpNode.networkRef().uri().toString()));
                spNodes.add(nodeKey);
            }
            node.setSupportingNodeIds(spNodes);
        }

        if (yangNode.yangAugmentedInfoMap() != null
                && !yangNode.yangAugmentedInfoMap().isEmpty()) {

            AugmentedNdNode yangTpNodeAugment = (AugmentedNdNode) yangNode
                    .yangAugmentedInfo(AugmentedNdNode.class);
            if (yang2TeSubsystemTpNodeAugment(yangTpNodeAugment) != null) {
                node.setTerminationPoints(yang2TeSubsystemTpNodeAugment(yangTpNodeAugment));
            }

            AugmentedNwNode yangNodeAugment = (AugmentedNwNode) yangNode
                    .yangAugmentedInfo(AugmentedNwNode.class);
            if (yangNodeAugment != null && yangNodeAugment.te() != null && yangNodeAugment.te().teNodeId() != null) {
                Te yangNodeAugTe = yangNodeAugment.te();
                TeNode teNode = yang2TeSubsystemNodeAugment(yangNodeAugTe, yangNetworkId, yangNode.nodeId());
                node.setTe(teNode);
            }
        }

        return node;
    }

    private static TeNode yang2TeSubsystemNodeConnectivityMatrix(TeNode teNode,
            String networkId, String nodeId, List<ConnectivityMatrix> yangMatrix) {
        List<org.onosproject.tetopology.management.api.node.ConnectivityMatrix> teCmList =
                Lists.newArrayList();
        for (ConnectivityMatrix cmYang : yangMatrix) {
            Long id = cmYang.id();
            TerminationPointKey from = new TerminationPointKey(
                                           KeyId.keyId(networkId),
                                           KeyId.keyId(nodeId),
                                           KeyId.keyId(cmYang.from().tpRef().toString()));
            TerminationPointKey to = new TerminationPointKey(
                                           KeyId.keyId(networkId),
                                           KeyId.keyId(nodeId),
                                           KeyId.keyId(cmYang.to().tpRef().toString()));
            boolean isAllowed = cmYang.isAllowed();
            org.onosproject.tetopology.management.api.node.ConnectivityMatrix cmTe =
                    new org.onosproject.tetopology.management.api.node.ConnectivityMatrix(
                            id, from, to, isAllowed);
            teCmList.add(cmTe);
        }
        teNode.setConnectivityMatrices(teCmList);
        return teNode;
    }

    private static TeNode yang2TeSubsystemNodeUnderlayTopology(TeNode teNode,
                                                               UnderlayTopology ut) {
        TeNetworkTopologyId underlayTopology =
                new TeNetworkTopologyId(KeyId.keyId(ut.networkIdRef().toString()),
                new org.onosproject.tetopology.management.api.TeTopologyId(
                            ((long) ut.providerIdRef()),
                            ((long) ut.clientIdRef()),
                            ut.teTopologyIdRef().toString()));
        teNode.setUnderlayTopology(underlayTopology);
        return teNode;
    }

    private static TeNode yang2TeSubsystemTtp(TeNode teNode,
                                              List<TunnelTerminationPoint> ttps) {
        List<org.onosproject.tetopology.management.api.node.TunnelTerminationPoint> ttpTeList =
                Lists.newArrayList();
        for (TunnelTerminationPoint ttpYang : ttps) {
            org.onosproject.tetopology.management.api.node.TunnelTerminationPoint ttpTe =
                    new org.onosproject.tetopology.management.api.node.TunnelTerminationPoint(
                            ttpYang.tunnelTpId());
            ttpTe.setInterLayerLockId(ttpYang.config().interLayerLockId());
            // FIXME: go through
            // ttpYang.config().switchingCapability() and populate
            // ttpTe.setSwitchingCapabilities(switchingCapabilities);
            // FIXME: go through
            // ttpYang.config().terminationCapability() and populate
            // ttpTe.setTerminationCapability(terminationCapability);
            ttpTeList.add(ttpTe);
        }
        teNode.setTunnelTerminationPoints(ttpTeList);

        return teNode;
    }

    private static TeNode yang2TeSubsystemNodeAugment(Te yangNodeAugTe,
            NetworkId yangNetworkId, NodeId yangNodeId) {
        TeNode teNode = new TeNode(yangNodeAugTe.teNodeId().toString());
        Config ynodeAugCfg = yangNodeAugTe.config();
        if (ynodeAugCfg != null) {
            TeNodeAttributes teNodeAttr = ynodeAugCfg.teNodeAttributes();
            if (teNodeAttr != null) {
                teNode.setAbstract(teNodeAttr.isAbstract());

                if (teNodeAttr.adminStatus() != null) {
                    teNode.setAdminStatus(EnumConverter.yang2TeSubsystemAdminStatus(
                                                            ynodeAugCfg.teNodeAttributes().adminStatus()));
                }

                if (yangNodeAugTe.state() != null &&
                        yangNodeAugTe.state().operStatus() != null) {
                    teNode.setOpStatus(EnumConverter.yang2TeSubsystemOpStatus(
                                                         yangNodeAugTe.state().operStatus()));
                }

                if (teNodeAttr.connectivityMatrix() != null) {
                    teNode = yang2TeSubsystemNodeConnectivityMatrix(teNode,
                                                                    yangNetworkId.uri().toString(),
                                                                    yangNodeId.uri().toString(),
                                                                    teNodeAttr.connectivityMatrix());
                }

                if (teNodeAttr.underlayTopology() != null) {
                    teNode = yang2TeSubsystemNodeUnderlayTopology(teNode,
                                                                  teNodeAttr.underlayTopology());
                }
            }
        }

        if (yangNodeAugTe.tunnelTerminationPoint() != null) {
            teNode = yang2TeSubsystemTtp(teNode, yangNodeAugTe.tunnelTerminationPoint());
        }
        return teNode;
    }

    private static List<TerminationPoint> yang2TeSubsystemTpNodeAugment(AugmentedNdNode yangTpNodeAugment) {
        if (yangTpNodeAugment.terminationPoint() != null) {
            List<TerminationPoint> teTpList = Lists.newArrayList();
            for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                    .rev20151208.ietfnetworktopology.networks.network.node.augmentedndnode.TerminationPoint
                    yangTpnode : yangTpNodeAugment.terminationPoint()) {
                teTpList.add(TerminationPointConverter.yang2teSubsystemTerminationPoint(yangTpnode));
            }
            return teTpList;
        }
        return null;
    }
}
