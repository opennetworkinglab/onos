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

package org.onosproject.incubator.net.virtual;

import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.provider.ProviderId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default representation of a virtual link.
 */
public final class DefaultVirtualLink extends DefaultLink implements VirtualLink {

    private static final String VIRTUAL = "virtualLink";
    public static final ProviderId PID = new ProviderId(VIRTUAL, VIRTUAL);

    private final NetworkId networkId;
    private final TunnelId tunnelId;

    /**
     * Private constructor for a default virtual link.
     *
     * @param networkId network identifier
     * @param src       source connection point
     * @param dst       destination connection point
     * @param state     link state
     * @param tunnelId  tunnel identifier
     */
    private DefaultVirtualLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst,
                               State state, TunnelId tunnelId) {
        super(PID, src, dst, Type.VIRTUAL, state, DefaultAnnotations.builder().build());
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

    /**
     * Creates a new default virtual link builder.
     *
     * @return default virtual link builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for DefaultVirtualLink objects.
     */
    public static final class Builder extends DefaultLink.Builder {
        private NetworkId networkId;
        private ConnectPoint src;
        private ConnectPoint dst;
        private TunnelId tunnelId;
        private State state;

        private Builder() {
            // Hide constructor
        }

        /**
         * Sets the network identifier to be used by the builder.
         *
         * @param networkId network identifier
         * @return self
         */
        public Builder networkId(NetworkId networkId) {
            this.networkId = networkId;
            return this;
        }

        /**
         * Sets the source connect point to be used by the builder.
         *
         * @param src source connect point
         * @return self
         */
        public Builder src(ConnectPoint src) {
            this.src = src;
            return this;
        }

        /**
         * Sets the destination connect point to be used by the builder.
         *
         * @param dst new destination connect point
         * @return self
         */
        public Builder dst(ConnectPoint dst) {
            this.dst = dst;
            return this;
        }

        /**
         * Sets the tunnel identifier to be used by the builder.
         *
         * @param tunnelId tunnel identifier
         * @return self
         */
        public Builder tunnelId(TunnelId tunnelId) {
            this.tunnelId = tunnelId;
            return this;
        }

        /**
         * Sets the link state to be used by the builder.
         *
         * @param state link state
         * @return self
         */
        public Builder state(State state) {
            this.state = state;
            return this;
        }

        /**
         * Builds a default virtual link object from the accumulated parameters.
         *
         * @return default virtual link object
         */
        public DefaultVirtualLink build() {
            checkNotNull(src, "Source connect point cannot be null");
            checkNotNull(dst, "Destination connect point cannot be null");
            checkNotNull(networkId, "Network Id cannot be null");

            return new DefaultVirtualLink(networkId, src, dst, state, tunnelId);
        }
    }
}
