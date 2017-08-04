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

import java.util.List;

import org.onosproject.bgpio.protocol.BgpVersion;
import org.onosproject.bgpio.types.BgpValueType;

/**
 * Abstraction of an BGP session info. Maintian session parameters obtained during session creation.
 */
public interface BgpSessionInfo {
    /**
     * Gets the bgp session type iBGP/eBGP.
     *
     * @return isiBGPSession, true if session is of type internal, otherwise false.
     */
    boolean isIbgpSession();

    /**
     * Gets the negotiated hold time for the session.
     *
     * @return negotiated hold time.
     */
    short negotiatedholdTime();

    /**
     * Gets the BGP ID of BGP peer.
     *
     * @return bgp ID.
     */
    BgpId remoteBgpId();

    /**
     * Gets the BGP version of peer.
     *
     * @return bgp version.
     */
    BgpVersion remoteBgpVersion();

    /**
     * Gets the BGP remote bgp AS number.
     *
     * @return remoteBgpASNum peer AS number.
     */
    long remoteBgpASNum();

    /**
     * Gets the BGP peer hold time.
     *
     * @return bgp hold time.
     */
    short remoteBgpHoldTime();

    /**
     * Gets the BGP version for this bgp peer.
     *
     * @return bgp identifier.
     */
    int remoteBgpIdentifier();

    /**
     * Gets the BGP capabilities for this BGP peer.
     *
     * @return BGP peer capabilities.
     */
    List<BgpValueType> remoteBgpCapability();
}
