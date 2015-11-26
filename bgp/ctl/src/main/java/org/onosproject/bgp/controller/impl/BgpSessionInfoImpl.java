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

package org.onosproject.bgp.controller.impl;

import org.onosproject.bgp.controller.BgpId;
import org.onosproject.bgp.controller.BgpSessionInfo;
import org.onosproject.bgpio.protocol.BgpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class maintains BGP peer session info.
 */
public class BgpSessionInfoImpl implements BgpSessionInfo {

    protected final Logger log = LoggerFactory.getLogger(BgpSessionInfoImpl.class);
    private BgpId remoteBgpId;
    private BgpVersion remoteBgpVersion;
    private long remoteBgpASNum;
    private short remoteBgpholdTime;
    private int remoteBgpIdentifier;
    private short negotiatedholdTime;
    private boolean isIbgpSession;

    /**
     * Initialize session info.
     *
     *@param remoteBgpId remote peer id
     *@param remoteBgpVersion remote peer version
     *@param remoteBgpASNum remote peer AS number
     *@param remoteBgpholdTime remote peer hold time
     *@param remoteBgpIdentifier remote peer identifier
     *@param negotiatedholdTime negotiated hold time
     *@param isIbgpSession session type ibgp/ebgp
     */
    public BgpSessionInfoImpl(BgpId remoteBgpId, BgpVersion remoteBgpVersion, long remoteBgpASNum,
                              short remoteBgpholdTime, int remoteBgpIdentifier, short negotiatedholdTime,
                              boolean isIbgpSession) {
        this.remoteBgpId = remoteBgpId;
        this.remoteBgpVersion = remoteBgpVersion;
        this.remoteBgpASNum = remoteBgpASNum;
        this.remoteBgpholdTime = remoteBgpholdTime;
        this.remoteBgpIdentifier = remoteBgpIdentifier;
        this.negotiatedholdTime = negotiatedholdTime;
        this.isIbgpSession = isIbgpSession;
    }

    @Override
    public boolean isIbgpSession() {
        return isIbgpSession;
    }

    @Override
    public short negotiatedholdTime() {
        return negotiatedholdTime;
    }

    @Override
    public BgpId remoteBgpId() {
        return remoteBgpId;
    }

    @Override
    public BgpVersion remoteBgpVersion() {
        return remoteBgpVersion;
    }

    @Override
    public long remoteBgpASNum() {
        return remoteBgpASNum;
    }

    @Override
    public short remoteBgpHoldTime() {
        return remoteBgpholdTime;
    }

    @Override
    public int remoteBgpIdentifier() {
        return remoteBgpIdentifier;
    }
}
