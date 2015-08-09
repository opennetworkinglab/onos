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

import java.util.Set;

import org.onlab.packet.IpAddress;

/**
 * Represents to provider facing side of a node.
 */
public interface OvsdbClientService {
    /**
     * Gets the node identifier.
     *
     * @return node identifier
     */
    OvsdbNodeId nodeId();

    /**
     * Creates the configuration for the tunnel.
     *
     * @param srcIp source IP address
     * @param dstIp destination IP address
     */
    void createTunnel(IpAddress srcIp, IpAddress dstIp);

    /**
     * Drops the configuration for the tunnel.
     *
     * @param srcIp source IP address
     * @param dstIp destination IP address
     */
    void dropTunnel(IpAddress srcIp, IpAddress dstIp);

    /**
     * Gets tunnels of the node.
     *
     * @return set of tunnels; empty if no tunnel is find
     */
    Set<OvsdbTunnel> getTunnels();

    /**
     * Creates a bridge.
     *
     * @param bridgeName bridge name
     */
    void createBridge(String bridgeName);

    /**
     * Drops a bridge.
     *
     * @param bridgeName bridge name
     */
    void dropBridge(String bridgeName);

    /**
     * Gets bridges of the node.
     *
     * @return set of bridges; empty if no bridge is find
     */
    Set<OvsdbBridge> getBridges();

    /**
     * Creates a port.
     *
     * @param bridgeName bridge name
     * @param portName port name
     */
    void createPort(String bridgeName, String portName);

    /**
     * Drops a port.
     *
     * @param bridgeName bridge name
     * @param portName port name
     */
    void dropPort(String bridgeName, String portName);

    /**
     * Gets ports of the bridge.
     *
     * @return set of ports; empty if no ports is find
     */
    Set<OvsdbPort> getPorts();

    /**
     * Checks if the node is still connected.
     *
     * @return true if the node is still connected
     */
    boolean isConnected();

}
