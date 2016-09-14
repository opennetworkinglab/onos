/*
 * Copyright 2016-present Open Networking Laboratory
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

import java.math.BigInteger;
import java.util.List;

import org.onosproject.tetopology.management.api.DefaultNetwork;
import org.onosproject.tetopology.management.api.DefaultNetworks;
import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.Network;
import org.onosproject.tetopology.management.api.Networks;
import org.onosproject.tetopology.management.api.TeTopologyId;
import org.onosproject.tetopology.management.api.link.DefaultNetworkLink;
import org.onosproject.tetopology.management.api.link.LinkProtectionType;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.TeLinkAccessType;
import org.onosproject.tetopology.management.api.node.ConnectivityMatrix;
import org.onosproject.tetopology.management.api.node.DefaultNetworkNode;
import org.onosproject.tetopology.management.api.node.DefaultTerminationPoint;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.TeNetworkTopologyId;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TeStatus;
import org.onosproject.tetopology.management.api.node.TerminationPoint;
import org.onosproject.tetopology.management.api.node.TerminationPointKey;

import com.google.common.collect.Lists;

/**
 * Builds a sample Topology, which consists of two Nodes, one link,
 * and each node has two termination points.
 */
public final class DefaultBuilder {

    private static final String HUAWEI_NETWORK_NEW = "HUAWEI_NETWORK_NEW";
    private static final String HUAWEI_ROADM_1 = "HUAWEI_ROADM_1";
    private static final String CLIENT1_NODE1 = "CLIENT1_NODE1";
    private static final String LINE1_NODE1 = "LINE1_NODE1";
    private static final String NODE1_IP = "10.11.12.33";
    private static final String HUAWEI_ROADM_2 = "HUAWEI_ROADM_2";
    private static final String CLIENT1_NODE2 = "CLIENT1_NODE2";
    private static final String LINE1_NODE2 = "LINE1_NODE2";
    private static final String NODE2_IP = "10.11.12.34";
    private static final String LINK1FORNETWORK1 = "LINK1FORNETWORK1";
    private static final String HUAWEI_TE_TOPOLOGY_NEW = "HUAWEI_TE_TOPOLOGY_NEW";

    // no instantiation
    private DefaultBuilder() {
    }

    /**
     * Returns a sample TeSubsystem Networks object.
     *
     * @return the Networks object
     */
    public static Networks sampleTeSubsystemNetworksBuilder() {
        DefaultNetworks defaultNetworks = new DefaultNetworks();
        List<Network> networks = Lists.newArrayList();
        networks.add(sampleTeSubsystemNetworkBuilder());
        defaultNetworks.setNetworks(networks);
        return defaultNetworks;
    }

    /**
     * Returns a sample TeSubsystem Network object.
     *
     * @return the Network object
     */
    public static Network sampleTeSubsystemNetworkBuilder() {
        DefaultNetwork huaweiNetworkNew = new DefaultNetwork(KeyId.keyId(HUAWEI_NETWORK_NEW));
        huaweiNetworkNew.setServerProvided(true);
        List<NetworkNode> nodes = Lists.newArrayList();


        DefaultNetworkNode node1 = new DefaultNetworkNode(KeyId.keyId(HUAWEI_ROADM_1));

        DefaultTerminationPoint tp11 = new DefaultTerminationPoint(KeyId.keyId(CLIENT1_NODE1));
        DefaultTerminationPoint tp12 = new DefaultTerminationPoint(KeyId.keyId(LINE1_NODE1));

        List<TerminationPoint> tps1 = Lists.newArrayList();
        tps1.add(tp11);
        tps1.add(tp12);
        node1.setTerminationPoints(tps1);

        TeNode teNode1 = new TeNode(NODE1_IP);
        teNode1.setAdminStatus(TeStatus.UP);
        teNode1.setAbstract(false);
        ConnectivityMatrix cMatrix1 =
                new ConnectivityMatrix(1,
                                       new TerminationPointKey(huaweiNetworkNew.networkId(),
                                                               node1.nodeId(), tp11.id()),
                                       new TerminationPointKey(huaweiNetworkNew.networkId(),
                                                               node1.nodeId(), tp12.id()),
                                       true);
        List<ConnectivityMatrix> connMatrices = Lists.newArrayList();
        connMatrices.add(cMatrix1);
        teNode1.setConnectivityMatrices(connMatrices);
        node1.setTe(teNode1);

        DefaultNetworkNode node2 = new DefaultNetworkNode(KeyId.keyId(HUAWEI_ROADM_2));

        DefaultTerminationPoint tp21 = new DefaultTerminationPoint(KeyId.keyId(CLIENT1_NODE2));
        DefaultTerminationPoint tp22 = new DefaultTerminationPoint(KeyId.keyId(LINE1_NODE2));

        List<TerminationPoint> tps2 = Lists.newArrayList();
        tps2.add(tp21);
        tps2.add(tp22);
        node2.setTerminationPoints(tps2);

        TeNode teNode2 = new TeNode(NODE2_IP);
        teNode1.setAdminStatus(TeStatus.UP);
        teNode1.setAbstract(false);
        ConnectivityMatrix cMatrix2 =
                new ConnectivityMatrix(1,
                                       new TerminationPointKey(huaweiNetworkNew.networkId(),
                                                               node2.nodeId(), tp21.id()),
                                       new TerminationPointKey(huaweiNetworkNew.networkId(),
                                                               node2.nodeId(), tp22.id()),
                                       true);
        List<ConnectivityMatrix> connMatrices2 = Lists.newArrayList();
        connMatrices.add(cMatrix2);
        teNode1.setConnectivityMatrices(connMatrices2);
        node2.setTe(teNode2);

        nodes.add(node1);
        nodes.add(node2);
        huaweiNetworkNew.setNodes(nodes);

        List<NetworkLink> links = Lists.newArrayList();

        DefaultNetworkLink link1 = new DefaultNetworkLink(KeyId.keyId(LINK1FORNETWORK1));
        link1.setSource(new TerminationPointKey(huaweiNetworkNew.networkId(),
                                                node1.nodeId(),
                                                tp11.id()));
        link1.setDestination(new TerminationPointKey(huaweiNetworkNew.networkId(),
                                                     node2.nodeId(),
                                                     tp21.id()));
        TeLink teLink1 = new TeLink(BigInteger.valueOf(1));
        teLink1.setIsAbstract(false);
        teLink1.setAdminStatus(TeStatus.UP);
        teLink1.setAccessType(TeLinkAccessType.POINT_TO_POINT);
        teLink1.setLinkProtectionType(LinkProtectionType.UNPROTECTED);
        List<Long> teSrlgs = Lists.newArrayList();
        teSrlgs.add(1000L);
        teSrlgs.add(2000L);
        teLink1.setTeSrlgs(teSrlgs);
        link1.setTe(teLink1);
        links.add(link1);
        huaweiNetworkNew.setLinks(links);

        TeNetworkTopologyId teNetwork =
                new TeNetworkTopologyId(new TeTopologyId(22222L, 44444L, HUAWEI_TE_TOPOLOGY_NEW));
        huaweiNetworkNew.setTeTopologyId(teNetwork.getTopologyId());

        return huaweiNetworkNew;
    }
}
