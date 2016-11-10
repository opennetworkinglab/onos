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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Representation of a TE tunnel identifier.
 */
public class TeTunnelId {
    private final long srcTeNodeId;
    private final long dstTeNodeId;
    private final long tunnelId;

    /**
     * Create a TE tunnel identifier.
     *
     * @param srcTeNodeId source TE node id
     * @param dstTeNodeId destination TE node id
     * @param tunnelId    tunnel id
     */
    public TeTunnelId(long srcTeNodeId, long dstTeNodeId, long tunnelId) {
        this.srcTeNodeId = srcTeNodeId;
        this.dstTeNodeId = dstTeNodeId;
        this.tunnelId = tunnelId;
    }

    /**
     * Returns the source TE node identifier of the tunnel.
     *
     * @return the source TE node id
     */
    public long sourceTeNodeId() {
        return srcTeNodeId;
    }

    /**
     * Returns the destination TE node identifier of the tunnel.
     *
     * @return the destination TE node id
     */
    public long destinationTeNodeId() {
        return dstTeNodeId;
    }

    /**
     * Returns the tunnel identifier.
     *
     * @return the tunnel id
     */
    public long tunnelId() {
        return tunnelId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(srcTeNodeId, dstTeNodeId, tunnelId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TeTunnelId) {
            TeTunnelId that = (TeTunnelId) object;
            return (srcTeNodeId == that.srcTeNodeId) &&
                    (dstTeNodeId == that.dstTeNodeId) &&
                    (tunnelId == that.tunnelId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("srcTeNodeId", srcTeNodeId)
                .add("dstTeNodeId", dstTeNodeId)
                .add("tunnelId", tunnelId)
                .toString();
    }

}
