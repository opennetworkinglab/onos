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
import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Map;

import org.onosproject.net.DeviceId;
import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.TeTopologyEvent;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.TeTopologyService;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkEventSubject;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.NetworkNodeEventSubject;
import org.onosproject.teyang.api.OperationType;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.IetfNetwork.OnosYangOpType;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.DefaultNetworks;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork
        .DefaultNetworksState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NetworkId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.Networks;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NetworksState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.networks
        .DefaultNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.networks.Network;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.networks.Network
        .NetworkBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.networks.network
        .DefaultNetworkTypes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.networks.network
        .DefaultSupportingNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.networks.network
        .DefaultSupportingNetwork.SupportingNetworkBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.networks.network
        .NetworkTypes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.networks.network
        .Node;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.networks.network
        .SupportingNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.ietfnetworktopology
        .networks.network.AugmentedNdNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.ietfnetworktopology
        .networks.network.DefaultAugmentedNdNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.ietfnetworktopology
        .networks.network.DefaultAugmentedNdNetwork.AugmentedNdNetworkBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.ietfnetworktopology
        .networks.network.augmentedndnetwork.Link;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology
        .IetfTeTopologyEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.IetfTeTopologyEventSubject;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.TeLinkEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.TeNodeEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.networks
        .network.AugmentedNwNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.networks
        .network.DefaultAugmentedNwNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.networks
        .network.networktypes.AugmentedNwNetworkTypes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.networks
        .network.networktypes.DefaultAugmentedNwNetworkTypes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology
        .tetopologyaugment.DefaultTe;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology
        .tetopologyaugment.Te;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tetopologyaugment.te.Config;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.tetopologyaugment.te.DefaultConfig;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology
        .tetopologytype.DefaultTeTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology
        .tetopologytype.TeTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.Cost;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.Delay;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.NotOptimized;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeGlobalId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeOptimizationCriterion;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeTopologyId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.tetopologyeventtype.TeTopologyEventTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


/**
 * Networks, Networks State conversion functions.
 */
public final class NetworkConverter {
    private static final String
            E_NULL_TE_NETWORKS = "TeSubsystem networks cannot be null";
    private static final String
            E_NULL_TE_NETWORK_LIST = "TeSubsystem network list cannot be null";
    private static final String
            E_NULL_TE_NETWORK = "TeSubsystem network cannot be null";
    private static final String
            E_NULL_TE_NETWORKID = "TeSubsystem networkId cannot be null";
    private static final String
            E_NULL_YANG_NETWORKS = "YANG networks cannot be null";
    private static final String
            E_NULL_YANG_NETWORK_LIST = "YANG network list cannot be null";
    private static final String
            E_NULL_YANG_NETWORK = "YANG network cannot be null";
    private static final String
            E_NULL_YANG_NETWORKID = "YANG networkId cannot be null";
    private static final String
            E_NULL_YANG_NETWORKSSTATE = "YANG networksState cannot be null";
    private static final String
            E_DIFF_YANG_NETWORKID = "YANG networkId must be same in Network and NetworkState";
    private static final String
            E_NULL_YANG_NETWORKSSTATE_NETWORK = "YANG networksState network cannot be null";
    private static final String
            E_NULL_YANG_NETWORKSSTATE_NETWORKREF = "YANG networksState networkRef cannot be null";

    private static final Logger log = LoggerFactory.getLogger(NetworkConverter.class);

    // no instantiation
    private NetworkConverter() {
    }

    private static OnosYangOpType toNetworksOperationType(OperationType operation) {
        switch (operation) {
            case CREATE:
                return OnosYangOpType.CREATE;

            case DELETE:
                return OnosYangOpType.DELETE;

            case REMOVE:
                return OnosYangOpType.REMOVE;

            case MERGE:
                return OnosYangOpType.MERGE;

            case REPLACE:
                return OnosYangOpType.REPLACE;

            default:
                return OnosYangOpType.NONE;
        }
    }

    /**
     * Networks object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsystem TE Topology subsystem networks object
     * @param operation operation type
     * @param teTopologyService teTopology core service
     * @return Networks YANG object
     */
    public static Networks teSubsystem2YangNetworks(
            org.onosproject.tetopology.management.api.Networks teSubsystem,
                                                    OperationType operation,
                                                    TeTopologyService teTopologyService) {
        checkNotNull(teSubsystem, E_NULL_TE_NETWORKS);
        checkNotNull(teSubsystem.networks(), E_NULL_TE_NETWORK_LIST);
        Networks.NetworksBuilder builder =
                DefaultNetworks.builder()
                        .yangNetworksOpType(toNetworksOperationType(operation));
        List<Network> networks = Lists.newArrayList();
        for (org.onosproject.tetopology.management.api.Network teNetwork : teSubsystem.networks()) {
            networks.add(teSubsystem2YangNetwork(teNetwork, operation,
                                                 teTopologyService));
        }
        builder.network(networks);
        return builder.build();
    }

    /**
     * Network States object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsystem TE Topology subsystem networks object
     * @param operation   operation type
     * @return NetworkStates YANG object
     */
    public static NetworksState teSubsystem2YangNetworkStates(
            org.onosproject.tetopology.management.api.Networks teSubsystem,
            OperationType operation) {
        checkNotNull(teSubsystem, "teSubsystem object cannot be null");
        checkNotNull(teSubsystem.networks(), "TeSubsystem Networks object cannot be null");
        NetworksState.NetworksStateBuilder builder =
                DefaultNetworksState.builder()
                        .yangNetworksStateOpType(toNetworksOperationType(operation));
        List<org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
                .ietfnetwork.networksstate.Network> networks = Lists.newArrayList();
        for (org.onosproject.tetopology.management.api.Network teNetwork : teSubsystem.networks()) {
            networks.add(teSubsystem2YangNetworkState(teNetwork, operation));
        }
        builder.network(networks);
        return builder.build();
    }

    private static org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
            .ietfnetwork.networksstate.Network networkStateNetwork(Network network,
                                                                   NetworksState yangNetworkStates) {
        checkNotNull(network, "YANG Network object cannot be null");
        checkNotNull(yangNetworkStates, "YANG NetworksState object cannot be null");
        if (yangNetworkStates.network() == null) {
            return null;
        }

        for (org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
                .ietfnetwork.networksstate.Network stateNetwork : yangNetworkStates.network()) {
            if (stateNetwork.networkRef().equals(network.networkId())) {
                return stateNetwork;
            }
        }
        return null;
    }

    /**
     * Networks object conversion from YANG to TE Topology subsystem.
     *
     * @param yangNetworks      Networks YANG object
     * @param yangNetworkStates NetworkStates YANG object
     * @param deviceId the device Id
     * @return teSubsystem TE Topology subsystem networks object
     */
    public static org.onosproject.tetopology.management.api.Networks yang2TeSubsystemNetworks(
            Networks yangNetworks, NetworksState yangNetworkStates, DeviceId deviceId) {
        checkNotNull(yangNetworks, E_NULL_YANG_NETWORKS);
        checkNotNull(yangNetworks.network(), E_NULL_YANG_NETWORK_LIST);
        checkNotNull(yangNetworkStates, E_NULL_YANG_NETWORKSSTATE);

        List<org.onosproject.tetopology.management.api.Network> networks = Lists.newArrayList();
        for (Network network : yangNetworks.network()) {
            org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
                    .ietfnetwork.networksstate.Network stateNetwork =
                    networkStateNetwork(network, yangNetworkStates);
            org.onosproject.tetopology.management.api.Network teNetwork;
            if (stateNetwork == null) {
                log.info("networkId {} can't be found in yangNetworkStates",
                          network.networkId());
                teNetwork = yang2TeSubsystemNetwork(network, yangNetworks, deviceId);
            } else {
                teNetwork = yang2TeSubsystemNetwork(network, stateNetwork, yangNetworks, deviceId);
            }
            networks.add(teNetwork);
        }

        org.onosproject.tetopology.management.api.DefaultNetworks defaultNetworks =
                new org.onosproject.tetopology.management.api.DefaultNetworks(networks);
        return defaultNetworks;
    }

    private static NetworkBuilder te2YangSupportingNetwork(NetworkBuilder builder,
                                                           List<KeyId> teSpptNetworkIds) {
        List<SupportingNetwork> snws = Lists.newArrayList();
        SupportingNetworkBuilder spNetworkBuilder = DefaultSupportingNetwork.builder();
        for (KeyId teSpNwKey : teSpptNetworkIds) {
            snws.add(spNetworkBuilder
                             .networkRef(NetworkId.fromString(teSpNwKey.toString()))
                             .build());
        }
        return builder.supportingNetwork(snws);
    }

    private static NetworkBuilder te2YangNodes(NetworkBuilder builder,
                                               Map<KeyId, NetworkNode> teNodes,
                                               TeTopologyService teTopologyService,
                                               TeTopologyKey teTopologyKey) {
        List<Node> nodeList = Lists.newArrayList();

        for (org.onosproject.tetopology.management.api.node.NetworkNode node : teNodes.values()) {
            nodeList.add(NodeConverter.teSubsystem2YangNode(node,
                                                            teTopologyService,
                                                            teTopologyKey));
        }
        return builder.node(nodeList);
    }

    private static NetworkBuilder te2YangLinks(NetworkBuilder builder,
                                               Map<KeyId, NetworkLink> teLinks,
                                               TeTopologyService teTopologyService) {
        List<Link> linkList = Lists.newArrayList();

        for (org.onosproject.tetopology.management.api.link.NetworkLink link : teLinks.values()) {
            linkList.add(LinkConverter.teSubsystem2YangLink(link, teTopologyService));
        }
        AugmentedNdNetworkBuilder ndAugment = DefaultAugmentedNdNetwork.builder();
        ndAugment.link(linkList);
        builder.addYangAugmentedInfo(ndAugment.build(), AugmentedNdNetwork.class);
        return builder;
    }

    private static NetworkBuilder te2YangNetworkType(NetworkBuilder builder,
                                                     org.onosproject.tetopology.management.api.TeTopologyId
                                                             teTopologyId) {
        NetworkTypes.NetworkTypesBuilder nwType = DefaultNetworkTypes.builder();
        if (teTopologyId != null) {
            // Set "te-topology" network type.
            TeTopology.TeTopologyBuilder teTopology = DefaultTeTopology.builder();
            AugmentedNwNetworkTypes.AugmentedNwNetworkTypesBuilder teNwType =
                    DefaultAugmentedNwNetworkTypes.builder();
            teNwType.teTopology(teTopology.build());
            nwType.addYangAugmentedInfo(teNwType.build(), AugmentedNwNetworkTypes.class);
        }
        return builder.networkTypes(nwType.build());
    }

    private static NetworkBuilder te2YangTopologyIds(NetworkBuilder builder,
                                                     org.onosproject.tetopology.management.api
                                                             .TeTopologyId teTopologyId,
                                                     TeTopologyService teTopologyService,
                                                     KeyId networkKeyId) {

        //teBuilder. //OPTIMIZATIONCRITERION for Config/State
        Te.TeBuilder teBuilder = DefaultTe.builder();
        Config.ConfigBuilder configBuilder = DefaultConfig.builder();
        org.onosproject.tetopology.management.api.Network nt = teTopologyService.network(networkKeyId);
        TeTopologyKey teTopoKey = new TeTopologyKey(nt.teTopologyId().providerId(),
                                                    nt.teTopologyId().clientId(),
                                                    Long.valueOf(nt.teTopologyId().topologyId()));
        switch (teTopologyService.teTopology(teTopoKey).optimization()) {
        case LEAST_COST:
            configBuilder = configBuilder.optimizationCriterion(Cost.class);
            break;
        case SHORTEST_DELAY:
            configBuilder = configBuilder.optimizationCriterion(Delay.class);
            break;
        case NOT_OPTIMIZED:
            configBuilder = configBuilder.optimizationCriterion(NotOptimized.class);
            break;
        default:
            configBuilder = configBuilder.optimizationCriterion(TeOptimizationCriterion.class);
            break;
        }

        teBuilder = teBuilder.config(configBuilder.build());

        AugmentedNwNetwork.AugmentedNwNetworkBuilder nwAugment = DefaultAugmentedNwNetwork
                .builder();
        nwAugment.clientId(new TeGlobalId(teTopologyId.clientId()));
        nwAugment.providerId(new TeGlobalId(teTopologyId.providerId()));
        if (teTopologyId.topologyId() != null) {
            nwAugment.teTopologyId(new TeTopologyId(teTopologyId.topologyId()));
        }
        nwAugment.te(teBuilder.build());
        builder.addYangAugmentedInfo(nwAugment.build(),
                                     AugmentedNwNetwork.class);
        return builder;
    }

    /**
     * Network object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsystem TE Topology subsystem network object
     * @param operation operation type
     * @param teTopologyService teTopology core service
     * @return Network YANG object
     */
    public static Network teSubsystem2YangNetwork(
            org.onosproject.tetopology.management.api.Network teSubsystem,
                                                  OperationType operation,
                                                  TeTopologyService teTopologyService) {
        checkNotNull(teSubsystem, E_NULL_TE_NETWORK);
        checkNotNull(teSubsystem.networkId(), E_NULL_TE_NETWORKID);

        // Generate a network builder with the specific networkId.
        NetworkId networkId = NetworkId.fromString(teSubsystem.networkId().toString());
        NetworkBuilder builder = DefaultNetwork.builder()
                .yangNetworkOpType(
                        toNetworksOperationType(operation))
                .networkId(networkId);

        // Supporting networks
        if (teSubsystem.supportingNetworkIds() != null) {
            builder = te2YangSupportingNetwork(builder, teSubsystem.supportingNetworkIds());
        }

        // Nodes
        if (teSubsystem.nodes() != null) {
            org.onosproject.tetopology.management.api.Network nt = teTopologyService.network(teSubsystem.networkId());
            TeTopologyKey teTopoKey = new TeTopologyKey(nt.teTopologyId().providerId(),
                                                        nt.teTopologyId().clientId(),
                                                        Long.valueOf(nt.teTopologyId().topologyId()));
            builder = te2YangNodes(builder, teSubsystem.nodes(),
                                   teTopologyService,
                                   teTopoKey);
        }

        // Network types
        builder = te2YangNetworkType(builder, teSubsystem.teTopologyId());

        // Add links - link is the augmentation
        if (teSubsystem.links() != null) {
            builder = te2YangLinks(builder, teSubsystem.links(), teTopologyService);
        }

        // TE Topology IDs
        if (teSubsystem.teTopologyId() != null) {
            builder = te2YangTopologyIds(builder, teSubsystem.teTopologyId(),
                                         teTopologyService,
                                         teSubsystem.networkId());
        }

        return builder.build();
    }

    /**
     * Network State object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsystem TE Topology subsystem network object
     * @param operation   operation type
     * @return Network YANG object
     */
    public static org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network
            .rev20151208.ietfnetwork.networksstate.Network
    teSubsystem2YangNetworkState(
            org.onosproject.tetopology.management.api.Network teSubsystem,
            OperationType operation) {
        checkNotNull(teSubsystem, E_NULL_TE_NETWORK);
        checkNotNull(teSubsystem.networkId(), E_NULL_TE_NETWORKID);

        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
                .ietfnetwork.networksstate.Network.NetworkBuilder stateBuilder =
                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
                        .ietfnetwork.networksstate.DefaultNetwork.builder();

        if (teSubsystem.networkId() != null) {
            stateBuilder.networkRef(NetworkId.fromString(teSubsystem.networkId().toString()));
        }
        stateBuilder.serverProvided(teSubsystem.isServerProvided());

        // Operation type may be required.
        return stateBuilder.build();
    }


    /**
     * Network conversion from YANG to TE Topology subsystem.
     *
     * @param yangNetwork Network YANG object
     * @param yangNetworkState NetworkState YANG object
     * @param yangNetworks Networks YANG object
     * @return TE Topology subsystem defaultNetwork object
     */
    private static org.onosproject.tetopology.management.api.DefaultNetwork yang2TeDefaultNetwork(
                                                Network yangNetwork,
                                                org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.
                                                yang.ietf.network.rev20151208.ietfnetwork.networksstate.
                                                Network yangNetworkState,
                                                Networks yangNetworks, DeviceId deviceId) {
        checkNotNull(yangNetwork, E_NULL_YANG_NETWORK);
        checkNotNull(yangNetwork.networkId(), E_NULL_YANG_NETWORKID);
        String networkId = yangNetwork.networkId().uri().string();

        KeyId networkKeyId = KeyId.keyId(networkId);
        List<KeyId> supportingNetworkIds = null;
        Map<KeyId, NetworkNode> teNodes = null;
        Map<KeyId, NetworkLink> teLinks = null;
        org.onosproject.tetopology.management.api.TeTopologyId teTopologyId = null;
        boolean serverProvided = false;

        // Supporting networks
        if (yangNetwork.supportingNetwork() != null) {
            supportingNetworkIds = Lists.newArrayList();
            for (SupportingNetwork supportNw : yangNetwork.supportingNetwork()) {
                supportingNetworkIds.add(
                        KeyId.keyId(supportNw.networkRef().uri().string()));
            }
        }

        // Nodes
        if (yangNetwork.node() != null) {
            teNodes = Maps.newHashMap();
            for (Node node : yangNetwork.node()) {
                // Convert the Yang Node to a TE node.
                teNodes.put(KeyId.keyId(node.nodeId().uri().string()),
                            NodeConverter.yang2TeSubsystemNode(node, yangNetwork, yangNetworks));
            }
        }

        // Links
        if (yangNetwork.yangAugmentedInfo(AugmentedNdNetwork.class) != null) {
            AugmentedNdNetwork augmentLink =
                    (AugmentedNdNetwork) yangNetwork.yangAugmentedInfo(AugmentedNdNetwork.class);
            teLinks = Maps.newHashMap();
            for (Link link : augmentLink.link()) {
                // Convert the Yang Link to a TE link.
                teLinks.put(KeyId.keyId(link.linkId().uri().string()),
                            LinkConverter.yang2TeSubsystemLink(link, yangNetwork, yangNetworks));
            }
        }

        // TE Topology Ids
        if (yangNetwork.yangAugmentedInfo(AugmentedNwNetwork.class) != null) {
            AugmentedNwNetwork augmentTeIds =
                    (AugmentedNwNetwork) yangNetwork.yangAugmentedInfo(AugmentedNwNetwork.class);
            teTopologyId =
                    new org.onosproject.tetopology.management.api.TeTopologyId(
                            augmentTeIds.providerId().uint32(),
                            augmentTeIds.clientId().uint32(),
                            augmentTeIds.teTopologyId().string());
        }

        if (yangNetworkState != null) {
            serverProvided = yangNetworkState.serverProvided();
        }

        org.onosproject.tetopology.management.api.DefaultNetwork network =
                new org.onosproject.tetopology.management.api.DefaultNetwork(networkKeyId, supportingNetworkIds,
                                                                             teNodes, teLinks, teTopologyId,
                                                                             serverProvided, deviceId);
        return network;
    }

    /**
     * Network object conversion from YANG to TE Topology subsystem.
     *
     * @param yangNetwork Network YANG object
     * @param yangNetworks Networks YANG object
     * @param deviceId The identifier of RESTCONF server device
     * @return network TE Topology subsystem networks object
     */
    public static org.onosproject.tetopology.management.api.Network yang2TeSubsystemNetwork(Network yangNetwork,
                                                                                            Networks yangNetworks,
                                                                                            DeviceId deviceId) {
        return yang2TeDefaultNetwork(yangNetwork, null, yangNetworks, deviceId);
    }

    /**
     * Network and State object conversion from YANG to TE Topology subsystem.
     *
     * @param yangNetwork Network YANG object
     * @param yangNetworkState NetworkState YANG object
     * @param yangNetworks Networks YANG object
     * @param deviceId The identifier of RESTCONF server device
     * @return teSubsystem TE Topology subsystem networks object
     */
    public static org.onosproject.tetopology.management.api.Network yang2TeSubsystemNetwork(Network yangNetwork,
            org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork
                    .networksstate.Network yangNetworkState, Networks yangNetworks, DeviceId deviceId) {
        checkNotNull(yangNetworkState, E_NULL_YANG_NETWORKSSTATE_NETWORK);
        checkNotNull(yangNetworkState.networkRef(), E_NULL_YANG_NETWORKSSTATE_NETWORKREF);

        org.onosproject.tetopology.management.api.DefaultNetwork teNetwork =
                yang2TeDefaultNetwork(yangNetwork, yangNetworkState, yangNetworks, deviceId);

        String networkref = yangNetworkState.networkRef().toString();
        checkState(teNetwork.networkId().toString().equals(networkref),
                   E_DIFF_YANG_NETWORKID);

        return teNetwork;
    }

    /**
     * Converts a TE Topology event from the data format used in core to its
     * corresponding YANG Object (YO) format.
     *
     * @param event TE Topology event from the core
     * @param teTopologyService TE Topology Service object
     * @return YANG Object converted from event
     */
    public static IetfTeTopologyEvent teTopoEvent2YangIetfTeTopoEvent(TeTopologyEvent event,
                                                                      TeTopologyService teTopologyService) {
        IetfTeTopologyEvent yangEvent = null;
        IetfTeTopologyEventSubject eventSubject = new IetfTeTopologyEventSubject();

        IetfTeTopologyEvent.Type yangEventType = teTopoEventType2YangIetfTopoEventType(event.type());
        if (yangEventType == IetfTeTopologyEvent.Type.TE_LINK_EVENT) {
            NetworkLinkEventSubject eventData = (NetworkLinkEventSubject) event.subject();
            TeTopologyEventTypeEnum linkEventType = teTopoEventType2YangTeTopoEventType(event.type());
            TeLinkEvent yangLinkEvent = LinkConverter
                    .teNetworkLink2yangTeLinkEvent(linkEventType, eventData, teTopologyService);
            eventSubject.teLinkEvent(yangLinkEvent);
            yangEvent = new IetfTeTopologyEvent(IetfTeTopologyEvent.Type.TE_LINK_EVENT, eventSubject);
        } else if (yangEventType == IetfTeTopologyEvent.Type.TE_NODE_EVENT) {
            NetworkNodeEventSubject eventData = (NetworkNodeEventSubject) event.subject();
            TeTopologyEventTypeEnum nodeEventType = teTopoEventType2YangTeTopoEventType(event.type());
            TeNodeEvent yangNodeEvent = NodeConverter.teNetworkNode2yangTeNodeEvent(nodeEventType, eventData);
            eventSubject.teNodeEvent(yangNodeEvent);
            yangEvent = new IetfTeTopologyEvent(IetfTeTopologyEvent.Type.TE_NODE_EVENT, eventSubject);
        }

        return yangEvent;
    }

    private static IetfTeTopologyEvent.Type teTopoEventType2YangIetfTopoEventType(TeTopologyEvent.Type type) {
        IetfTeTopologyEvent.Type returnType = null;

        switch (type) {
            case LINK_ADDED:
            case LINK_REMOVED:
            case LINK_UPDATED:
                returnType = IetfTeTopologyEvent.Type.TE_LINK_EVENT;
                break;
            case NODE_ADDED:
            case NODE_REMOVED:
            case NODE_UPDATED:
                returnType = IetfTeTopologyEvent.Type.TE_NODE_EVENT;
                break;
            default:
                log.warn("teTopoEventType2YangIetfTopoEventType: unknown type: {}", type);
        }

        return returnType;
    }

    private static TeTopologyEventTypeEnum teTopoEventType2YangTeTopoEventType(TeTopologyEvent.Type type) {
        TeTopologyEventTypeEnum returnType = null;

        switch (type) {
            case LINK_ADDED:
            case NODE_ADDED:
                returnType = TeTopologyEventTypeEnum.ADD;
                break;
            case LINK_REMOVED:
            case NODE_REMOVED:
                returnType = TeTopologyEventTypeEnum.REMOVE;
                break;
            case LINK_UPDATED:
            case NODE_UPDATED:
                returnType = TeTopologyEventTypeEnum.UPDATE;
                break;
            default:
                log.warn("teTopoEventType2YangteTopoEventType: unsupported type: {}", type);
            break;
        }

        return returnType;
    }
}


