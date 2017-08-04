/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onlab.packet.IpAddress;
import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The class representing a nodeId of node which using ovsdb connection.
 * This class is immutable.
 */
public final class OvsdbNodeId extends Identifier<String> {
    private static final String SCHEME = "ovsdb";
    private final String ipAddress;

    /**
     * Creates a new node identifier from an IpAddress ipAddress, a long port.
     *
     * @param ipAddress node IP address
     * @param port node port
     */
    public OvsdbNodeId(IpAddress ipAddress, long port) {
        // TODO: port is currently not in use, need to remove it later
        super(checkNotNull(ipAddress, "ipAddress is not null").toString());
        this.ipAddress = ipAddress.toString();
    }

    @Override
    public String toString() {
        return SCHEME + ":" + identifier;
    }

    /**
     * Gets the value of the NodeId.
     *
     * @return the value of the NodeId.
     */
    public String nodeId() {
        return SCHEME + ":" + identifier;
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
