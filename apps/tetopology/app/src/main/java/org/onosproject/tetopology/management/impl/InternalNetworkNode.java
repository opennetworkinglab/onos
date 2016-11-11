/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.tetopology.management.impl;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TerminationPoint;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Network Node representation in store.
 */
public class InternalNetworkNode {
    private List<NetworkNodeKey> supportingNodeIds;
    private List<KeyId> tpIds;
    private TeNodeKey teNodeKey;
    private boolean parentUpdate;
    private boolean childUpdate;

    /**
     * Creates an instance of InternalNetworkNode.
     *
     * @param node the network node
     * @param parentUpdate the flag if the data is updated by parent
     */
    public InternalNetworkNode(NetworkNode node,  boolean parentUpdate) {
        supportingNodeIds = node
                .supportingNodeIds() == null ? null
                                             : Lists.newArrayList(node
                                                     .supportingNodeIds());
        if (MapUtils.isNotEmpty(node.terminationPoints())) {
            tpIds = Lists.newArrayList();
            for (Map.Entry<KeyId, TerminationPoint> entry : node
                    .terminationPoints().entrySet()) {
                tpIds.add(entry.getKey());
            }
        }
        this.parentUpdate = parentUpdate;
    }

    /**
     * Returns the list of supporting node Ids.
     *
     * @return the supporting nodeIds
     */
    public List<NetworkNodeKey> supportingNodeIds() {
        return supportingNodeIds == null ? null
                                         : ImmutableList
                                                 .copyOf(supportingNodeIds);
    }

    /**
     * Sets the list of supporting node Ids.
     *
     * @param supportingNodeIds the supportingNodeIds to set
     */
    public void setSupportingNodeIds(List<NetworkNodeKey> supportingNodeIds) {
        this.supportingNodeIds = supportingNodeIds == null ? null
                                                           : Lists.newArrayList(supportingNodeIds);
    }

    /**
     * Returns the list of termination point Ids.
     *
     * @return the termination point Ids
     */
    public List<KeyId> tpIds() {
        return tpIds;
    }

    /**
     * Sets the list of termination point Ids.
     *
     * @param tpIds the tpIds to set
     */
    public void setTpIds(List<KeyId> tpIds) {
        this.tpIds = tpIds;
    }

    /**
     * Returns the TE Node key.
     *
     * @return the teNodeKey
     */
    public TeNodeKey teNodeKey() {
        return teNodeKey;
    }

    /**
     * Sets the TE Node key.
     *
     * @param teNodeKey the teNodeKey to set
     */
    public void setTeNodeKey(TeNodeKey teNodeKey) {
        this.teNodeKey = teNodeKey;
    }

    /**
     * Returns the flag if the data was updated by parent change.
     *
     * @return value of parentUpdate
     */
    public boolean parentUpdate() {
        return parentUpdate;
    }

    /**
     * Returns the flag if the data was updated by child change.
     *
     * @return value of childUpdate
     */
    public boolean childUpdate() {
        return childUpdate;
    }

    /**
     * Sets the flag if the data was updated by child change.
     *
     * @param childUpdate the childUpdate value to set
     */
    public void setChildUpdate(boolean childUpdate) {
        this.childUpdate = childUpdate;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(supportingNodeIds, tpIds, teNodeKey);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof InternalNetworkNode) {
            InternalNetworkNode that = (InternalNetworkNode) object;
            return Objects.equal(supportingNodeIds, that.supportingNodeIds)
                    && Objects.equal(tpIds, that.tpIds)
                    && Objects.equal(teNodeKey, that.teNodeKey);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("supportingNodeIds", supportingNodeIds)
                .add("tpIds", tpIds)
                .add("teNodeKey", teNodeKey)
                .add("parentUpdate", parentUpdate)
                .add("childUpdate", childUpdate)
                .toString();
    }
}
