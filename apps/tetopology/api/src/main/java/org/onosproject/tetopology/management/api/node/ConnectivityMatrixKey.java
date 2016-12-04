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
 * Representation of a TE connectivity matrix entry key.
 */
public class ConnectivityMatrixKey extends TeNodeKey {
    private final long entryId;

    /**
     * Creates a connectivity matrix key.
     *
     * @param providerId provider identifier
     * @param clientId   client identifier
     * @param topologyId topology identifier
     * @param teNodeId   TE node identifier
     * @param entryId    connectivity matrix entry id
     */
    public ConnectivityMatrixKey(long providerId, long clientId,
                                 long topologyId, long teNodeId,
                                 long entryId) {
        super(providerId, clientId, topologyId, teNodeId);
        this.entryId = entryId;
    }

    /**
     * Creates a connectivity matrix key base on a given TE node key.
     *
     * @param teNodeKey TE node key
     * @param entryId   connectivity matrix entry id
     */
    public ConnectivityMatrixKey(TeNodeKey teNodeKey, long entryId) {
        super(teNodeKey.providerId(), teNodeKey.clientId(),
              teNodeKey.topologyId(), teNodeKey.teNodeId());
        this.entryId = entryId;
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
     * Returns the connectivity matrix entry identifier.
     *
     * @return the connectivity matrix entry id
     */
    public long entryId() {
        return entryId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), entryId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof ConnectivityMatrixKey) {
            if (!super.equals(object)) {
                return false;
            }
            ConnectivityMatrixKey that = (ConnectivityMatrixKey) object;
            return Objects.equal(this.entryId, that.entryId);
        }
        return false;
    }

    /**
     * Returns ToStringHelper with additional entry identifier.
     *
     * @return toStringHelper
     */
    protected ToStringHelper toConnMatrixKeyStringHelper() {
        return toTeNodeKeyStringHelper().add("entryId", entryId);
    }

    @Override
    public String toString() {
        return toConnMatrixKeyStringHelper().toString();
    }

}
