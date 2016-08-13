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

import org.onosproject.tetopology.management.api.KeyId;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Representation of an external domain link.
 */
public class ExternalDomain {
    private final KeyId remoteTeNodeId;
    private final KeyId remoteTeLinkTpId;
    private final long plugId;

    /**
     * Creates an instance of ExternalDomain.
     *
     * @param remoteTeNodeId remote TE node identifier
     * @param remoteTeLinkTpId remote TE link termination point identifier
     * @param plugId global plug id
     */
    public ExternalDomain(KeyId remoteTeNodeId, KeyId remoteTeLinkTpId, long plugId) {
        this.remoteTeNodeId = remoteTeNodeId;
        this.remoteTeLinkTpId = remoteTeLinkTpId;
        this.plugId = plugId;
    }

    /**
     * Creates an instance of ExternalDomain with remote TE node and tp.
     *
     * @param remoteTeNodeId remote TE node identifier
     * @param remoteTeLinkTpId remote TE link termination point identifier
     */
    public ExternalDomain(KeyId remoteTeNodeId, KeyId remoteTeLinkTpId) {
        this.remoteTeNodeId = remoteTeNodeId;
        this.remoteTeLinkTpId = remoteTeLinkTpId;
        this.plugId = 0L;
    }

    /**
     * Creates an instance of ExternalDomain with plugId.
     *
     * @param plugId global plug id
     */
    public ExternalDomain(long plugId) {
        this.remoteTeNodeId = null;
        this.remoteTeLinkTpId = null;
        this.plugId = plugId;
    }

    /**
     * Returns the remote TeNode Id.
     *
     * @return value of the remote TE node identifier
     */
    public KeyId remoteTeNodeId() {
        return remoteTeNodeId;
    }

    /**
     * Returns the remote TeLink TpId.
     *
     * @return value of the remote TE link identifier
     */
    public KeyId remoteTeLinkTpId() {
        return remoteTeLinkTpId;
    }

    /**
     * Returns the plugId.
     *
     * @return value of the global plug id
     */
    public long plugId() {
        return plugId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(remoteTeNodeId, remoteTeLinkTpId, remoteTeLinkTpId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof ExternalDomain) {
            ExternalDomain that = (ExternalDomain) object;
            return Objects.equal(this.remoteTeNodeId, that.remoteTeNodeId) &&
                    Objects.equal(this.remoteTeLinkTpId, that.remoteTeLinkTpId) &&
                    Objects.equal(this.remoteTeLinkTpId, that.remoteTeLinkTpId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("remoteTeNodeId", remoteTeNodeId)
                .add("remoteTeLinkTpId", remoteTeLinkTpId)
                .add("remoteTeLinkTpId", remoteTeLinkTpId)
                .toString();
    }
}
