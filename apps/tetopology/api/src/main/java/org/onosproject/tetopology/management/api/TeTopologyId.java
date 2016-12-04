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
package org.onosproject.tetopology.management.api;

import com.google.common.base.Objects;

/**
 * TE Topology identifier in String format.
 */
public class TeTopologyId extends ProviderClientId {
    private final String topologyId;

    /**
     * Creates an instance of TE topology identifier.
     *
     * @param providerId value of provider identifier
     * @param clientId   value of client identifier
     * @param topologyId value of topology identifier
     */
    public TeTopologyId(long providerId, long clientId, String topologyId) {
        super(providerId, clientId);
        this.topologyId = topologyId;
    }

    /**
     * Returns the topology identifier.
     *
     * @return topology identifier
     */
    public String topologyId() {
        return topologyId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), topologyId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TeTopologyId) {
            if (!super.equals(object)) {
                return false;
            }
            TeTopologyId that = (TeTopologyId) object;
            return Objects.equal(this.topologyId, that.topologyId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper()
                .add("topologyId", topologyId)
                .toString();
    }
}
