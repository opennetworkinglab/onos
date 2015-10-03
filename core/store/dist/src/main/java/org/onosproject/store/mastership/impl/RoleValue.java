/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.mastership.impl;

import static org.onosproject.net.MastershipRole.MASTER;
import static org.onosproject.net.MastershipRole.NONE;
import static org.onosproject.net.MastershipRole.STANDBY;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.net.MastershipRole;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Lists;

/**
 * A structure that holds node mastership roles associated with a
 * {@link org.onosproject.net.DeviceId}. This structure needs to be locked through IMap.
 */
final class RoleValue {

    protected final Map<MastershipRole, List<NodeId>> value = new EnumMap<>(MastershipRole.class);

    /**
     * Constructs empty RoleValue.
     */
    public RoleValue() {
        value.put(MastershipRole.MASTER, new LinkedList<>());
        value.put(MastershipRole.STANDBY, new LinkedList<>());
        value.put(MastershipRole.NONE, new LinkedList<>());
    }

    /**
     * Constructs copy of specified RoleValue.
     *
     * @param original original to create copy from
     */
    public RoleValue(final RoleValue original) {
        value.put(MASTER, Lists.newLinkedList(original.value.get(MASTER)));
        value.put(STANDBY, Lists.newLinkedList(original.value.get(STANDBY)));
        value.put(NONE, Lists.newLinkedList(original.value.get(NONE)));
    }

    // exposing internals for serialization purpose only
    Map<MastershipRole, List<NodeId>> value() {
        return Collections.unmodifiableMap(value);
    }

    public List<NodeId> nodesOfRole(MastershipRole type) {
        return value.get(type);
    }

    /**
     * Returns the first node to match the MastershipRole, or if there
     * are none, null.
     *
     * @param type the role
     * @return a node ID or null
     */
    public NodeId get(MastershipRole type) {
        return value.get(type).isEmpty() ? null : value.get(type).get(0);
    }

    public boolean contains(MastershipRole type, NodeId nodeId) {
        return value.get(type).contains(nodeId);
    }

    public MastershipRole getRole(NodeId nodeId) {
        if (contains(MASTER, nodeId)) {
            return MASTER;
        }
        if (contains(STANDBY, nodeId)) {
            return STANDBY;
        }
        return NONE;
    }

    /**
     * Associates a node to a certain role.
     *
     * @param type the role
     * @param nodeId the node ID of the node to associate
     * @return true if modified
     */
    public boolean add(MastershipRole type, NodeId nodeId) {
        List<NodeId> nodes = value.get(type);

        if (!nodes.contains(nodeId)) {
            return nodes.add(nodeId);
        }
        return false;
    }

    /**
     * Removes a node from a certain role.
     *
     * @param type the role
     * @param nodeId the ID of the node to remove
     * @return true if modified
     */
    public boolean remove(MastershipRole type, NodeId nodeId) {
        List<NodeId> nodes = value.get(type);
        if (!nodes.isEmpty()) {
            return nodes.remove(nodeId);
        } else {
            return false;
        }
    }

    /**
     * Reassigns a node from one role to another. If the node was not of the
     * old role, it will still be assigned the new role.
     *
     * @param nodeId the Node ID of node changing roles
     * @param from the old role
     * @param to the new role
     * @return true if modified
     */
    public boolean reassign(NodeId nodeId, MastershipRole from, MastershipRole to) {
        boolean modified = remove(from, nodeId);
        modified |= add(to, nodeId);
        return modified;
    }

    /**
     * Replaces a node in one role with another node. Even if there is no node to
     * replace, the new node is associated to the role.
     *
     * @param from the old NodeId to replace
     * @param to the new NodeId
     * @param type the role associated with the old NodeId
     * @return true if modified
     */
    public boolean replace(NodeId from, NodeId to, MastershipRole type) {
        boolean modified = remove(type, from);
        modified |= add(type, to);
        return modified;
    }

    /**
     * Summarizes this RoleValue as a RoleInfo. Note that master and/or backups
     * may be empty, so the values should be checked for safety.
     *
     * @return the RoleInfo.
     */
    public RoleInfo roleInfo() {
        return new RoleInfo(
                get(MastershipRole.MASTER), nodesOfRole(MastershipRole.STANDBY));
    }

    @Override
    public String toString() {
        ToStringHelper helper = MoreObjects.toStringHelper(this.getClass());
        for (Map.Entry<MastershipRole, List<NodeId>> el : value.entrySet()) {
            helper.add(el.getKey().toString(), el.getValue());
        }
        return helper.toString();
    }
}
