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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the notion of the ONOS cluster.
 */
class UiCluster extends UiElement {

    private final List<UiClusterMember> members = new ArrayList<>();
    private final Map<NodeId, UiClusterMember> lookup = new HashMap<>();

    @Override
    public String toString() {
        return String.valueOf(members.size()) + "-member cluster";
    }

    /**
     * Removes all cluster members.
     */
    void clear() {
        members.clear();
    }

    /**
     * Returns the cluster member with the given identifier, or null if no
     * such member exists.
     *
     * @param id identifier of member to find
     * @return corresponding member
     */
    public UiClusterMember find(NodeId id) {
        return lookup.get(id);
    }

    /**
     * Adds the given member to the cluster.
     *
     * @param member member to add
     */
    public void add(UiClusterMember member) {
        members.add(member);
        lookup.put(member.id(), member);
    }

    /**
     * Returns the number of members in the cluster.
     *
     * @return number of members
     */
    public int size() {
        return members.size();
    }
}
