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

import org.onosproject.tetopology.management.api.KeyId;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Representation of a node's termination point key under a known network.
 */
public class NodeTpKey {
    private final KeyId nodeId;
    private final KeyId tpId;

    /**
     * Creates a node's termination point key.
     *
     * @param nodeId node identifier
     * @param tpId   termination point identifier
     */
    public NodeTpKey(KeyId nodeId, KeyId tpId) {
        this.nodeId = nodeId;
        this.tpId = tpId;
    }

    /**
     * Returns the node identifier.
     *
     * @return node id
     */
    public KeyId nodeId() {
        return nodeId;
    }

    /**
     * Returns the termination point identifier.
     *
     * @return termination point identifier
     */
    public KeyId tpId() {
        return tpId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nodeId, tpId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof NodeTpKey) {
            NodeTpKey that = (NodeTpKey) object;
            return Objects.equal(nodeId, that.nodeId) &&
                    Objects.equal(tpId, that.tpId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("nodeId", nodeId)
                .add("tpId", tpId)
                .toString();
    }

}
