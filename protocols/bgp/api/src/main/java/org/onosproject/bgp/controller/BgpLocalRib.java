/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.bgp.controller;

import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpLSNlri;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetails;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetailsLocalRib;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpPrefixLSIdentifier;
import org.onosproject.bgpio.types.RouteDistinguisher;

import java.util.Map;

/**
 * Abstraction of BGP local RIB.
 */
public interface BgpLocalRib {

    /**
     * Add NLRI to local RIB.
     *
     * @param sessionInfo session info
     * @param nlri network layer reach info
     * @param details path attributes and NLRI information
     * @throws BgpParseException while adding NLRI to local rib
     */
    void add(BgpSessionInfo sessionInfo, BgpLSNlri nlri, PathAttrNlriDetails details) throws BgpParseException;

    /**
     * Removes NLRI identifier if it exists.
     *
     * @param nlri info
     * @throws BgpParseException while deleting NLRI from local rib
     */
    void delete(BgpLSNlri nlri) throws BgpParseException;

    /**
     * Update NLRI identifier mapped with route distinguisher if it exists in tree otherwise add NLRI information mapped
     * to respective route distinguisher.
     *
     * @param sessionInfo BGP session info
     * @param nlri info
     * @param details has pathattribute, protocol id and identifier
     * @param routeDistinguisher unique for each VPN
     * @throws BgpParseException while adding NLRI updation
     */
    void add(BgpSessionInfo sessionInfo, BgpLSNlri nlri, PathAttrNlriDetails details,
                    RouteDistinguisher routeDistinguisher) throws BgpParseException;

    /**
     * Removes VPN NLRI identifier mapped to route distinguisher if it exists.
     *
     * @param nlri info
     * @param routeDistinguisher unique for each VPN
     * @throws BgpParseException while deleting NLRI from local rib
     */
    void delete(BgpLSNlri nlri, RouteDistinguisher routeDistinguisher) throws BgpParseException;

    /**
     * Returns node NLRI tree.
     *
     * @return node tree
     */
    Map<BgpNodeLSIdentifier, PathAttrNlriDetailsLocalRib> nodeTree();

    /**
     * Returns link NLRI tree.
     *
     * @return link tree
     */
    Map<BgpLinkLSIdentifier, PathAttrNlriDetailsLocalRib> linkTree();

    /**
     * Returns prefix NLRI tree.
     *
     * @return prefix tree
     */
    Map<BgpPrefixLSIdentifier, PathAttrNlriDetailsLocalRib> prefixTree();

    /**
     * Returns VPN node NLRI tree.
     *
     * @return vpn node NLRI tree
     */
    Map<RouteDistinguisher, Map<BgpNodeLSIdentifier, PathAttrNlriDetailsLocalRib>> vpnNodeTree();

    /**
     * Returns VPN link NLRI tree.
     *
     * @return vpn link NLRI Tree
     */
    Map<RouteDistinguisher, Map<BgpLinkLSIdentifier, PathAttrNlriDetailsLocalRib>> vpnLinkTree();

    /**
     * Returns VPN prefix NLRI tree.
     *
     * @return vpn prefix NLRI Tree
     */
    Map<RouteDistinguisher, Map<BgpPrefixLSIdentifier, PathAttrNlriDetailsLocalRib>> vpnPrefixTree();
}
