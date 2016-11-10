/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.node;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onosproject.tetopology.management.api.KeyId;

import java.util.List;
import java.util.Map;

/**
 * Default network node implementation.
 */
public class DefaultNetworkNode implements NetworkNode {
    private final KeyId id;
    private final List<NetworkNodeKey> supportingNodeIds;
    private final TeNode teNode;
    private final Map<KeyId, TerminationPoint> tps;


    /**
     * Creates a network node instance.
     *
     * @param id      network node identifier
     * @param nodeIds support node identifiers
     * @param teNode  te parameter of the node
     * @param tps     the tps to set
     */
    public DefaultNetworkNode(KeyId id,
                              List<NetworkNodeKey> nodeIds,
                              TeNode teNode,
                              Map<KeyId, TerminationPoint> tps) {
        this.id = id;
        this.supportingNodeIds = nodeIds != null ?
                Lists.newArrayList(nodeIds) : null;
        this.teNode = teNode;
        this.tps = tps != null ? Maps.newHashMap(tps) : null;
    }

    /**
     * Returns the node identifier.
     *
     * @return node identifier
     */
    @Override
    public KeyId nodeId() {
        return id;
    }

    /**
     * Returns the list of supporting node identifiers for this node.
     *
     * @return list of supporting node identifiers
     */
    @Override
    public List<NetworkNodeKey> supportingNodeIds() {
        if (supportingNodeIds == null) {
            return null;
        }
        return ImmutableList.copyOf(supportingNodeIds);
    }

    /**
     * Returns the node TE attributes.
     *
     * @return TE attributes of this node
     */
    @Override
    public TeNode teNode() {
        return teNode;
    }

    /**
     * Returns the list of termination points associated with this node.
     *
     * @return a list of termination points
     */
    @Override
    public Map<KeyId, TerminationPoint> terminationPoints() {
        if (tps == null) {
            return null;
        }
        return ImmutableMap.copyOf(tps);
    }

    /**
     * Returns the termination point.
     *
     * @return the termination point
     */
    @Override
    public TerminationPoint terminationPoint(KeyId tpId) {
        return tps.get(tpId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, supportingNodeIds, teNode, tps);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DefaultNetworkNode) {
            DefaultNetworkNode that = (DefaultNetworkNode) object;
            return Objects.equal(id, that.id) &&
                    Objects.equal(supportingNodeIds, that.supportingNodeIds) &&
                    Objects.equal(teNode, that.teNode) &&
                    Objects.equal(tps, that.tps);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("supportingNodeIds", supportingNodeIds)
                .add("teNode", teNode)
                .add("tps", tps)
                .toString();
    }

}
