/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.flow.criteria;

import java.util.Objects;
/**
 * Implementation of Tunnel ID criterion.
 */
public class TunnelIdCriterion implements Criterion {
    private final long tunnelId;

    /**
     * Constructor.
     *
     * @param tunnelId a Tunnel ID to match(64 bits)
     */
    TunnelIdCriterion(long tunnelId) {
        this.tunnelId = tunnelId;
    }

    @Override
    public Type type() {
        return Type.TUNNEL_ID;
    }

    /**
     * Gets the Tunnel ID to match.
     *
     * @return the Tunnel ID to match (64 bits)
     */
    public long tunnelId() {
        return tunnelId;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + Long.toHexString(tunnelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), tunnelId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TunnelIdCriterion) {
            TunnelIdCriterion that = (TunnelIdCriterion) obj;
            return Objects.equals(tunnelId, that.tunnelId) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
