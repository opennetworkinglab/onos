/*
 * Copyright 2015 Open Networking Laboratory
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

import org.onosproject.bgpio.protocol.BGPLSNlri;
import org.onosproject.bgpio.protocol.linkstate.BGPLinkLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BGPNodeLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BGPNodeLSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BGPPrefixIPv4LSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BGPPrefixLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetails;
import org.onosproject.bgpio.types.RouteDistinguisher;

import com.google.common.base.MoreObjects;

/**
 * Implementation of Adj-RIB-In with VPN for each peer.
 */
public class VpnAdjRibIn {
    private Map<BGPNodeLSIdentifier, PathAttrNlriDetails> nodeTree = new TreeMap<>();
    private Map<BGPLinkLSIdentifier, PathAttrNlriDetails> linkTree = new TreeMap<>();
    private Map<BGPPrefixLSIdentifier, PathAttrNlriDetails> prefixTree = new TreeMap<>();

    private Map<RouteDistinguisher, Map<BGPNodeLSIdentifier, PathAttrNlriDetails>> vpnNodeTree
                                                                                                     = new TreeMap<>();
    private Map<RouteDistinguisher, Map<BGPLinkLSIdentifier, PathAttrNlriDetails>> vpnLinkTree
                                                                                                     = new TreeMap<>();
    private Map<RouteDistinguisher, Map<BGPPrefixLSIdentifier, PathAttrNlriDetails>> vpnPrefixTree
                                                                                                     = new TreeMap<>();
    /**
     * Returns the adjacency node.
     *
     * @return node adjacency RIB node
     */
    public Map<BGPNodeLSIdentifier, PathAttrNlriDetails> nodeTree() {
        return nodeTree;
    }

    /**
     * Returns the adjacency link.
     *
     * @return link adjacency RIB node
     */
    public Map<BGPLinkLSIdentifier, PathAttrNlriDetails> linkTree() {
        return linkTree;
    }

    /**
     * Returns the adjacency prefix.
     *
     * @return prefix adjacency RIB node
     */
    public Map<BGPPrefixLSIdentifier, PathAttrNlriDetails> prefixTree() {
        return prefixTree;
    }

    /**
     * Returns the adjacency vpnNode.
     *
     * @return vpnNode adjacency RIB node
     */
    public Map<RouteDistinguisher, Map<BGPNodeLSIdentifier, PathAttrNlriDetails>> vpnNodeTree() {
        return vpnNodeTree;
    }

    /**
     * Returns the adjacency vpnLink.
     *
     * @return vpnLink adjacency RIB node
     */
    public Map<RouteDistinguisher, Map<BGPLinkLSIdentifier, PathAttrNlriDetails>> vpnLinkTree() {
        return vpnLinkTree;
    }

    /**
     * Returns the adjacency vpnPrefix.
     *
     * @return vpnPrefix adjacency RIB node
     */
    public Map<RouteDistinguisher, Map<BGPPrefixLSIdentifier, PathAttrNlriDetails>> vpnPrefixTree() {
        return vpnPrefixTree;
    }

    /**
     * Update vpn nlri identifier into the tree if nlri identifier exists in tree otherwise add this to the tree.
     *
     * @param nlri NLRI info
     * @param details has pathattribute , protocolID and identifier
     */
    public void add(BGPLSNlri nlri, PathAttrNlriDetails details) {
        if (nlri instanceof BGPNodeLSNlriVer4) {
            BGPNodeLSIdentifier nodeLSIdentifier = ((BGPNodeLSNlriVer4) nlri).getLocalNodeDescriptors();
            if (nodeTree.containsKey(nodeLSIdentifier)) {
                nodeTree.replace(nodeLSIdentifier, details);
            } else {
                nodeTree.put(nodeLSIdentifier, details);
            }
        } else if (nlri instanceof BgpLinkLsNlriVer4) {
            BGPLinkLSIdentifier linkLSIdentifier = ((BgpLinkLsNlriVer4) nlri).getLinkIdentifier();
            if (linkTree.containsKey(linkLSIdentifier)) {
                linkTree.replace(linkLSIdentifier, details);
            } else {
                linkTree.put(linkLSIdentifier, details);
            }
        } else if (nlri instanceof BGPPrefixIPv4LSNlriVer4) {
            BGPPrefixLSIdentifier prefixIdentifier = ((BGPPrefixIPv4LSNlriVer4) nlri).getPrefixIdentifier();
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
    public void addVpn(BGPLSNlri nlri, PathAttrNlriDetails details, RouteDistinguisher routeDistinguisher) {
        add(nlri, details);
        if (nlri instanceof BGPNodeLSNlriVer4) {
            if (!vpnNodeTree.containsKey(routeDistinguisher)) {
                vpnNodeTree.put(routeDistinguisher, nodeTree);
            }
        } else if (nlri instanceof BgpLinkLsNlriVer4) {
            if (!vpnLinkTree.containsKey(routeDistinguisher)) {
                vpnLinkTree.put(routeDistinguisher, linkTree);
            }
        } else if (nlri instanceof BGPPrefixIPv4LSNlriVer4) {
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
    public void removeVpn(BGPLSNlri nlri, RouteDistinguisher routeDistinguisher) {
        if (nlri instanceof BGPNodeLSNlriVer4) {
            if (vpnNodeTree.containsKey(routeDistinguisher)) {
                BGPNodeLSIdentifier nodeLSIdentifier = ((BGPNodeLSNlriVer4) nlri).getLocalNodeDescriptors();
                if (nodeTree.containsKey(nodeLSIdentifier)) {
                    nodeTree.remove(nodeLSIdentifier);
                }
                if ((vpnNodeTree.get(routeDistinguisher)).isEmpty()) {
                    vpnNodeTree.remove(routeDistinguisher);
                }
            }
        } else if (nlri instanceof BgpLinkLsNlriVer4) {
            if (vpnLinkTree.containsKey(routeDistinguisher)) {
                BGPLinkLSIdentifier linkLSIdentifier = ((BgpLinkLsNlriVer4) nlri).getLinkIdentifier();
                if (linkTree.containsKey(linkLSIdentifier)) {
                    linkTree.remove(linkLSIdentifier);
                }
                if ((vpnLinkTree.get(routeDistinguisher)).isEmpty()) {
                    vpnLinkTree.remove(routeDistinguisher);
                }
            }
        } else if (nlri instanceof BGPPrefixIPv4LSNlriVer4) {
            if (vpnPrefixTree.containsKey(routeDistinguisher)) {
                BGPPrefixLSIdentifier prefixIdentifier = ((BGPPrefixIPv4LSNlriVer4) nlri).getPrefixIdentifier();
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