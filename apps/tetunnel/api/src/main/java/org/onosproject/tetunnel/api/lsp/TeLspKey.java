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

package org.onosproject.tetunnel.api.lsp;

import com.google.common.base.Objects;
import org.onosproject.tetopology.management.api.TeTopologyKey;

/**
 * Representation of a TE LSP key, which identifies a TE Label-switched path
 * globally.
 */
public class TeLspKey extends TeTopologyKey {

    private final long teLspId;

    /**
     * Creates an instance of TeLspKey with supplied information.
     *
     * @param providerId provider identifier
     * @param clientId   client identifier
     * @param topologyId topology identifier
     * @param teLspId TE LSP identifier
     */
    public TeLspKey(long providerId, long clientId,
                    long topologyId, long teLspId) {
        super(providerId, clientId, topologyId);
        this.teLspId = teLspId;
    }

    /**
     * Creates an instance of TeLspKey with specified TeTopologyKey and
     * supplied TE LSP identifier.
     *
     * @param key the key of TE Topology to which this LSP belongs
     * @param teLspId TE LSP identifier
     */
    public TeLspKey(TeTopologyKey key, long teLspId) {
        super(key.providerId(), key.clientId(), key.topologyId());
        this.teLspId = teLspId;
    }

    /**
     * Returns the TE LSP identifier corresponding to this key.
     *
     * @return TE LSP identifier
     */
    public long teLspId() {
        return teLspId;
    }

    /**
     * Returns the key of the TE topology to which this LSP belongs.
     *
     * @return corresponding TE topology key
     */
    public TeTopologyKey teTopologyKey() {
        return new TeTopologyKey(providerId(), clientId(), topologyId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), teLspId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TeLspKey) {
            if (!super.equals(object)) {
                return false;
            }
            TeLspKey that = (TeLspKey) object;
            return Objects.equal(this.teLspId, that.teLspId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper()
                .add("topologyId", topologyId())
                .add("teLspId", teLspId)
                .toString();
    }
}

