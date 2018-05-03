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
import org.onlab.packet.TpPort;

import java.util.List;
import java.util.function.Consumer;

/**
 * Abstraction of an ovsdb controller. Serves as an one stop shop for obtaining
 * OvsdbNode and (un)register listeners on ovsdb events and ovsdb node events.
 */
public interface OvsdbController {

    /**
     * Adds Node Event Listener.
     *
     * @param listener node listener
     */
    void addNodeListener(OvsdbNodeListener listener);

    /**
     * Removes Node Event Listener.
     *
     * @param listener node listener
     */
    void removeNodeListener(OvsdbNodeListener listener);

    /**
     * Adds ovsdb event listener.
     *
     * @param listener event listener
     */
    void addOvsdbEventListener(OvsdbEventListener listener);

    /**
     * Removes ovsdb event listener.
     *
     * @param listener event listener
     */
    void removeOvsdbEventListener(OvsdbEventListener listener);

    /**
     * Gets all the nodes information.
     *
     * @return the list of node id
     */
    List<OvsdbNodeId> getNodeIds();

    /**
     * Gets an ovsdb client by node identifier.
     *
     * @param nodeId node identifier
     * @return OvsdbClient ovsdb node information
     */
    OvsdbClientService getOvsdbClient(OvsdbNodeId nodeId);

    /**
     * Connect to the ovsdb server with given ip address and port number.
     *
     * @param ip ip address
     * @param port port number
     */
    void connect(IpAddress ip, TpPort port);

    /**
     * Connect to the ovsdb server with given ip address, port number, and connection failure handler.
     *
     * @param ip ip address
     * @param port port number
     * @param failhandler connection failure handler
     */
    void connect(IpAddress ip, TpPort port, Consumer<Exception> failhandler);

    /**
     * Configure the OVSDB instance to run as a server mode.
     * If this mode is configured as true, then OVSDB will run as both OVSDB client and server.
     * If this mode is configured as false, then OVSDB will run as OVS client only.
     *
     * @param serverMode server mode flag
     */
    void setServerMode(boolean serverMode);
}
