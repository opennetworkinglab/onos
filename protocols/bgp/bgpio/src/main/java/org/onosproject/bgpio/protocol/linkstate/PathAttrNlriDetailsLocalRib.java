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
package org.onosproject.bgpio.protocol.linkstate;

import java.util.Objects;

import org.onlab.packet.IpAddress;
import com.google.common.base.MoreObjects;

/**
 * This Class stores path Attributes, protocol ID and Identifier of LinkState nlri.
 */
public class PathAttrNlriDetailsLocalRib {

    private IpAddress localRibIpAddress;
    private long localRibAsNum;
    private int localRibIdentifier;
    private boolean isLocalRibIbgpSession;
    private PathAttrNlriDetails localRibNlridetails;

    /**
     * Constructor to initialize parameter.
     *
     * @param localRibIpAddress peer ip address
     * @param localRibIdentifier peer identifier
     * @param localRibAsNum peer As number
     * @param isLocalRibIbgpSession flag to indicate is Ibgp session
     * @param localRibNlridetails Nlri details
     *
     */
    public PathAttrNlriDetailsLocalRib(IpAddress localRibIpAddress, int localRibIdentifier, long localRibAsNum,
                                       boolean isLocalRibIbgpSession, PathAttrNlriDetails localRibNlridetails) {
        this.localRibIpAddress = localRibIpAddress;
        this.localRibAsNum = localRibAsNum;
        this.localRibIdentifier = localRibIdentifier;
        this.isLocalRibIbgpSession = isLocalRibIbgpSession;
        this.localRibNlridetails = localRibNlridetails;
    }

    /**
     * Gets the Ipaddress updated in local rib.
     *
     *  @return localRibIpAddress ip address
     */
    public IpAddress localRibIpAddress() {
        return localRibIpAddress;
    }

    /**
     * Gets the autonomous system number updated in local rib.
     *
     *  @return localRibAsNum autonomous system number
     */
    public long localRibAsNum() {
        return localRibAsNum;
    }

    /**
     * Gets the indetifier updated in local rib.
     *
     *  @return localRibIdentifier identifier
     */
    public int localRibIdentifier() {
        return localRibIdentifier;
    }

    /**
     * Gets the bgp session type updated in local rib.
     *
     *  @return isLocalRibIbgpSession session type
     */
    public boolean isLocalRibIbgpSession() {
        return isLocalRibIbgpSession;
    }

    /**
     * Returns local RIB Nlri details.
     *
     * @return localRibNlridetails Nlri details in local rib
     */
    public PathAttrNlriDetails localRibNlridetails() {
        return this.localRibNlridetails;
    }

    @Override
    public int hashCode() {
        return Objects.hash(localRibIpAddress, localRibIdentifier, localRibAsNum, isLocalRibIbgpSession,
                            localRibNlridetails.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PathAttrNlriDetailsLocalRib) {
            PathAttrNlriDetailsLocalRib other = (PathAttrNlriDetailsLocalRib) obj;
            return Objects.equals(localRibIpAddress, other.localRibIpAddress)
                    && Objects.equals(localRibIdentifier, other.localRibIdentifier)
                    && Objects.equals(localRibAsNum, other.localRibAsNum)
                    && Objects.equals(isLocalRibIbgpSession, other.isLocalRibIbgpSession)
                    && Objects.equals(localRibNlridetails, other.localRibNlridetails);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("peerIdentifier", localRibIdentifier)
                .add("localRibpathAttributes", localRibNlridetails.pathAttributes()).toString();
    }
}
