/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.onosproject.bgp.controller.impl;

import com.google.common.base.MoreObjects;

import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpId;
import org.onosproject.bgp.controller.BgpLinkListener;
import org.onosproject.bgp.controller.BgpLocalRib;
import org.onosproject.bgp.controller.BgpNodeListener;
import org.onosproject.bgp.controller.BgpSessionInfo;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpLSNlri;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpPrefixIPv4LSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpPrefixLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetails;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetailsLocalRib;
import org.onosproject.bgpio.types.RouteDistinguisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of local RIB.
 */
public class BgpLocalRibImpl implements BgpLocalRib {

    private static final Logger log = LoggerFactory.getLogger(BgpLocalRibImpl.class);
    private BgpController bgpController;

    private Map<BgpNodeLSIdentifier, PathAttrNlriDetailsLocalRib> nodeTree = new TreeMap<>();
    private Map<BgpLinkLSIdentifier, PathAttrNlriDetailsLocalRib> linkTree = new TreeMap<>();
    private Map<BgpPrefixLSIdentifier, PathAttrNlriDetailsLocalRib> prefixTree = new TreeMap<>();

    private Map<RouteDistinguisher, Map<BgpNodeLSIdentifier, PathAttrNlriDetailsLocalRib>> vpnNodeTree
                                                                                                   = new TreeMap<>();
    private Map<RouteDistinguisher, Map<BgpLinkLSIdentifier, PathAttrNlriDetailsLocalRib>> vpnLinkTree
                                                                                                   = new TreeMap<>();
    private Map<RouteDistinguisher, Map<BgpPrefixLSIdentifier, PathAttrNlriDetailsLocalRib>> vpnPrefixTree
                                                                                                   = new TreeMap<>();

    public BgpLocalRibImpl(BgpController bgpController) {
        this.bgpController = bgpController;
    }

    /**
     * Gets node NLRI tree.
     *
     * @return node tree
     */
    @Override
    public Map<BgpNodeLSIdentifier, PathAttrNlriDetailsLocalRib> nodeTree() {
        return nodeTree;
    }

    /**
     * Gets link NLRI tree.
     *
     * @return link tree
     */
    @Override
    public Map<BgpLinkLSIdentifier, PathAttrNlriDetailsLocalRib> linkTree() {
        return linkTree;
    }

    /**
     * Gets prefix NLRI tree.
     *
     * @return prefix tree
     */
    @Override
    public Map<BgpPrefixLSIdentifier, PathAttrNlriDetailsLocalRib> prefixTree() {
        return prefixTree;
    }

    /**
     * Gets VPN node NLRI tree.
     *
     * @return vpn node NLRI tree
     */
    @Override
    public Map<RouteDistinguisher, Map<BgpNodeLSIdentifier, PathAttrNlriDetailsLocalRib>> vpnNodeTree() {
        return vpnNodeTree;
    }

    /**
     * Gets VPN link NLRI tree.
     *
     * @return vpn link NLRI Tree
     */
    @Override
    public Map<RouteDistinguisher, Map<BgpLinkLSIdentifier, PathAttrNlriDetailsLocalRib>> vpnLinkTree() {
        return vpnLinkTree;
    }

    /**
     * Gets VPN prefix NLRI tree.
     *
     * @return vpn prefix NLRI Tree
     */
    @Override
    public Map<RouteDistinguisher, Map<BgpPrefixLSIdentifier, PathAttrNlriDetailsLocalRib>> vpnPrefixTree() {
        return vpnPrefixTree;
    }

    @Override
    public void add(BgpSessionInfo sessionInfo, BgpLSNlri nlri, PathAttrNlriDetails details) throws BgpParseException {
        int decisionResult;

        log.debug("Add to local RIB {}", details.toString());

        PathAttrNlriDetailsLocalRib detailsLocRib = new PathAttrNlriDetailsLocalRib(
                                                               sessionInfo.remoteBgpId().ipAddress(),
                                                               sessionInfo.remoteBgpIdentifier(),
                                                               sessionInfo.remoteBgpASNum(),
                                                               sessionInfo.isIbgpSession(), details);
        if (nlri instanceof BgpNodeLSNlriVer4) {
            BgpNodeLSIdentifier nodeLsIdentifier = ((BgpNodeLSNlriVer4) nlri).getLocalNodeDescriptors();
            if (nodeTree.containsKey(nodeLsIdentifier)) {
                BgpSelectionAlgo selectionAlgo = new BgpSelectionAlgo();
                // Compare local RIB entry with the current attribute
                decisionResult = selectionAlgo.compare(nodeTree.get(nodeLsIdentifier), detailsLocRib);
                if (decisionResult <= 0) {
                    for (BgpNodeListener l : bgpController.listener()) {
                        l.addNode((BgpNodeLSNlriVer4) nlri, details);
                    }
                    nodeTree.replace(nodeLsIdentifier, detailsLocRib);
                    log.debug("Local RIB update node: {}", detailsLocRib.toString());
                }
            } else {
                nodeTree.put(nodeLsIdentifier, detailsLocRib);
                for (BgpNodeListener l : bgpController.listener()) {
                    l.addNode((BgpNodeLSNlriVer4) nlri, details);
                }
                log.debug("Local RIB ad node: {}", detailsLocRib.toString());
            }
        } else if (nlri instanceof BgpLinkLsNlriVer4) {
            BgpLinkLSIdentifier linkLsIdentifier = ((BgpLinkLsNlriVer4) nlri).getLinkIdentifier();
            if (linkTree.containsKey(linkLsIdentifier)) {
                BgpSelectionAlgo selectionAlgo = new BgpSelectionAlgo();
                // Compare local RIB entry with the current attribute
                decisionResult = selectionAlgo.compare(linkTree.get(linkLsIdentifier), detailsLocRib);
                if (decisionResult <= 0) {
                    linkTree.replace(linkLsIdentifier, detailsLocRib);
                    for (BgpLinkListener l : bgpController.linkListener()) {
                        l.addLink((BgpLinkLsNlriVer4) nlri, details);
                    }
                    log.debug("Local RIB update link: {}", detailsLocRib.toString());
                }
            } else {
                linkTree.put(linkLsIdentifier, detailsLocRib);
                for (BgpLinkListener l : bgpController.linkListener()) {
                    l.addLink((BgpLinkLsNlriVer4) nlri, details);
                }
                log.debug("Local RIB add link: {}", detailsLocRib.toString());
            }
        } else if (nlri instanceof BgpPrefixIPv4LSNlriVer4) {
            BgpPrefixLSIdentifier prefixIdentifier = ((BgpPrefixIPv4LSNlriVer4) nlri).getPrefixIdentifier();
            if (prefixTree.containsKey(prefixIdentifier)) {
                BgpSelectionAlgo selectionAlgo = new BgpSelectionAlgo();
                // Compare local RIB entry with the current attribute
                decisionResult = selectionAlgo.compare(prefixTree.get(prefixIdentifier), detailsLocRib);
                if (decisionResult <= 0) {
                    prefixTree.replace(prefixIdentifier, detailsLocRib);
                    log.debug("Local RIB update prefix: {}", detailsLocRib.toString());
                }
            } else {
                prefixTree.put(prefixIdentifier, detailsLocRib);
                log.debug("Local RIB add prefix: {}", detailsLocRib.toString());
            }
        }
    }

    @Override
    public void delete(BgpLSNlri nlri) throws BgpParseException {
        log.debug("Delete from local RIB.");

        // Update local RIB
        decisionProcess(nlri);
    }

    /**
     * Update local RIB based on selection algorithm.
     *
     * @param nlri NLRI to update
     * @throws BgpParseException while updating to local RIB
     */
    public void decisionProcess(BgpLSNlri nlri) throws BgpParseException {
        checkNotNull(nlri);
        if (nlri instanceof BgpNodeLSNlriVer4) {
            selectionProcessNode(nlri, false);
        } else if (nlri instanceof BgpLinkLsNlriVer4) {
            selectionProcessLink(nlri, false);
        } else if (nlri instanceof BgpPrefixIPv4LSNlriVer4) {
            selectionProcessPrefix(nlri, false);
        }
    }

    /**
     * Update VPN local RIB .
     *
     * @param nlri NLRI to update
     * @param routeDistinguisher VPN id to update
     * @throws BgpParseException BGP parse exception
     */
    public void decisionProcess(BgpLSNlri nlri, RouteDistinguisher routeDistinguisher) throws BgpParseException {
        checkNotNull(nlri);
        if (nlri instanceof BgpNodeLSNlriVer4) {
            if (vpnNodeTree.containsKey(routeDistinguisher)) {
                selectionProcessNode(nlri, true);
                if (nodeTree.size() == 0) {
                    vpnNodeTree.remove(routeDistinguisher);
                }
            }
        } else if (nlri instanceof BgpLinkLsNlriVer4) {
            if (vpnLinkTree.containsKey(routeDistinguisher)) {
                selectionProcessLink(nlri, true);
                if (linkTree.size() == 0) {
                    vpnLinkTree.remove(routeDistinguisher);
                }
            }
        } else if (nlri instanceof BgpPrefixIPv4LSNlriVer4) {
            if (vpnPrefixTree.containsKey(routeDistinguisher)) {
                selectionProcessPrefix(nlri, true);
                if (prefixTree.size() == 0) {
                    vpnPrefixTree.remove(routeDistinguisher);
                }
            }
        }
    }

     /**
     * Selection process for local RIB node.
     *
     * @param nlri NLRI to update
     * @param isVpnRib true if VPN  local RIB, otherwise false
     * @throws BgpParseException throws BGP parse exception
     */
    public void selectionProcessNode(BgpLSNlri nlri, boolean isVpnRib) throws BgpParseException {
        BgpPeerImpl peer;
        BgpSessionInfo sessionInfo;
        int decisionResult;
        boolean containsKey;

        BgpNodeLSIdentifier nodeLsIdentifier = ((BgpNodeLSNlriVer4) nlri).getLocalNodeDescriptors();

        if (nodeTree.containsKey(nodeLsIdentifier)) {
            for (BgpNodeListener l : bgpController.listener()) {
                l.deleteNode((BgpNodeLSNlriVer4) nlri);
            }
            log.debug("Local RIB delete node: {}", nodeLsIdentifier.toString());
            nodeTree.remove(nodeLsIdentifier);
        }

        for (BgpId bgpId : bgpController.connectedPeers().keySet()) {
            peer = (BgpPeerImpl) (bgpController.getPeer(bgpId));

            if (nodeTree.containsKey(nodeLsIdentifier)) {
                containsKey = (!isVpnRib) ? (peer.adjacencyRib().nodeTree().containsKey(nodeLsIdentifier)) :
                                            (peer.vpnAdjacencyRib().nodeTree().containsKey(nodeLsIdentifier));

                if (!containsKey) {
                    continue;
                }
                sessionInfo = peer.sessionInfo();
                PathAttrNlriDetailsLocalRib detailsLocRib = new PathAttrNlriDetailsLocalRib(
                                                                sessionInfo.remoteBgpId().ipAddress(),
                                                                sessionInfo.remoteBgpIdentifier(),
                                                                sessionInfo.remoteBgpASNum(),
                                                                sessionInfo.isIbgpSession(),
                                                                (!isVpnRib) ?
                                                                (peer.adjacencyRib().nodeTree()
                                                                                    .get(nodeLsIdentifier)) :
                                                                (peer.vpnAdjacencyRib().nodeTree()
                                                                                        .get(nodeLsIdentifier)));
                BgpSelectionAlgo selectionAlgo = new BgpSelectionAlgo();
                decisionResult = selectionAlgo.compare(nodeTree.get(nodeLsIdentifier), detailsLocRib);
                if (decisionResult < 0) {
                    nodeTree.replace(nodeLsIdentifier, detailsLocRib);
                    log.debug("Local RIB node updated: {}", detailsLocRib.toString());
                }
            } else {
                if (!isVpnRib) {
                    if (peer.adjacencyRib().nodeTree().containsKey(nodeLsIdentifier)) {
                        add(peer.sessionInfo(), nlri, peer.adjacencyRib().nodeTree().get(nodeLsIdentifier));
                    }
                } else {
                    if (peer.vpnAdjacencyRib().nodeTree().containsKey(nodeLsIdentifier)) {
                        add(peer.sessionInfo(), nlri, peer.vpnAdjacencyRib().nodeTree().get(nodeLsIdentifier));
                    }
                }
            }
        }
    }

     /**
     * Selection process for local RIB link.
     *
     * @param nlri NLRI to update
     * @param isVpnRib true if VPN local RIB, otherwise false
     * @throws BgpParseException BGP parse exception
     */
    public void selectionProcessLink(BgpLSNlri nlri, boolean isVpnRib) throws BgpParseException {
        BgpPeerImpl peer;
        BgpSessionInfo sessionInfo;
        int decisionResult;
        boolean containsKey;

        BgpLinkLSIdentifier linkLsIdentifier = ((BgpLinkLsNlriVer4) nlri).getLinkIdentifier();

        if (linkTree.containsKey(linkLsIdentifier)) {
            log.debug("Local RIB remove link: {}", linkLsIdentifier.toString());
            for (BgpLinkListener l : bgpController.linkListener()) {
                l.deleteLink((BgpLinkLsNlriVer4) nlri);
            }
            linkTree.remove(linkLsIdentifier);
        }

        for (BgpId bgpId : bgpController.connectedPeers().keySet()) {
            peer = (BgpPeerImpl) (bgpController.getPeer(bgpId));

            if (linkTree.containsKey(linkLsIdentifier)) {

                containsKey = (!isVpnRib) ? (peer.adjacencyRib().linkTree().containsKey(linkLsIdentifier)) :
                                            (peer.vpnAdjacencyRib().linkTree().containsKey(linkLsIdentifier));

                if (!containsKey) {
                    continue;
                }

                sessionInfo = peer.sessionInfo();

                PathAttrNlriDetailsLocalRib detailsLocRib = new PathAttrNlriDetailsLocalRib(
                                                            sessionInfo.remoteBgpId().ipAddress(),
                                                            sessionInfo.remoteBgpIdentifier(),
                                                            sessionInfo.remoteBgpASNum(),
                                                            sessionInfo.isIbgpSession(),
                                                            ((!isVpnRib) ?
                                                            (peer.adjacencyRib().linkTree().get(linkLsIdentifier)) :
                                                            (peer.vpnAdjacencyRib().linkTree()
                                                                                   .get(linkLsIdentifier))));

                BgpSelectionAlgo selectionAlgo = new BgpSelectionAlgo();
                decisionResult = selectionAlgo.compare(linkTree.get(linkLsIdentifier), detailsLocRib);
                if (decisionResult < 0) {
                    linkTree.replace(linkLsIdentifier, detailsLocRib);
                    log.debug("Local RIB link updated: {}", detailsLocRib.toString());
                }
            } else {
                if (!isVpnRib) {
                    if (peer.adjacencyRib().linkTree().containsKey(linkLsIdentifier)) {
                        add(peer.sessionInfo(), nlri, peer.adjacencyRib().linkTree().get(linkLsIdentifier));
                    }
                } else {
                    if (peer.vpnAdjacencyRib().linkTree().containsKey(linkLsIdentifier)) {
                        add(peer.sessionInfo(), nlri, peer.vpnAdjacencyRib().linkTree().get(linkLsIdentifier));
                    }
                }
            }
        }
    }

     /**
     * Selection process for local RIB prefix.
     *
     * @param nlri NLRI to update
     * @param isVpnRib true if VPN local RIB, otherwise false
     * @throws BgpParseException BGP parse exception
     */
    public void selectionProcessPrefix(BgpLSNlri nlri, boolean isVpnRib) throws BgpParseException {
        BgpPeerImpl peer;
        BgpSessionInfo sessionInfo;
        int decisionResult;
        boolean containsKey;

        BgpPrefixLSIdentifier prefixIdentifier = ((BgpPrefixIPv4LSNlriVer4) nlri).getPrefixIdentifier();
        if (prefixTree.containsKey(prefixIdentifier)) {
            log.debug("Local RIB remove prefix: {}", prefixIdentifier.toString());
            prefixTree.remove(prefixIdentifier);
        }

        for (BgpId bgpId : bgpController.connectedPeers().keySet()) {
            peer = (BgpPeerImpl) (bgpController.getPeer(bgpId));

            if (prefixTree.containsKey(prefixIdentifier)) {

                containsKey = (!isVpnRib) ? (peer.adjacencyRib().prefixTree().containsKey(prefixIdentifier)) :
                                            (peer.vpnAdjacencyRib().prefixTree().containsKey(prefixIdentifier));
                if (!containsKey) {
                    continue;
                }
                sessionInfo = peer.sessionInfo();

                PathAttrNlriDetailsLocalRib detailsLocRib = new PathAttrNlriDetailsLocalRib(
                                                                sessionInfo.remoteBgpId().ipAddress(),
                                                                sessionInfo.remoteBgpIdentifier(),
                                                                sessionInfo.remoteBgpASNum(),
                                                                sessionInfo.isIbgpSession(),
                                                                ((!isVpnRib) ?
                                                                (peer.adjacencyRib().prefixTree()
                                                                                    .get(prefixIdentifier)) :
                                                                (peer.vpnAdjacencyRib().prefixTree()
                                                                                       .get(prefixIdentifier))));

                BgpSelectionAlgo selectionAlgo = new BgpSelectionAlgo();
                decisionResult = selectionAlgo.compare(prefixTree.get(prefixIdentifier), detailsLocRib);
                if (decisionResult < 0) {
                    prefixTree.replace(prefixIdentifier, detailsLocRib);
                    log.debug("Local RIB prefix updated: {}", detailsLocRib.toString());
                }
            } else {
                    if (!isVpnRib) {
                        if (peer.adjacencyRib().prefixTree().containsKey(prefixIdentifier)) {
                            add(peer.sessionInfo(), nlri, peer.adjacencyRib().prefixTree().get(prefixIdentifier));
                    } else {
                        if (peer.vpnAdjacencyRib().prefixTree().containsKey(prefixIdentifier)) {
                            add(peer.sessionInfo(), nlri, peer.vpnAdjacencyRib().prefixTree().get(prefixIdentifier));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void add(BgpSessionInfo sessionInfo, BgpLSNlri nlri, PathAttrNlriDetails details,
                    RouteDistinguisher routeDistinguisher) throws BgpParseException {
        add(sessionInfo, nlri, details);
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

    @Override
    public void delete(BgpLSNlri nlri, RouteDistinguisher routeDistinguisher) throws BgpParseException {
        // Update local RIB
        decisionProcess(nlri, routeDistinguisher);
    }

    /**
     * Update local RIB node based on avaliable peer adjacency RIB.
     *
     * @param o adjacency-in/VPN adjacency-in
     * @throws BgpParseException BGP parse exception
     */
    public void localRibUpdateNode(Object o) throws BgpParseException {

        if (o instanceof AdjRibIn) {
            AdjRibIn adjRib = (AdjRibIn) o;
            log.debug("Update local RIB node.");

            Set<BgpNodeLSIdentifier> nodeKeys = adjRib.nodeTree().keySet();
            for (BgpNodeLSIdentifier key : nodeKeys) {
                PathAttrNlriDetails pathAttrNlri = adjRib.nodeTree().get(key);

                BgpNodeLSNlriVer4 nodeNlri = new BgpNodeLSNlriVer4(pathAttrNlri.identifier(), pathAttrNlri
                                                                   .protocolID().getType(), key, false, null);
                decisionProcess(nodeNlri);
            }
        }

        if (o instanceof VpnAdjRibIn) {
            VpnAdjRibIn vpnAdjRib = (VpnAdjRibIn) o;
            log.debug("Update local RIB VPN node.");
            Set<RouteDistinguisher> nodeKeysVpn = vpnAdjRib.vpnNodeTree().keySet();
            Map<BgpNodeLSIdentifier, PathAttrNlriDetails> node;
            for (RouteDistinguisher keyVpnNode : nodeKeysVpn) {
                node = vpnAdjRib.vpnNodeTree().get(keyVpnNode);

                Set<BgpNodeLSIdentifier> vpnNodeKeys = node.keySet();
                for (BgpNodeLSIdentifier key : vpnNodeKeys) {
                    PathAttrNlriDetails pathAttrNlri = vpnAdjRib.nodeTree().get(key);
                    BgpNodeLSNlriVer4 nodeNlri = new BgpNodeLSNlriVer4(pathAttrNlri.identifier(),
                                                                      pathAttrNlri.protocolID().getType(),
                                                                      key, true, keyVpnNode);
                    decisionProcess(nodeNlri, keyVpnNode);
                }
            }
        }
    }

    /**
     * Update localRIB link based on avaliable peer adjacency RIB.
     *
     * @param o adjacency-in/VPN adjacency-in
     * @throws BgpParseException BGP parse exceptions
     */
    public void localRibUpdateLink(Object o) throws BgpParseException {

        if (o instanceof AdjRibIn) {
            AdjRibIn adjRib = (AdjRibIn) o;
            log.debug("Update local RIB link.");

            Set<BgpLinkLSIdentifier> linkKeys = adjRib.linkTree().keySet();
            for (BgpLinkLSIdentifier key : linkKeys) {
                PathAttrNlriDetails pathAttrNlri = adjRib.linkTree().get(key);
                BgpLinkLsNlriVer4 linkNlri = new BgpLinkLsNlriVer4(pathAttrNlri.protocolID().getType(),
                                                                   pathAttrNlri.identifier(), key, null, false);
                decisionProcess(linkNlri);
            }
        }

        if (o instanceof VpnAdjRibIn) {
            VpnAdjRibIn vpnAdjRib = (VpnAdjRibIn) o;
            log.debug("Update local RIB VPN link");

            Set<RouteDistinguisher> linkKeysVpn = vpnAdjRib.vpnLinkTree().keySet();
            Map<BgpLinkLSIdentifier, PathAttrNlriDetails> link;
            for (RouteDistinguisher keyVpnLink : linkKeysVpn) {
                link = vpnAdjRib.vpnLinkTree().get(keyVpnLink);

                Set<BgpLinkLSIdentifier> vpnLinkKeys = link.keySet();
                for (BgpLinkLSIdentifier key : vpnLinkKeys) {
                    PathAttrNlriDetails pathAttrNlri = vpnAdjRib.linkTree().get(key);
                    BgpLinkLsNlriVer4 linkNlri = new BgpLinkLsNlriVer4(pathAttrNlri.protocolID().getType(),
                                                                       pathAttrNlri.identifier(), key, keyVpnLink,
                                                                       true);
                    decisionProcess(linkNlri, keyVpnLink);
                }
            }
        }
    }

    /**
     * Update localRIB prefix based on avaliable peer adjacency RIB.
     *
     * @param o instance of adjacency-in/VPN adjacency-in
     * @throws BgpParseException BGP parse exception
     */
    public void localRibUpdatePrefix(Object o) throws BgpParseException {

        if (o instanceof AdjRibIn) {
            AdjRibIn adjRib = (AdjRibIn) o;
            log.debug("Update local RIB prefix.");

            Set<BgpPrefixLSIdentifier> prefixKeys = adjRib.prefixTree().keySet();
            for (BgpPrefixLSIdentifier key : prefixKeys) {
                PathAttrNlriDetails pathAttrNlri = adjRib.prefixTree().get(key);
                BgpPrefixIPv4LSNlriVer4 prefixNlri = new BgpPrefixIPv4LSNlriVer4(
                                                                             pathAttrNlri.identifier(),
                                                                             pathAttrNlri.protocolID().getType(),
                                                                             key, null, false);
                decisionProcess(prefixNlri);
            }
        }

        if (o instanceof VpnAdjRibIn) {
            VpnAdjRibIn vpnAdjRib = (VpnAdjRibIn) o;
            log.debug("Update local RIB VPN prefix.");

            Set<RouteDistinguisher> prefixKeysVpn = vpnAdjRib.vpnPrefixTree().keySet();
            Map<BgpPrefixLSIdentifier, PathAttrNlriDetails> prefix;
            for (RouteDistinguisher keyVpnPrefix : prefixKeysVpn) {
                prefix = vpnAdjRib.vpnPrefixTree().get(keyVpnPrefix);

                Set<BgpPrefixLSIdentifier> vpnPrefixKeys = prefix.keySet();
                for (BgpPrefixLSIdentifier key : vpnPrefixKeys) {
                    PathAttrNlriDetails pathAttrNlri = vpnAdjRib.prefixTree().get(key);
                    BgpPrefixIPv4LSNlriVer4 prefixNlri = new BgpPrefixIPv4LSNlriVer4(pathAttrNlri.identifier(),
                                                                                     pathAttrNlri.protocolID()
                                                                                             .getType(), key,
                                                                                     keyVpnPrefix, true);
                    decisionProcess(prefixNlri, keyVpnPrefix);
                }
            }
        }
    }

    /**
     * Update localRIB.
     *
     * @param adjRibIn adjacency RIB-in
     * @throws BgpParseException BGP parse exception
     */
    public void localRibUpdate(AdjRibIn adjRibIn) throws BgpParseException {
        log.debug("Update local RIB.");

        localRibUpdateNode(adjRibIn);
        localRibUpdateLink(adjRibIn);
        localRibUpdatePrefix(adjRibIn);
    }

    /**
     * Update localRIB.
     *
     * @param vpnAdjRibIn VPN adjacency RIB-in
     * @throws BgpParseException BGP parse exception
     */
    public void localRibUpdate(VpnAdjRibIn vpnAdjRibIn) throws BgpParseException {
        log.debug("Update VPN local RIB.");

        localRibUpdateNode(vpnAdjRibIn);
        localRibUpdateLink(vpnAdjRibIn);
        localRibUpdatePrefix(vpnAdjRibIn);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues().add("nodeTree", nodeTree)
                .add("linkTree", linkTree).add("prefixTree", prefixTree).add("vpnNodeTree", vpnNodeTree)
                .add("vpnLinkTree", vpnLinkTree).add("vpnPrefixTree", vpnPrefixTree).toString();
    }
}
