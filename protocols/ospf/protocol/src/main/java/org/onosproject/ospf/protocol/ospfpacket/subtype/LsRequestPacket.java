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
package org.onosproject.ospf.protocol.ospfpacket.subtype;

import com.google.common.base.MoreObjects;

/**
 * Representation of an LS Request packet and fields and access methods to access it.
 */
public class LsRequestPacket {

    private int lsType;
    private String linkStateId;
    private String ownRouterId;

    /**
     * Gets the LSA type.
     *
     * @return LSA type
     */
    public int lsType() {
        return lsType;
    }

    /**
     * Sets the LSA type.
     *
     * @param lsType LSA type
     */
    public void setLsType(int lsType) {
        this.lsType = lsType;
    }

    /**
     * Gets the link state id.
     *
     * @return link state id
     */
    public String linkStateId() {
        return linkStateId;
    }

    /**
     * Sets link state id.
     *
     * @param linkStateId state id
     */
    public void setLinkStateId(String linkStateId) {
        this.linkStateId = linkStateId;
    }

    /**
     * Gets the router id.
     *
     * @return router id
     */
    public String ownRouterId() {
        return ownRouterId;
    }

    /**
     * Sets the router id.
     *
     * @param ownRouterId router id
     */
    public void setOwnRouterId(String ownRouterId) {
        this.ownRouterId = ownRouterId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("lsType", lsType)
                .add("linkStateId", linkStateId)
                .add("ownRouterId", ownRouterId)
                .toString();
    }
}