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

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Objects;

/**
 * TE topology key in long integer format.
 */
public class TeTopologyKey extends ProviderClientId {
    private final long topologyId;

    /**
     * Creates an instance of TE topology identifier.
     *
     * @param providerId provider identifier
     * @param clientId   client identifier
     * @param topologyId topology identifier
     */
    public TeTopologyKey(long providerId, long clientId, long topologyId) {
        super(providerId, clientId);
        this.topologyId = topologyId;
    }

    /**
     * Returns the topology identifier.
     *
     * @return topology identifier
     */
    public long topologyId() {
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
        if (object instanceof TeTopologyKey) {
            if (!super.equals(object)) {
                return false;
            }
            TeTopologyKey that = (TeTopologyKey) object;
            return Objects.equal(topologyId, that.topologyId);
        }
        return false;
    }

    /**
     * Returns ToStringHelper with additional topologyId.
     *
     * @return toStringHelper
     */
    protected ToStringHelper toTopologyKeyStringHelper() {
        return toStringHelper().add("topologyId", topologyId);
    }

    @Override
    public String toString() {
        return toTopologyKeyStringHelper().toString();
    }
}
