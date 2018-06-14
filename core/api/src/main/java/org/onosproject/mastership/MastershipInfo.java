/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.mastership;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.MastershipRole;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Mastership info.
 */
public final class MastershipInfo {
    private final long term;
    private final Optional<NodeId> master;
    private final ImmutableMap<NodeId, MastershipRole> roles;

    public MastershipInfo() {
        this(0, Optional.empty(), ImmutableMap.of());
    }

    public MastershipInfo(long term, Optional<NodeId> master, ImmutableMap<NodeId, MastershipRole> roles) {
        this.term = term;
        this.master = master;
        this.roles = roles;
    }

    /**
     * Returns the mastership term.
     *
     * @return the mastership term
     */
    public long term() {
        return term;
    }

    /**
     * Returns the current master.
     *
     * @return the current master
     */
    public Optional<NodeId> master() {
        return master;
    }

    /**
     * Returns a sorted list of standby nodes.
     *
     * @return a sorted list of standby nodes
     */
    public List<NodeId> backups() {
        return getRoles(MastershipRole.STANDBY);
    }

    /**
     * Returns the list of nodes with the given role.
     *
     * @param role the role by which to filter nodes
     * @return an immutable list of nodes with the given role sorted in priority order
     */
    public List<NodeId> getRoles(MastershipRole role) {
        return ImmutableList.copyOf(roles.entrySet()
            .stream()
            .filter(entry -> entry.getValue() == role)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList()));
    }

    /**
     * Returns the current role for the given node.
     *
     * @param nodeId the node for which to return the current role
     * @return the current role for the given node
     */
    public MastershipRole getRole(NodeId nodeId) {
        return roles.get(nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, master, roles);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof MastershipInfo) {
            MastershipInfo that = (MastershipInfo) object;
            return this.term == that.term
                && Objects.equals(this.master, that.master)
                && Objects.equals(this.roles, that.roles);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
            .add("term", term)
            .add("master", master)
            .add("roles", roles)
            .toString();
    }
}
