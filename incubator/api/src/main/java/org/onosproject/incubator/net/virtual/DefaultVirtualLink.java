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

package org.onosproject.incubator.net.virtual;

import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.provider.ProviderId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default representation of a virtual link.
 */
public final class DefaultVirtualLink extends DefaultLink implements VirtualLink {

    private static final String VIRTUAL = "virtual";
    private static final ProviderId PID = new ProviderId(VIRTUAL, VIRTUAL);

    private final NetworkId networkId;
    private final TunnelId tunnelId;

    /**
     * Constructor for a default virtual link.
     *
     * @param networkId network identifier
     * @param src       source connection point
     * @param dst       destination connection point
     * @param tunnelId  tunnel identifier
     */
    public DefaultVirtualLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst, TunnelId tunnelId) {
        super(PID, src, dst, Type.VIRTUAL, DefaultAnnotations.builder().build());
        this.networkId = networkId;
        this.tunnelId = tunnelId;
    }

    @Override
    public NetworkId networkId() {
        return networkId;
    }

    /**
     * Returns the tunnel identifier.
     *
     * @return tunnel identifier.
     */
    public TunnelId tunnelId() {
        return tunnelId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId, tunnelId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultVirtualLink) {
            DefaultVirtualLink that = (DefaultVirtualLink) obj;
            return super.equals(that) &&
                    Objects.equals(this.networkId, that.networkId) &&
                    Objects.equals(this.tunnelId, that.tunnelId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("networkId", networkId).add("tunnelId", tunnelId).toString();
    }
}
