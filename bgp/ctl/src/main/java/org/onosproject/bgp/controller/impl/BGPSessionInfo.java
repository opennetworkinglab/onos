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

import org.onosproject.bgp.controller.BGPId;
import org.onosproject.bgpio.protocol.BGPVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class maintains BGP peer session info.
 */
public class BGPSessionInfo {

    protected final Logger log = LoggerFactory.getLogger(BGPSessionInfo.class);
    private BGPId remoteBgpId;
    private BGPVersion remoteBgpVersion;
    private short remoteBgpASNum;
    private short remoteBgpholdTime;
    private int remoteBgpIdentifier;
    private short negotiatedholdTime;

    /**
     * Gets the negotiated hold time for the session.
     *
     * @return negotiated hold time.
     */
    public short getNegotiatedholdTime() {
        return negotiatedholdTime;
    }

    /**
     * Sets the negotiated hold time for the session.
     *
     * @param negotiatedholdTime negotiated hold time.
     */
    public void setNegotiatedholdTime(short negotiatedholdTime) {
        this.negotiatedholdTime = negotiatedholdTime;
    }

    /**
     * Gets the BGP ID of BGP peer.
     *
     * @return bgp ID.
     */
    public BGPId getRemoteBgpId() {
        return remoteBgpId;
    }

    /**
     * Sets the BGP ID of bgp peer.
     *
     * @param bgpId BGP ID to set.
     */
    public void setRemoteBgpId(BGPId bgpId) {
        log.debug("Remote BGP ID {}", bgpId);
        this.remoteBgpId = bgpId;
    }

    /**
     * Gets the BGP version of peer.
     *
     * @return bgp version.
     */
    public BGPVersion getRemoteBgpVersion() {
        return remoteBgpVersion;
    }

    /**
     * Sets the BGP version for this bgp peer.
     *
     * @param bgpVersion bgp version to set.
     */
    public void setRemoteBgpVersion(BGPVersion bgpVersion) {
        log.debug("Remote BGP version {}", bgpVersion);
        this.remoteBgpVersion = bgpVersion;
    }

    /**
     * Gets the BGP remote bgp AS number.
     *
     * @return remoteBgpASNum peer AS number.
     */
    public short getRemoteBgpASNum() {
        return remoteBgpASNum;
    }

    /**
     * Sets the AS Number for this bgp peer.
     *
     * @param bgpASNum the autonomous system number value to set.
     */
    public void setRemoteBgpASNum(short bgpASNum) {
        log.debug("Remote BGP AS number {}", bgpASNum);
        this.remoteBgpASNum = bgpASNum;
    }

    /**
     * Gets the BGP peer hold time.
     *
     * @return bgp hold time.
     */
    public short getRemoteBgpHoldTime() {
        return remoteBgpholdTime;
    }

    /**
     * Sets the hold time for this bgp peer.
     *
     * @param holdTime the hold timer value to set.
     */
    public void setRemoteBgpHoldTime(short holdTime) {
        log.debug("Remote BGP HoldTime {}", holdTime);
        this.remoteBgpholdTime = holdTime;
    }

    /**
     * Gets the BGP version for this bgp peer.
     *
     * @return bgp identifier.
     */
    public int getRemoteBgpIdentifier() {
        return remoteBgpIdentifier;
    }

    /**
     * Sets the peer identifier value.
     *
     * @param bgpIdentifier the bgp peer identifier value.
     */
    public void setRemoteBgpIdentifier(int bgpIdentifier) {
        log.debug("Remote BGP Identifier {}", bgpIdentifier);
        this.remoteBgpIdentifier = bgpIdentifier;
    }
}
