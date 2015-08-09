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
package org.onosproject.ovsdb.controller;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onlab.packet.IpAddress;

/**
 * The class representing a OpenStack Compute or Network nodeId. This class is
 * immutable.
 */
public final class OvsdbNodeId {
    private static final String SCHEME = "ovsdb";
    private final String nodeId;
    private final String ipAddress;

    /**
     * Creates a new node identifier from a IpAddress ipAddress, a long port.
     *
     * @param ipAddress node IP address
     * @param port node port
     */
    public OvsdbNodeId(IpAddress ipAddress, long port) {
        checkNotNull(ipAddress, "ipAddress is not null");
        this.ipAddress = ipAddress.toString();
        this.nodeId = ipAddress + ":" + port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof OvsdbNodeId)) {
            return false;
        }

        OvsdbNodeId otherNodeId = (OvsdbNodeId) other;

        return Objects.equals(otherNodeId.nodeId, this.nodeId);
    }

    @Override
    public String toString() {
        return SCHEME + ":" + nodeId;
    }

    /**
     * Gets the value of the NodeId.
     *
     * @return the value of the NodeId.
     */
    public String nodeId() {
        return SCHEME + ":" + nodeId;
    }

    /**
     * Get the IP address of the node.
     *
     * @return the IP address of the node
     */
    public String getIpAddress() {
        return ipAddress;
    }
}
