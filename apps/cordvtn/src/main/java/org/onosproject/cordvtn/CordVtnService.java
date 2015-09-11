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

import java.util.List;

/**
 * Service for provisioning overlay virtual networks on compute nodes.
 */
public interface CordVtnService {
    /**
     * Adds new nodes to the service and processes initial setup.
     *
     * @param ovsdbNodes list of nodes
     */
    void addNodes(List<OvsdbNode> ovsdbNodes);

    /**
     * Deletes the nodes from the service and cleans up unnecessary configurations
     * associated with the deleted nodes.
     *
     * @param ovsdbNodes list of nodes
     */
    void deleteNodes(List<OvsdbNode> ovsdbNodes);

    /**
     * Returns the number of the nodes known to the service.
     *
     * @return number of nodes
     */
    int getNodeCount();

    /**
     * Returns all nodes known to the service.
     *
     * @return list of nodes
     */
    List<OvsdbNode> getNodes();
}
