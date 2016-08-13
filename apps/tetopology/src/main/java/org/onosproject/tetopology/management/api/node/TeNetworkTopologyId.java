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
import org.onosproject.tetopology.management.api.TeTopologyId;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * TE Network Topology identifiers.
 */
public class TeNetworkTopologyId {
    private final KeyId networkId;
    private final TeTopologyId topologyId;

    /**
     * Creates an instance of TeNetworkTopologyId.
     *
     * @param networkId network identifier
     * @param topologyId topology identifier
     */
    public TeNetworkTopologyId(KeyId networkId, TeTopologyId topologyId) {
        this.networkId = networkId;
        this.topologyId = topologyId;
    }

    /**
     * Creates TeNetworkTopologyId with networkId.
     *
     * @param networkId network identifier
     */
    public TeNetworkTopologyId(KeyId networkId) {
        this.networkId = networkId;
        this.topologyId = null;
    }

    /**
     * Creates TeNetworkTopologyId with topologyId.
     *
     * @param topologyId topology identifier
     */
    public TeNetworkTopologyId(TeTopologyId topologyId) {
        this.networkId = null;
        this.topologyId = topologyId;
    }

    /**
     * Returns the network identifier.
     *
     * @return network id
     */
    public KeyId getNetworkId() {
        return networkId;
    }

    /**
     * Returns the topology identifier.
     *
     * @return TE topology id
     */
    public TeTopologyId getTopologyId() {
        return topologyId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(networkId, topologyId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TeNetworkTopologyId) {
            TeNetworkTopologyId that = (TeNetworkTopologyId) object;
            return Objects.equal(this.networkId, that.networkId) &&
                    Objects.equal(this.topologyId, that.topologyId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("networkId", networkId)
                .add("topologyId", topologyId)
                .toString();
    }

}
