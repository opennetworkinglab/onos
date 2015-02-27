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
package org.onosproject.store.cluster.impl;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onosproject.cluster.ControllerNode;

/**
 * Node info read from configuration files during bootstrap.
 */
public final class NodeInfo {
    private final String id;
    private final String ip;
    private final int tcpPort;

    private NodeInfo(String id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.tcpPort = port;
    }

    /*
     * Needed for serialization.
     */
    private NodeInfo() {
        id = null;
        ip = null;
        tcpPort = 0;
    }

    /**
     * Creates a new instance.
     * @param id node id
     * @param ip node ip address
     * @param port tcp port
     * @return NodeInfo
     */
    public static NodeInfo from(String id, String ip, int port) {
        NodeInfo node = new NodeInfo(id, ip, port);
        return node;
    }

    /**
     * Returns the NodeInfo for a controller node.
     * @param node controller node
     * @return NodeInfo
     */
    public static NodeInfo of(ControllerNode node) {
        return NodeInfo.from(node.id().toString(), node.ip().toString(), node.tcpPort());
    }

    /**
     * Returns node id.
     * @return node id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns node ip.
     * @return node ip
     */
    public String getIp() {
        return ip;
    }

    /**
     * Returns node port.
     * @return port
     */
    public int getTcpPort() {
        return tcpPort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ip, tcpPort);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof NodeInfo) {
            NodeInfo that = (NodeInfo) o;
            return Objects.equals(this.id, that.id) &&
                    Objects.equals(this.ip, that.ip) &&
                    Objects.equals(this.tcpPort, that.tcpPort);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("ip", ip)
                .add("tcpPort", tcpPort).toString();
    }
}