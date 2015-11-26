/*
 * Copyright 2015 Open Networking Laboratory
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

import org.onosproject.bgpio.protocol.BgpLSNlri;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetails;
import org.onosproject.bgpio.types.RouteDistinguisher;

/**
 * Abstraction of BGP local RIB.
 */
public interface BgpLocalRib {

    /**
     * Add NLRI to local RIB.
     *
     * @param sessionInfo session info
     * @param nlri network layer reach info
     * @param details nlri details
     */
    void add(BgpSessionInfo sessionInfo, BgpLSNlri nlri, PathAttrNlriDetails details);

    /**
     * Removes NLRI identifier if it exists.
     *
     * @param nlri info
     */
    void delete(BgpLSNlri nlri);

    /**
     * Update NLRI identifier mapped with route distinguisher if it exists in tree otherwise add NLRI infomation mapped
     * to respective route distinguisher.
     *
     * @param sessionInfo BGP session info
     * @param nlri info
     * @param details has pathattribute, protocol id and identifier
     * @param routeDistinguisher unique for each VPN
     */
    void add(BgpSessionInfo sessionInfo, BgpLSNlri nlri, PathAttrNlriDetails details,
                    RouteDistinguisher routeDistinguisher);

    /**
     * Removes VPN NLRI identifier mapped to route distinguisher if it exists.
     *
     * @param nlri info
     * @param routeDistinguisher unique for each VPN
     */
    void delete(BgpLSNlri nlri, RouteDistinguisher routeDistinguisher);
}
