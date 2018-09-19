/*
 * Copyright 2014-present Open Networking Foundation
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import org.onlab.packet.IpAddress;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of a controller instance descriptor.
 */
public class DefaultControllerNode implements ControllerNode {

    public static final int DEFAULT_PORT = 9876;

    private final NodeId id;
    private final String host;
    private final int tcpPort;
    private transient volatile IpAddress ip;

    // For serialization
    private DefaultControllerNode() {
        this.id = null;
        this.host = null;
        this.tcpPort = 0;
    }

    /**
     * Creates a new instance with the specified id and IP address.
     *
     * @param id instance identifier
     * @param host instance hostname
     */
    public DefaultControllerNode(NodeId id, String host) {
        this(id, host, DEFAULT_PORT);
    }

    /**
     * Creates a new instance with the specified id and IP address and TCP port.
     *
     * @param id instance identifier
     * @param host instance host name
     * @param tcpPort TCP port
     */
    public DefaultControllerNode(NodeId id, String host, int tcpPort) {
        this.id = checkNotNull(id);
        this.host = host;
        this.tcpPort = tcpPort;
    }

    /**
     * Creates a new instance with the specified id and IP address.
     *
     * @param id instance identifier
     * @param ip instance IP address
     */
    public DefaultControllerNode(NodeId id, IpAddress ip) {
        this(id, ip != null ? ip.toString() : null, DEFAULT_PORT);
    }

    /**
     * Creates a new instance with the specified id and IP address.
     *
     * @param id instance identifier
     * @param ip instance IP address
     * @param tcpPort TCP port
     */
    public DefaultControllerNode(NodeId id, IpAddress ip, int tcpPort) {
        this(id, ip != null ? ip.toString() : null, tcpPort);
    }

    @Override
    public NodeId id() {
        return id;
    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public IpAddress ip(boolean resolve) {
        if (resolve) {
            ip = resolveIp();
            return ip;
        }

        if (ip == null) {
            synchronized (this) {
                if (ip == null) {
                    ip = resolveIp();
                }
            }
        }
        return ip;
    }

    private IpAddress resolveIp() {
        try {
            return IpAddress.valueOf(InetAddress.getByName(host));
        } catch (UnknownHostException e) {
            return null;
        }
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
        return toStringHelper(this)
            .add("id", id)
            .add("host", host)
            .add("tcpPort", tcpPort)
            .toString();
    }

}
