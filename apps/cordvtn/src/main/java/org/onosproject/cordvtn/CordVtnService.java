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

import org.onosproject.cordvtn.OvsdbNode.State;
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
     * @param ovsdbNode ovsdb node
     */
    void addNode(OvsdbNode ovsdbNode);

    /**
     * Deletes a node from the service.
     *
     * @param ovsdbNode ovsdb node
     */
    void deleteNode(OvsdbNode ovsdbNode);

    /**
     * Updates ovsdb node.
     * It only used for updating node's connection state.
     *
     * @param ovsdbNode ovsdb node
     * @param state ovsdb connection state
     */
    void updateNode(OvsdbNode ovsdbNode, State state);

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
     * Returns all nodes known to the service.
     *
     * @return list of nodes
     */
    List<OvsdbNode> getNodes();
}
