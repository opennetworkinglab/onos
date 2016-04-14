/*
 *  Copyright 2016-present Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.model.topo;

import org.onosproject.cluster.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.TreeSet;

/**
 * Represents the overall network topology.
 */
public class UiTopology extends UiElement {

    private static final Logger log = LoggerFactory.getLogger(UiTopology.class);

    private final UiCluster uiCluster = new UiCluster();
    private final Set<UiRegion> uiRegions = new TreeSet<>();

    @Override
    public String toString() {
        return "Topology: " + uiCluster + ", " + uiRegions.size() + " regions";
    }

    /**
     * Clears the topology state; that is, drops all regions, devices, hosts,
     * links, and cluster members.
     */
    public void clear() {
        log.debug("clearing topology model");
        uiRegions.clear();
        uiCluster.clear();
    }

    /**
     * Returns the cluster member with the given identifier, or null if no
     * such member.
     *
     * @param id cluster node identifier
     * @return the cluster member with that identifier
     */
    public UiClusterMember findClusterMember(NodeId id) {
        return uiCluster.find(id);
    }

    /**
     * Adds the given cluster member to the topology model.
     *
     * @param member cluster member to add
     */
    public void add(UiClusterMember member) {
        uiCluster.add(member);
    }

    /**
     * Returns the number of members in the cluster.
     *
     * @return number of cluster members
     */
    public int clusterMemberCount() {
        return uiCluster.size();
    }

    /**
     * Returns the number of regions configured in the topology.
     *
     * @return number of regions
     */
    public int regionCount() {
        return uiRegions.size();
    }
}
