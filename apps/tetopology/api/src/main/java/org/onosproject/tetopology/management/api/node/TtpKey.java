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

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Objects;

/**
 * Representation of a TE tunnel termination point key.
 */
public class TtpKey extends TeNodeKey {
    private final long ttpId;

    /**
     * Creates a TE tunnel termination point key.
     *
     * @param providerId provider identifier
     * @param clientId   client identifier
     * @param topologyId topology identifier
     * @param teNodeId   TE node identifier
     * @param ttpId      tunnel termination point identifier
     */
    public TtpKey(long providerId, long clientId, long topologyId,
                  long teNodeId, long ttpId) {
        super(providerId, clientId, topologyId, teNodeId);
        this.ttpId = ttpId;
    }

    /**
     * Creates a TE tunnel termination point key based on a given TE node
     * key and a tunnel termination point identifier.
     *
     * @param teNodeKey TE node key
     * @param ttpId     tunnel termination point id
     */
    public TtpKey(TeNodeKey teNodeKey, long ttpId) {
        super(teNodeKey.providerId(), teNodeKey.clientId(),
              teNodeKey.topologyId(), teNodeKey.teNodeId());
        this.ttpId = ttpId;
    }

    /**
     * Returns the TE node key.
     *
     * @return the TE node key
     */
    public TeNodeKey teNodekey() {
        return new TeNodeKey(providerId(), clientId(), topologyId(), teNodeId());
    }

    /**
     * Returns the tunnel termination point identifier.
     *
     * @return the tunnel termination point id
     */
    public long ttpId() {
        return ttpId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), ttpId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TtpKey) {
            if (!super.equals(object)) {
                return false;
            }
            TtpKey that = (TtpKey) object;
            return Objects.equal(ttpId, that.ttpId);
        }
        return false;
    }

    /**
     * Returns ToStringHelper with an additional tunnel termination point
     * identifier.
     *
     * @return toStringHelper
     */
    protected ToStringHelper toTtpKeyStringHelper() {
        return toTeNodeKeyStringHelper().add("ttpId", ttpId);
    }

    @Override
    public String toString() {
        return toTtpKeyStringHelper().toString();
    }

}
