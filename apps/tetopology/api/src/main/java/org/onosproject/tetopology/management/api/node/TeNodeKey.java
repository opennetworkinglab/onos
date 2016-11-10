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

import org.onosproject.tetopology.management.api.TeTopologyKey;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Objects;

/**
 * TE node Key.
 */
public class TeNodeKey extends TeTopologyKey {
    private final long teNodeId;

    /**
     * Creates a TE node key.
     *
     * @param providerId provider identifier
     * @param clientId   client identifier
     * @param topologyId topology identifier
     * @param teNodeId   TE node identifier
     */
    public TeNodeKey(long providerId, long clientId,
                     long topologyId, long teNodeId) {
        super(providerId, clientId, topologyId);
        this.teNodeId = teNodeId;
    }

    /**
     * Creates a TE node key based on a given TE topology key and a
     * TE node identifier.
     *
     * @param teTopologyKey the key of TE Topology to which this node belongs
     * @param nodeId        TE node identifier
     */
    public TeNodeKey(TeTopologyKey teTopologyKey, long nodeId) {
        super(teTopologyKey.providerId(), teTopologyKey.clientId(),
              teTopologyKey.topologyId());
        this.teNodeId = nodeId;
    }

    /**
     * Returns the TE Node identifier.
     *
     * @return the TE node id
     */
    public long teNodeId() {
        return teNodeId;
    }

    public TeTopologyKey teTopologyKey() {
        return new TeTopologyKey(providerId(), clientId(), topologyId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), teNodeId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TeNodeKey) {
            if (!super.equals(object)) {
                return false;
            }
            TeNodeKey that = (TeNodeKey) object;
            return Objects.equal(this.teNodeId, that.teNodeId);
        }
        return false;
    }

    /**
     * Returns ToStringHelper with an additional TE node identifier.
     *
     * @return toStringHelper
     */
    protected ToStringHelper toTeNodeKeyStringHelper() {
        return toTopologyKeyStringHelper().add("teNodeId", teNodeId);
    }

    @Override
    public String toString() {
        return toTeNodeKeyStringHelper().toString();
    }
}
