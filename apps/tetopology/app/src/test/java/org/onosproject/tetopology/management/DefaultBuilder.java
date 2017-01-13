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
package org.onosproject.tetopology.management;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.onlab.packet.Ip4Address;
import org.onosproject.net.DeviceId;
import org.onosproject.tetopology.management.api.DefaultNetwork;
import org.onosproject.tetopology.management.api.EncodingType;
import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.Network;
import org.onosproject.tetopology.management.api.SwitchingType;
import org.onosproject.tetopology.management.api.TeConstants;
import org.onosproject.tetopology.management.api.TeStatus;
import org.onosproject.tetopology.management.api.TeTopologyId;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.link.CommonLinkData;
import org.onosproject.tetopology.management.api.link.DefaultNetworkLink;
import org.onosproject.tetopology.management.api.link.DefaultTeLink;
import org.onosproject.tetopology.management.api.link.ExternalLink;
import org.onosproject.tetopology.management.api.link.LinkBandwidth;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.link.OduResource;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.link.TeLinkTpKey;
import org.onosproject.tetopology.management.api.link.TePathAttributes;
import org.onosproject.tetopology.management.api.link.UnderlayPath;
import org.onosproject.tetopology.management.api.node.CommonNodeData;
import org.onosproject.tetopology.management.api.node.ConnectivityMatrix;
import org.onosproject.tetopology.management.api.node.DefaultNetworkNode;
import org.onosproject.tetopology.management.api.node.DefaultTeNode;
import org.onosproject.tetopology.management.api.node.DefaultTerminationPoint;
import org.onosproject.tetopology.management.api.node.DefaultTunnelTerminationPoint;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.tetopology.management.api.node.NodeTpKey;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TerminationPoint;
import org.onosproject.tetopology.management.api.node.TunnelTerminationPoint;
import org.onosproject.tetopology.management.impl.TeMgrUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Builds a sample abstract TE Topology, which consists of one node(represents
 * an entire network), one inter-domain link and one TTP.
 */
public final class DefaultBuilder {
    private static final String NODEIP = "100.10.10.10";
    // Bandwidth in GigaBits/second
    private static final float[] ODU0BW = {1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f};
    private static final float[] ODU2BW = {10, 10, 10, 10, 10, 10, 10, 10};
    private static final float[] ODU3BW = {40, 40, 40, 40, 40, 40, 40, 40};
    private static final float[] ODU4BW = {100, 100, 100, 100, 100, 100, 100, 100};

    private static final String ODU2 = "ODU2";
    private static final String ODU3 = "ODU3";
    private static final String ODU4 = "ODU4";

    private static final long PROVIDER_ID = 0x100;
    private static final long CLIENT_ID = 0x0a0a0a0a;
    private static final long CLIENT_NIL = 0;
    private static final long ABSTRACT_TOPOLOGY_ID = 100;
    private static final long NATIVE_TOPOLOGY_ID = 1;
    private static final long NUM_TPS = 1;
    private static final long NUM_TTPS = 1;
    private static final boolean ABSTRACT = true;
    private static final boolean UNABSTRACT = false;
    private static final int FIRST_INDEX = 0;
    private static final long INTER_DOMAIN_LINK_PLUGID = 100;
    private static final long LINK_COST = 500;
    private static final long LINK_DELAY = 2000;
    private static final long LINK_SRLG = 150;
    private static final String DOMAIN_ID = "DomainX";

    private static NetworkNode networkNode;
    private static TeNode teNode;
    private static NetworkLink networkLink;
    private static TeLink teLink;
    private static Network network;
    private static TunnelTerminationPoint ttp;

    private static TeTopologyKey teTopologyKey = new TeTopologyKey(PROVIDER_ID,
                                                                   CLIENT_ID,
                                                                   ABSTRACT_TOPOLOGY_ID);

    // no instantiation
    private DefaultBuilder() {
    }

    private static TunnelTerminationPoint ttpBuilder(long ttpId) {
        return new DefaultTunnelTerminationPoint(ttpId, SwitchingType.OTN_TDM_CAPABLE,
                                                 EncodingType.LSP_ENCODING_ODUK,
                                                 new BitSet(TeConstants.FLAG_MAX_BITS),
                                                 null, null,
                                                 ODU2BW, null); //10G for ODU2
    }

    private static TerminationPoint tpBuilder(long teTpId) {
        return new DefaultTerminationPoint(KeyId.keyId(Long.toString(teTpId)), null, teTpId);
    }

    private static NetworkNode nodeBuilder(String nodeIp, long numTps, long numTtps,
                                          TeTopologyKey underlayTopologyId, TeNodeKey supportTeNodeId,
                                          TeNodeKey sourceTeNodeId, boolean isAbstract) {
        long teNodeId = Ip4Address.valueOf(nodeIp).toInt();
        BitSet flags = new BitSet(TeConstants.FLAG_MAX_BITS);

        if (isAbstract) {
            flags.set(TeNode.BIT_ABSTRACT);
        }
        CommonNodeData common = new CommonNodeData(nodeIp, TeStatus.UP, TeStatus.UP, flags);
        Map<Long, ConnectivityMatrix> connMatrices = null;
        List<Long> teTpIds = Lists.newArrayList();
        Map<KeyId, TerminationPoint> tps = Maps.newHashMap();
        for (long i = 0; i < numTps; i++) {
            teTpIds.add(i);
            tps.put(KeyId.keyId(Long.toString(i)), tpBuilder(i));
        }
        //TTP
        Map<Long, TunnelTerminationPoint> ttps = Maps.newHashMap();
        for (long i = 0; i < numTtps; i++) {
            ttps.put(i, ttpBuilder(i));
        }
        ttp = ttps.get(FIRST_INDEX);
        //TeNode
        teNode = new DefaultTeNode(teNodeId, underlayTopologyId,
                                   supportTeNodeId, sourceTeNodeId,
                                          common, connMatrices, teTpIds, ttps, teTpIds);
        List<NetworkNodeKey> supportingNodeIds = null;
        if (supportTeNodeId != null) {
            supportingNodeIds = Lists
                    .newArrayList(TeMgrUtil.networkNodeKey(supportTeNodeId));
        }

        return new DefaultNetworkNode(KeyId.keyId(nodeIp), supportingNodeIds, teNode, tps);
    }

    private static LinkBandwidth linkBwBuilder(String odu) {

        float[] maxBandwidth;  //Maximum bandwidth, Size is MAX_PRIORITY + 1
        float[] avaiBandwidth; //Unreserved bandwidth, Size is MAX_PRIORITY + 1
        float[] maxAvialLspBandwidth;  //Maximum available bandwidth for a LSP
        float[] minAvialLspBandwidth;  //Minimum available bandwidth for a LSP
        short odu0s;
        short odu1s;
        short odu2s;
        short odu2es = 0;
        short odu3s;
        short odu4s;
        short oduFlexes = 0;

        switch (odu) {
        case ODU3:
            maxBandwidth = ODU3BW;
            avaiBandwidth = ODU3BW;
            maxAvialLspBandwidth = ODU3BW;
            minAvialLspBandwidth = ODU0BW;
            odu0s = 32;
            odu1s = 16;
            odu2s = 4;
            odu3s = 1;
            odu4s = 0;
            break;
        case ODU4:
            maxBandwidth = ODU4BW;
            avaiBandwidth = ODU4BW;
            maxAvialLspBandwidth = ODU4BW;
            minAvialLspBandwidth = ODU0BW;
            odu0s = 80;
            odu1s = 40;
            odu2s = 10;
            odu3s = 2;
            odu4s = 1;
            break;
        default:
            maxBandwidth = ODU2BW;
            avaiBandwidth = ODU2BW;
            maxAvialLspBandwidth = ODU2BW;
            minAvialLspBandwidth = ODU0BW;
            odu0s = 8;
            odu1s = 4;
            odu2s = 1;
            odu3s = 0;
            odu4s = 0;
        }

        OduResource oduRrc = new OduResource(odu0s, odu1s, odu2s, odu2es, odu3s,
                                             odu4s, oduFlexes);
        return new LinkBandwidth(maxBandwidth, avaiBandwidth, maxAvialLspBandwidth,
                                 minAvialLspBandwidth, oduRrc);
    }

    private static NetworkLink linkBuilder(TeLinkTpKey teLinkKey, TeLinkTpKey peerTeLinkKey,
                                          TeTopologyKey underlayTopologyId, TeLinkTpGlobalKey supportTeLinkId,
                                          TeLinkTpGlobalKey sourceTeLinkId, boolean isAbstract, Long plugid,
                                          Long cost, Long delay, List<Long> srlgs, String odu) {
        //NetworkLink
        KeyId linkId = TeMgrUtil.toNetworkLinkId(teLinkKey);
        NodeTpKey source = TeMgrUtil.nodeTpKey(teLinkKey);
        NodeTpKey destination = null;
        if (peerTeLinkKey != null) {
            destination = TeMgrUtil.nodeTpKey(peerTeLinkKey);
        }
        List<NetworkLinkKey> supportingLinkIds = null;
        if (supportTeLinkId != null) {
            supportingLinkIds = Lists
                    .newArrayList(TeMgrUtil.networkLinkKey(supportTeLinkId));
        }
        BitSet flags = new BitSet(TeConstants.FLAG_MAX_BITS);
        if (isAbstract) {
            flags.set(TeLink.BIT_ABSTRACT);
        }
        ExternalLink externalLink = null;

        if (plugid != null) {
            // Inter-Domain Link
            flags.set(TeLink.BIT_ACCESS_INTERDOMAIN);
            externalLink = new ExternalLink(null, plugid);
        }
        UnderlayPath underlayPath = null;
        Long adminGroup = null;
        List<Long> interLayerLocks = null;
        teLink = new DefaultTeLink(teLinkKey, peerTeLinkKey, underlayTopologyId,
                                          supportTeLinkId, sourceTeLinkId,
                                          new CommonLinkData(TeStatus.UP, TeStatus.UP, flags,
                                                             SwitchingType.OTN_TDM_CAPABLE,
                                                             EncodingType.LSP_ENCODING_ODUK,
                                                             externalLink, underlayPath,
                                                             new TePathAttributes(cost, delay, srlgs),
                                                             adminGroup, interLayerLocks, linkBwBuilder(odu)));
        return new DefaultNetworkLink(linkId, source, destination, supportingLinkIds, teLink);
    }

    private static Network networkBuilder(TeTopologyId teTopologyId, KeyId supportingNetworkId,
                                         Map<KeyId, NetworkNode> nodes, Map<KeyId, NetworkLink> links,
                                         boolean serverProvided, DeviceId ownerId) {
        KeyId networkId = TeMgrUtil.toNetworkId(teTopologyId);
        List<KeyId> supportingNetworkIds = null;
        if (supportingNetworkId != null) {
            supportingNetworkIds = Lists.newArrayList(supportingNetworkId);
        }
        return new DefaultNetwork(networkId, supportingNetworkIds, nodes, links, teTopologyId,
                              serverProvided, ownerId);
    }

    /**
     * Returns the key for the sample TE Topology.
     *
     * @return value of TE Topology key
     */
    public static TeTopologyKey teTopologyKey() {
        return teTopologyKey;
    }

    /**
     * Returns the abstract TE Node in the sample TE Topology.
     *
     * @return value of TE node
     */
    public static TeNode teNode() {
        return teNode;
    }

    /**
     * Returns the TE link in the sample TE Topology.
     *
     * @return value of TE link
     */
    public static TeLink teLink() {
        return teLink;
    }

    /**
     * Builds a sample abstract TE Topology, which consists of one abstract node
     * representing an entire physical network, one inter-domain link and one
     * TTP.
     *
     * @return value of network with an abstract TE Topology
     */
    public static Network buildSampleAbstractNetwork() {
        TeTopologyKey underlayTopologyId = new TeTopologyKey(PROVIDER_ID,
                                                             CLIENT_NIL,
                                                             NATIVE_TOPOLOGY_ID);
        Map<KeyId, NetworkNode> nodes = Maps.newHashMap();
        networkNode = nodeBuilder(NODEIP, NUM_TPS, NUM_TTPS, underlayTopologyId,
                                  null, null, ABSTRACT);
        nodes.put(networkNode.nodeId(), networkNode);

        Map<KeyId, NetworkLink> links = Maps.newHashMap();
        TeLinkTpKey node1tp1 = new TeLinkTpKey(networkNode.teNode().teNodeId(),
                                               networkNode.teNode()
                                                       .teTerminationPointIds()
                                                       .get(FIRST_INDEX));
        networkLink = linkBuilder(node1tp1, null, null, null, null, UNABSTRACT,
                                  INTER_DOMAIN_LINK_PLUGID, LINK_COST,
                                  LINK_DELAY, Lists.newArrayList(LINK_SRLG),
                                  ODU4);
        links.put(networkLink.linkId(), networkLink);
        DeviceId ownerId = DeviceId.deviceId(DOMAIN_ID);
        TeTopologyId topologyId = new TeTopologyId(PROVIDER_ID, CLIENT_ID, Long
                .toString(ABSTRACT_TOPOLOGY_ID));
        network = networkBuilder(topologyId, null, nodes, links, false,
                                 ownerId);
        return network;
    }

}
