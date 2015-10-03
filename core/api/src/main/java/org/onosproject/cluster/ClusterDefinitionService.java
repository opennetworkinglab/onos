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
package org.onosproject.cluster;

import java.util.Set;

/**
 * Service for obtaining the static definition of a controller cluster.
 */
public interface ClusterDefinitionService {

    /**
     * Returns the local controller node.
     * @return local controller node
     */
    ControllerNode localNode();

    /**
     * Returns the set of seed nodes that should be used for discovering other members
     * of the cluster.
     * @return set of seed controller nodes
     */
    Set<ControllerNode> seedNodes();

    /**
     * Forms cluster configuration based on the specified set of node
     * information. Assumes subsequent restart for the new configuration to
     * take hold.
     *
     * @param nodes    set of nodes that form the cluster
     * @param ipPrefix IP address prefix, e.g. 10.0.1.*
     */
    void formCluster(Set<ControllerNode> nodes, String ipPrefix);
}