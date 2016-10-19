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

import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.teyang.api.OperationType;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.IetfNetwork.OnosYangOpType;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
        .ietfnetwork.DefaultNetworks;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
        .ietfnetwork.DefaultNetworksState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
        .ietfnetwork.NetworkId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
        .ietfnetwork.Networks;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
        .ietfnetwork.NetworksState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
        .ietfnetwork.networks.DefaultNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
        .ietfnetwork.networks.Network;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
        .ietfnetwork.networks.Network.NetworkBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
        .ietfnetwork.networks.network.DefaultNetworkTypes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
        .ietfnetwork.networks.network.DefaultSupportingNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.networks.network.DefaultSupportingNetwork.SupportingNetworkBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
        .ietfnetwork.networks.network.NetworkTypes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
        .ietfnetwork.networks.network.Node;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
        .ietfnetwork.networks.network.SupportingNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
        .ietfnetworktopology.networks.network.AugmentedNdNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
        .ietfnetworktopology.networks.network.DefaultAugmentedNdNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.ietfnetworktopology.networks.network.DefaultAugmentedNdNetwork.AugmentedNdNetworkBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
        .ietfnetworktopology.networks.network.augmentedndnetwork.Link;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
        .ietftetopology.networks.network.AugmentedNwNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
        .ietftetopology.networks.network.DefaultAugmentedNwNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
        .ietftetopology.networks.network.networktypes.AugmentedNwNetworkTypes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
        .ietftetopology.networks.network.networktypes.DefaultAugmentedNwNetworkTypes;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tetopologyaugment.DefaultTe;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tetopologyaugment.Te;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tetopologytype.DefaultTeTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.tetopologytype.TeTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705
        .ietftetypes.TeGlobalId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705
        .ietftetypes.TeTopologyId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;


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
     * @return Networks YANG object
     */
    public static Networks teSubsystem2YangNetworks(
            org.onosproject.tetopology.management.api.Networks teSubsystem,
            OperationType operation) {
        checkNotNull(teSubsystem, E_NULL_TE_NETWORKS);
        checkNotNull(teSubsystem.networks(), E_NULL_TE_NETWORK_LIST);
        Networks.NetworksBuilder builder =
                DefaultNetworks.builder()
                        .yangNetworksOpType(toNetworksOperationType(operation));
        List<Network> networks = Lists.newArrayList();
        for (org.onosproject.tetopology.management.api.Network teNetwork : teSubsystem.networks()) {
            networks.add(teSubsystem2YangNetwork(teNetwork, operation));
        }
        builder.network(networks);
        return builder.build();
    }

    /**
     * Network States object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsystem TE Topology subsystem networks object
     * @param operation operation type
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
     * @param yangNetworks Networks YANG object
     * @param yangNetworkStates NetworkStates YANG object
     * @return teSubsystem TE Topology subsystem networks object
     */
    public static org.onosproject.tetopology.management.api.Networks yang2TeSubsystemNetworks(
            Networks yangNetworks, NetworksState yangNetworkStates) {
        checkNotNull(yangNetworks, E_NULL_YANG_NETWORKS);
        checkNotNull(yangNetworks.network(), E_NULL_YANG_NETWORK_LIST);
        checkNotNull(yangNetworkStates, E_NULL_YANG_NETWORKSSTATE);

        org.onosproject.tetopology.management.api.DefaultNetworks defaultNetworks =
                new org.onosproject.tetopology.management.api.DefaultNetworks();
        List<org.onosproject.tetopology.management.api.Network> networks = Lists.newArrayList();
        for (Network network : yangNetworks.network()) {
            org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208
                    .ietfnetwork.networksstate.Network stateNetwork =
                            networkStateNetwork(network, yangNetworkStates);
            org.onosproject.tetopology.management.api.Network teNetwork;
            if (stateNetwork == null) {
                log.info("networkId {} can't be found in yangNetworkStates",
                          network.networkId());
                teNetwork = yang2TeSubsystemNetwork(network);
            } else {
                teNetwork = yang2TeSubsystemNetwork(network, stateNetwork);
            }
            networks.add(teNetwork);
        }

        defaultNetworks.setNetworks(networks);
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
                                               List<NetworkNode> teNodes) {
        List<Node> nodeList = Lists.newArrayList();
        // Add each node
        for (org.onosproject.tetopology.management.api.node.NetworkNode node : teNodes) {
            // Convert the te node to a YO.
            nodeList.add(NodeConverter.teSubsystem2YangNode(node));
        }
        return builder.node(nodeList);
    }

    private static NetworkBuilder te2YangLinks(NetworkBuilder builder,
                                               List<NetworkLink> teLinks) {
        List<Link> linkList = Lists.newArrayList();
        // Add each link
        for (org.onosproject.tetopology.management.api.link.NetworkLink link : teLinks) {
            // Convert the te link to a YO
            linkList.add(LinkConverter.teSubsystem2YangLink(link));
        }
        AugmentedNdNetworkBuilder ndAugment = DefaultAugmentedNdNetwork.builder();
        ndAugment.link(linkList);
        builder.addYangAugmentedInfo(ndAugment.build(), AugmentedNdNetwork.class);
        return builder;
    }

    private static NetworkBuilder te2YangNetworkType(NetworkBuilder builder,
                org.onosproject.tetopology.management.api.TeTopologyId teTopologyId) {
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
            org.onosproject.tetopology.management.api.TeTopologyId teTopologyId) {
        Te.TeBuilder teBuilder = DefaultTe.builder();
        teBuilder.clientId(new TeGlobalId(teTopologyId.clientId()));
        teBuilder.providerId(new TeGlobalId(teTopologyId.providerId()));
        if (teTopologyId.topologyId() !=  null) {
            teBuilder.teTopologyId(new TeTopologyId(teTopologyId.topologyId()));
        }

        AugmentedNwNetwork.AugmentedNwNetworkBuilder nwAugment = DefaultAugmentedNwNetwork
                .builder();
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
     * @return Network YANG object
     */
    public static Network teSubsystem2YangNetwork(
            org.onosproject.tetopology.management.api.Network teSubsystem,
            OperationType operation) {
        checkNotNull(teSubsystem, E_NULL_TE_NETWORK);
        checkNotNull(teSubsystem.networkId(), E_NULL_TE_NETWORKID);

        // Generate a network builder with the specific networkId.
        NetworkId networkId = NetworkId.fromString(teSubsystem.networkId().toString());
        NetworkBuilder builder = DefaultNetwork.builder()
                                               .yangNetworkOpType(
                                                       toNetworksOperationType(operation))
                                               .networkId(networkId);

        // Supporting networks
        if (teSubsystem.getSupportingNetworkIds() != null) {
            builder = te2YangSupportingNetwork(builder, teSubsystem.getSupportingNetworkIds());
        }

        // Nodes
        if (teSubsystem.getNodes() != null) {
            builder = te2YangNodes(builder, teSubsystem.getNodes());
        }

        // Network types
        builder = te2YangNetworkType(builder, teSubsystem.getTeTopologyId());

        // Add links - link is the augmentation
        if (teSubsystem.getLinks() != null) {
            builder = te2YangLinks(builder, teSubsystem.getLinks());
        }

        // TE Topology IDs
        if (teSubsystem.getTeTopologyId() != null) {
            builder = te2YangTopologyIds(builder, teSubsystem.getTeTopologyId());
        }

        return builder.build();
    }

    /**
     * Network State object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsystem TE Topology subsystem network object
     * @param operation operation type
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
     * @return TE Topology subsystem defaultNetwork object
     */
    private static org.onosproject.tetopology.management.api.DefaultNetwork yang2TeDefaultNetwork(
            Network yangNetwork) {
        checkNotNull(yangNetwork, E_NULL_YANG_NETWORK);
        checkNotNull(yangNetwork.networkId(), E_NULL_YANG_NETWORKID);
        String networkId = yangNetwork.networkId().uri().string();
        org.onosproject.tetopology.management.api.DefaultNetwork teNetwork =
                new org.onosproject.tetopology.management.api.DefaultNetwork(KeyId.keyId(networkId));

        // Supporting networks
        if (yangNetwork.supportingNetwork() != null) {
            List<KeyId> supportingNetworkIds = Lists.newArrayList();
            for (SupportingNetwork supportNw : yangNetwork.supportingNetwork()) {
                supportingNetworkIds.add(
                        KeyId.keyId(supportNw.networkRef().uri().string()));
            }
            teNetwork.setSupportingNetworkIds(supportingNetworkIds);
        }

        // Nodes
        if (yangNetwork.node() != null) {
            List<org.onosproject.tetopology.management.api.node.NetworkNode>
                teNodes = Lists.newArrayList();
            for (Node node : yangNetwork.node()) {
                // Convert the Yang Node to a TE node.
                teNodes.add(NodeConverter.yang2TeSubsystemNode(node, yangNetwork.networkId()));
            }
            teNetwork.setNodes(teNodes);
        }

        // Links
        if (yangNetwork.yangAugmentedInfo(AugmentedNdNetwork.class) != null) {
            AugmentedNdNetwork augmentLink =
                    (AugmentedNdNetwork) yangNetwork.yangAugmentedInfo(AugmentedNdNetwork.class);
            List<org.onosproject.tetopology.management.api.link.NetworkLink>
                teLinks = Lists.newArrayList();
            for (Link link : augmentLink.link()) {
                // Convert the Yang Link to a TE link.
                teLinks.add(LinkConverter.yang2TeSubsystemLink(link, yangNetwork.networkId()));
            }
            teNetwork.setLinks(teLinks);
        }

        // TE Topology Ids
        if (yangNetwork.yangAugmentedInfo(AugmentedNwNetwork.class) != null) {
            AugmentedNwNetwork augmentTeIds =
                    (AugmentedNwNetwork) yangNetwork.yangAugmentedInfo(AugmentedNwNetwork.class);
            org.onosproject.tetopology.management.api.TeTopologyId teTopologyId =
                    new org.onosproject.tetopology.management.api.TeTopologyId(
                            augmentTeIds.te().clientId().uint32(),
                            augmentTeIds.te().providerId().uint32(),
                            augmentTeIds.te().teTopologyId().string());

            teNetwork.setTeTopologyId(teTopologyId);
        }

        return teNetwork;
    }

    /**
     * Network object conversion from YANG to TE Topology subsystem.
     *
     * @param yangNetwork Network YANG object
     * @return network TE Topology subsystem networks object
     */
    public static org.onosproject.tetopology.management.api.Network yang2TeSubsystemNetwork(Network yangNetwork) {
       return yang2TeDefaultNetwork(yangNetwork);
    }

    /**
     * Network and State object conversion from YANG to TE Topology subsystem.
     *
     * @param yangNetwork Network YANG object
     * @param yangNetworkState NetworkState YANG object
     * @return teSubsystem TE Topology subsystem networks object
     */
    public static org.onosproject.tetopology.management.api.Network yang2TeSubsystemNetwork(Network yangNetwork,
            org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork
                    .networksstate.Network yangNetworkState) {
        org.onosproject.tetopology.management.api.DefaultNetwork teNetwork =
                yang2TeDefaultNetwork(yangNetwork);

        checkNotNull(yangNetworkState, E_NULL_YANG_NETWORKSSTATE_NETWORK);
        checkNotNull(yangNetworkState.networkRef(), E_NULL_YANG_NETWORKSSTATE_NETWORKREF);
        String networkref = yangNetworkState.networkRef().toString();
        checkState(teNetwork.networkId().toString().equals(networkref),
                   E_DIFF_YANG_NETWORKID);

        teNetwork.setServerProvided(yangNetworkState.serverProvided());

        return teNetwork;
    }
}


