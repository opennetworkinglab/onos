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

package org.onosproject.segmentrouting.pwaas;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper class to carry the l2 tunnel
 * and its policy.
 */
public class DefaultL2TunnelDescription {

    /**
     * The l2 tunnel.
     */
    private DefaultL2Tunnel l2Tunnel;

    /**
     * The l2 tunnel policy.
     */
    private DefaultL2TunnelPolicy l2TunnelPolicy;

    /**
     * Creates a l2 tunnel description using the given info.
     *
     * @param l2Tunnel the l2 tunnel
     * @param l2TunnelPolicy the l2 tunnel description
     */
    public DefaultL2TunnelDescription(DefaultL2Tunnel l2Tunnel,
                                      DefaultL2TunnelPolicy l2TunnelPolicy) {
        checkNotNull(l2Tunnel);
        checkNotNull(l2TunnelPolicy);

        this.l2Tunnel = l2Tunnel;
        this.l2TunnelPolicy = l2TunnelPolicy;
    }

    /**
     * Creates an empty l2 tunnel description.
     */
    public DefaultL2TunnelDescription() {
        this.l2Tunnel = null;
        this.l2TunnelPolicy = null;
    }

    /**
     * Returns the l2 tunnel.
     *
     * @return the l2 tunnel
     */
    public DefaultL2Tunnel l2Tunnel() {
        return l2Tunnel;
    }

    /**
     * Returns the l2 tunnel policy.
     *
     * @return the l2 tunnel policy.
     */
    public DefaultL2TunnelPolicy l2TunnelPolicy() {
        return l2TunnelPolicy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.l2Tunnel, this.l2TunnelPolicy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (getClass() != o.getClass()) {
            return false;
        }

        if (o instanceof DefaultL2TunnelDescription) {
            DefaultL2TunnelDescription that = (DefaultL2TunnelDescription) o;
            // Equality is based on tunnel id and pw label
            // which is always the last label.
            return this.l2Tunnel.equals(that.l2Tunnel) &&
                    this.l2TunnelPolicy.equals(that.l2TunnelPolicy);
        }

        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("l2Tunnel", l2Tunnel())
                .add("l2TunnelPolicy", l2TunnelPolicy())
                .toString();
    }
}
