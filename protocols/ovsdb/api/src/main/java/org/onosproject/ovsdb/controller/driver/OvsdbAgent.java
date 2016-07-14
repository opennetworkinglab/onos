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
package org.onosproject.ovsdb.controller.driver;

import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.OvsdbClientService;

/**
 * Responsible for keeping track of the current set of nodes connected to the
 * system.
 */
public interface OvsdbAgent {
    /**
     * Add a node that has just connected to the system.
     *
     * @param nodeId the nodeId to add
     * @param ovsdbClient the actual node object.
     */
    void addConnectedNode(OvsdbNodeId nodeId, OvsdbClientService ovsdbClient);

    /**
     * Clear all state in controller node maps for a node that has disconnected
     * from the local controller. Also release control for that node from the
     * global repository. Notify node listeners.
     *
     * @param nodeId the node id to be removed.
     */
    void removeConnectedNode(OvsdbNodeId nodeId);
}
