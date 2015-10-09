/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.cordvtn;

import org.onosproject.net.DeviceId;

import java.util.List;

/**
 * Service for provisioning overlay virtual networks on compute nodes.
 */
public interface CordVtnService {

    String CORDVTN_APP_ID = "org.onosproject.cordvtn";
    /**
     * Adds a new node to the service.
     *
     * @param ovsdb ovsdb node
     */
    void addNode(OvsdbNode ovsdb);

    /**
     * Deletes a node from the service.
     *
     * @param ovsdb ovsdb node
     */
    void deleteNode(OvsdbNode ovsdb);

    /**
     * Connect to a node.
     *
     * @param ovsdb ovsdb node
     */
    void connect(OvsdbNode ovsdb);

    /**
     * Disconnect a node.
     *
     * @param ovsdb ovsdb node
     */
    void disconnect(OvsdbNode ovsdb);

    /**
     * Returns the number of the nodes known to the service.
     *
     * @return number of nodes
     */
    int getNodeCount();

    /**
     * Returns OvsdbNode with given device id.
     *
     * @param deviceId device id
     * @return ovsdb node
     */
    OvsdbNode getNode(DeviceId deviceId);

    /**
     * Returns connection state of the node.
     *
     * @param ovsdb ovsdb node
     * @return true if the node is connected, false otherwise
     */
    boolean isNodeConnected(OvsdbNode ovsdb);

    /**
     * Returns all nodes known to the service.
     *
     * @return list of nodes
     */
    List<OvsdbNode> getNodes();
}
