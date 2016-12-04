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

package org.onosproject.tetunnel.api.tunnel;

import com.google.common.base.Objects;
import org.onosproject.tetopology.management.api.TeTopologyKey;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Representation of a TE tunnel key, which identifies a TE tunnel globally.
 */
public class TeTunnelKey extends TeTopologyKey {

    private final long teTunnelId;

    /**
     * Creates an instance of TE tunnel key with supplied information.
     *
     * @param providerId provider identifier
     * @param clientId client identifier
     * @param topologyId topology identifier
     * @param teTunnelId TE tunnel identifier
     */
    public TeTunnelKey(long providerId, long clientId,
                     long topologyId, long teTunnelId) {
        super(providerId, clientId, topologyId);
        this.teTunnelId = teTunnelId;
    }

    /**
     * Creates an instance of TE tunnel key with specified TeTopologyKey and
     * supplied TE tunnel identifier.
     *
     * @param key the key of TE topology to which this tunnel belongs
     * @param tunnelId TE tunnel identifier
     */
    public TeTunnelKey(TeTopologyKey key, long tunnelId) {
        super(key.providerId(), key.clientId(), key.topologyId());
        this.teTunnelId = tunnelId;
    }

    /**
     * Returns the TE tunnel identifier.
     *
     * @return TE tunnel identifier
     */
    public long teTunnelId() {
        return teTunnelId;
    }

    /**
     * Returns key of the TE topology to which this tunnel belongs.
     *
     * @return corresponding TE topology key
     */
    public TeTopologyKey teTopologyKey() {
        return new TeTopologyKey(providerId(), clientId(), topologyId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), teTunnelId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TeTunnelKey) {
            if (!super.equals(object)) {
                return false;
            }
            TeTunnelKey that = (TeTunnelKey) object;
            return Objects.equal(this.teTunnelId, that.teTunnelId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper()
                .add("topologyId", topologyId())
                .add("teTunnelId", teTunnelId)
                .toString();
    }
}

