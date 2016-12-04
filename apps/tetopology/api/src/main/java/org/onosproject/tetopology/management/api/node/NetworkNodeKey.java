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
 * Representation of a node key or node reference.
 */
public class NetworkNodeKey {
    private final KeyId networkId;
    private final KeyId nodeId;

    /**
     * Creates an instance of NetworkNodeKey.
     *
     * @param networkId network identifier
     * @param nodeId node identifier
     */
    public NetworkNodeKey(KeyId networkId, KeyId nodeId) {
        this.networkId = networkId;
        this.nodeId = nodeId;
    }

    /**
     * Returns the network identifier.
     *
     * @return network identifier
     */
    public KeyId networkId() {
        return networkId;
    }

    /**
     * Returns the node identifier.
     *
     * @return node identifier
     */
    public KeyId nodeId() {
        return nodeId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(networkId, nodeId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof NetworkNodeKey) {
            NetworkNodeKey that = (NetworkNodeKey) object;
            return Objects.equal(this.networkId, that.networkId) &&
                    Objects.equal(this.nodeId, that.nodeId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("networkId", networkId)
                .add("nodeId", nodeId)
                .toString();
    }

}
