/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.cluster;

import org.onlab.packet.IpAddress;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of a controller instance descriptor.
 */
public class DefaultControllerNode implements ControllerNode {

    public static final int DEFAULT_PORT = 9876;

    private final NodeId id;
    private final IpAddress ip;
    private final int tcpPort;

    // For serialization
    private DefaultControllerNode() {
        this.id = null;
        this.ip = null;
        this.tcpPort = 0;
    }

    /**
     * Creates a new instance with the specified id and IP address.
     *
     * @param id instance identifier
     * @param ip instance IP address
     */
    public DefaultControllerNode(NodeId id, IpAddress ip) {
        this(id, ip, DEFAULT_PORT);
    }

    /**
     * Creates a new instance with the specified id and IP address and TCP port.
     *
     * @param id instance identifier
     * @param ip instance IP address
     * @param tcpPort TCP port
     */
    public DefaultControllerNode(NodeId id, IpAddress ip, int tcpPort) {
        this.id = checkNotNull(id);
        this.ip = ip;
        this.tcpPort = tcpPort;
    }

    @Override
    public NodeId id() {
        return id;
    }

    @Override
    public IpAddress ip() {
        return ip;
    }

    @Override
    public int tcpPort() {
        return tcpPort;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof DefaultControllerNode) {
            DefaultControllerNode that = (DefaultControllerNode) o;
            return Objects.equals(this.id, that.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id)
                .add("ip", ip).add("tcpPort", tcpPort).toString();
    }

}
