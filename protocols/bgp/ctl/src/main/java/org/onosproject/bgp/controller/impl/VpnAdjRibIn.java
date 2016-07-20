/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.bgp.controller.impl;

import java.util.Map;
import java.util.TreeMap;

import org.onosproject.bgpio.protocol.BgpLSNlri;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpPrefixIPv4LSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpPrefixLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetails;
import org.onosproject.bgpio.types.RouteDistinguisher;

import com.google.common.base.MoreObjects;

/**
 * Implementation of Adj-RIB-In with VPN for each peer.
 */
public class VpnAdjRibIn {
    private Map<BgpNodeLSIdentifier, PathAttrNlriDetails> nodeTree = new TreeMap<>();
    private Map<BgpLinkLSIdentifier, PathAttrNlriDetails> linkTree = new TreeMap<>();
    private Map<BgpPrefixLSIdentifier, PathAttrNlriDetails> prefixTree = new TreeMap<>();

    private Map<RouteDistinguisher, Map<BgpNodeLSIdentifier, PathAttrNlriDetails>> vpnNodeTree
                                                                                                     = new TreeMap<>();
    private Map<RouteDistinguisher, Map<BgpLinkLSIdentifier, PathAttrNlriDetails>> vpnLinkTree
                                                                                                     = new TreeMap<>();
    private Map<RouteDistinguisher, Map<BgpPrefixLSIdentifier, PathAttrNlriDetails>> vpnPrefixTree
                                                                                                     = new TreeMap<>();
    /**
     * Returns the adjacency node.
     *
     * @return node adjacency RIB node
     */
    public Map<BgpNodeLSIdentifier, PathAttrNlriDetails> nodeTree() {
        return nodeTree;
    }

    /**
     * Returns the adjacency link.
     *
     * @return link adjacency RIB node
     */
    public Map<BgpLinkLSIdentifier, PathAttrNlriDetails> linkTree() {
        return linkTree;
    }

    /**
     * Returns the adjacency prefix.
     *
     * @return prefix adjacency RIB node
     */
    public Map<BgpPrefixLSIdentifier, PathAttrNlriDetails> prefixTree() {
        return prefixTree;
    }

    /**
     * Returns the adjacency vpnNode.
     *
     * @return vpnNode adjacency RIB node
     */
    public Map<RouteDistinguisher, Map<BgpNodeLSIdentifier, PathAttrNlriDetails>> vpnNodeTree() {
        return vpnNodeTree;
    }

    /**
     * Returns the adjacency vpnLink.
     *
     * @return vpnLink adjacency RIB node
     */
    public Map<RouteDistinguisher, Map<BgpLinkLSIdentifier, PathAttrNlriDetails>> vpnLinkTree() {
        return vpnLinkTree;
    }

    /**
     * Returns the adjacency vpnPrefix.
     *
     * @return vpnPrefix adjacency RIB node
     */
    public Map<RouteDistinguisher, Map<BgpPrefixLSIdentifier, PathAttrNlriDetails>> vpnPrefixTree() {
        return vpnPrefixTree;
    }

    /**
     * Update vpn nlri identifier into the tree if nlri identifier exists in tree otherwise add this to the tree.
     *
     * @param nlri NLRI info
     * @param details has pathattribute , protocolID and identifier
     */
    public void add(BgpLSNlri nlri, PathAttrNlriDetails details) {
        if (nlri instanceof BgpNodeLSNlriVer4) {
            BgpNodeLSIdentifier nodeLSIdentifier = ((BgpNodeLSNlriVer4) nlri).getLocalNodeDescriptors();
            if (nodeTree.containsKey(nodeLSIdentifier)) {
                nodeTree.replace(nodeLSIdentifier, details);
            } else {
                nodeTree.put(nodeLSIdentifier, details);
            }
        } else if (nlri instanceof BgpLinkLsNlriVer4) {
            BgpLinkLSIdentifier linkLSIdentifier = ((BgpLinkLsNlriVer4) nlri).getLinkIdentifier();
            if (linkTree.containsKey(linkLSIdentifier)) {
                linkTree.replace(linkLSIdentifier, details);
            } else {
                linkTree.put(linkLSIdentifier, details);
            }
        } else if (nlri instanceof BgpPrefixIPv4LSNlriVer4) {
            BgpPrefixLSIdentifier prefixIdentifier = ((BgpPrefixIPv4LSNlriVer4) nlri).getPrefixIdentifier();
            if (prefixTree.containsKey(prefixIdentifier)) {
                prefixTree.replace(prefixIdentifier, details);
            } else {
                prefixTree.put(prefixIdentifier, details);
            }
        }
    }

    /**
     * Update nlri identifier mapped with route distinguisher if it exists in tree otherwise add nlri infomation mapped
     * to respective route distinguisher in tree.
     *
     * @param nlri NLRI info
     * @param details has pathattribute , protocolID and identifier
     * @param routeDistinguisher unique for for each vpn
     */
    public void addVpn(BgpLSNlri nlri, PathAttrNlriDetails details, RouteDistinguisher routeDistinguisher) {
        add(nlri, details);
        if (nlri instanceof BgpNodeLSNlriVer4) {
            if (!vpnNodeTree.containsKey(routeDistinguisher)) {
                vpnNodeTree.put(routeDistinguisher, nodeTree);
            }
        } else if (nlri instanceof BgpLinkLsNlriVer4) {
            if (!vpnLinkTree.containsKey(routeDistinguisher)) {
                vpnLinkTree.put(routeDistinguisher, linkTree);
            }
        } else if (nlri instanceof BgpPrefixIPv4LSNlriVer4) {
            if (!vpnPrefixTree.containsKey(routeDistinguisher)) {
                vpnPrefixTree.put(routeDistinguisher, prefixTree);
            }
        }
    }

    /**
     * Removes vpn nlri identifier mapped to route distinguisher if it exists in tree.
     *
     * @param nlri NLRI Info
     * @param routeDistinguisher unique for for each vpn
     */
    public void removeVpn(BgpLSNlri nlri, RouteDistinguisher routeDistinguisher) {
        if (nlri instanceof BgpNodeLSNlriVer4) {
            if (vpnNodeTree.containsKey(routeDistinguisher)) {
                BgpNodeLSIdentifier nodeLSIdentifier = ((BgpNodeLSNlriVer4) nlri).getLocalNodeDescriptors();
                if (nodeTree.containsKey(nodeLSIdentifier)) {
                    nodeTree.remove(nodeLSIdentifier);
                }
                if ((vpnNodeTree.get(routeDistinguisher)).isEmpty()) {
                    vpnNodeTree.remove(routeDistinguisher);
                }
            }
        } else if (nlri instanceof BgpLinkLsNlriVer4) {
            if (vpnLinkTree.containsKey(routeDistinguisher)) {
                BgpLinkLSIdentifier linkLSIdentifier = ((BgpLinkLsNlriVer4) nlri).getLinkIdentifier();
                if (linkTree.containsKey(linkLSIdentifier)) {
                    linkTree.remove(linkLSIdentifier);
                }
                if ((vpnLinkTree.get(routeDistinguisher)).isEmpty()) {
                    vpnLinkTree.remove(routeDistinguisher);
                }
            }
        } else if (nlri instanceof BgpPrefixIPv4LSNlriVer4) {
            if (vpnPrefixTree.containsKey(routeDistinguisher)) {
                BgpPrefixLSIdentifier prefixIdentifier = ((BgpPrefixIPv4LSNlriVer4) nlri).getPrefixIdentifier();
                if (prefixTree.containsKey(prefixIdentifier)) {
                    prefixTree.remove(prefixIdentifier);
                }
                if ((vpnPrefixTree.get(routeDistinguisher)).isEmpty()) {
                    vpnPrefixTree.remove(routeDistinguisher);
                }
            }
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues().add("nodeTree", nodeTree)
                .add("linkTree", linkTree)
                .add("prefixTree", prefixTree)
                .add("vpnNodeTree", vpnNodeTree)
                .add("vpnLinkTree", vpnLinkTree)
                .add("vpnPrefixTree", vpnPrefixTree)
                .toString();
    }
}