/*
 * Copyright 2015-present Open Networking Laboratory
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
}
