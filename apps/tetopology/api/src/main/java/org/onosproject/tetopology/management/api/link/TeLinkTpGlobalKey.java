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
package org.onosproject.tetopology.management.api.link;

import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.node.TeNodeKey;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Objects;

/**
 * Representation of a global TE link TP (i.e., TE termination point) key.
 */
public class TeLinkTpGlobalKey extends TeNodeKey {
    private final long teLinkTpId;

    /**
     * Creates a global TE link TP key.
     *
     * @param providerId provider identifier
     * @param clientId   client identifier
     * @param topologyId topology identifier
     * @param teNodeId   TE node identifier
     * @param teLinkTpId TE link termination point identifier
     */
    public TeLinkTpGlobalKey(long providerId, long clientId,
                             long topologyId, long teNodeId,
                             long teLinkTpId) {
        super(providerId, clientId, topologyId, teNodeId);
        this.teLinkTpId = teLinkTpId;
    }

    /**
     * Creates a global TE link TP key based on a given local TE node key.
     *
     * @param teNodeKey  the local TE node key
     * @param teLinkTpId TE link termination point identifier
     */
    public TeLinkTpGlobalKey(TeNodeKey teNodeKey, long teLinkTpId) {
        super(teNodeKey.providerId(), teNodeKey.clientId(),
              teNodeKey.topologyId(), teNodeKey.teNodeId());
        this.teLinkTpId = teLinkTpId;
    }

    /**
     * Creates a global TE link TP key based on a given TE topology key
     * and a local TE link TP key.
     *
     * @param teTopologyKey the key of TE Topology to which this link belongs
     * @param teLinkTpKey   the local TE link key
     */
    public TeLinkTpGlobalKey(TeTopologyKey teTopologyKey,
                             TeLinkTpKey teLinkTpKey) {
        super(teTopologyKey.providerId(), teTopologyKey.clientId(),
              teTopologyKey.topologyId(), teLinkTpKey.teNodeId());
        this.teLinkTpId = teLinkTpKey.teLinkTpId();
    }

    /**
     * Returns the TE link TP identifier.
     *
     * @return the TE link id
     */
    public long teLinkTpId() {
        return teLinkTpId;
    }

    /**
     * Returns the key of the TE node from which this link TP originates.
     *
     * @return the TE node key
     */
    public TeNodeKey teNodeKey() {
        return new TeNodeKey(providerId(), clientId(), topologyId(), teNodeId());
    }

    /**
     * Returns the key of the TE Topology to which this link TP belongs.
     *
     * @return the TE topology key
     */
    @Override
    public TeTopologyKey teTopologyKey() {
        return new TeTopologyKey(providerId(), clientId(), topologyId());
    }

    /**
     * Returns the local TE link TP key.
     *
     * @return the TE link TP key
     */
    public TeLinkTpKey teLinkTpKey() {
        return new TeLinkTpKey(teNodeId(), teLinkTpId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), teLinkTpId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TeLinkTpGlobalKey) {
            if (!super.equals(object)) {
                return false;
            }
            TeLinkTpGlobalKey that = (TeLinkTpGlobalKey) object;
            return Objects.equal(teLinkTpId, that.teLinkTpId);
        }
        return false;
    }

    /**
     * Returns a helper for toString() with additional TE link TP identifier.
     *
     * @return a toString helper
     */
    protected ToStringHelper toTeLinkTpKeyStringHelper() {
        return toTeNodeKeyStringHelper().add("teLinkTpId", teLinkTpId);
    }

    @Override
    public String toString() {
        return toTeLinkTpKeyStringHelper().toString();
    }

}
