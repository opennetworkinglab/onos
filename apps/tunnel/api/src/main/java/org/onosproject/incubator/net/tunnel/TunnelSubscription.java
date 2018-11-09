/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.incubator.net.tunnel;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.google.common.annotations.Beta;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.AbstractAnnotated;
import org.onosproject.net.Annotations;
import org.onosproject.incubator.net.tunnel.Tunnel.Type;

import com.google.common.base.MoreObjects;

/**
 * Represents for a order that consumer subscribe tunnel. ONOS maintains request
 * information, it means ONOS knows how much resource echo consumer uses in the
 * ONOS. Although there is no a tunnel that consumer want to use, when producer
 * creates a new tunnel, ONOS will notify the consumers that want to use it.
 */
@Beta
public final class TunnelSubscription extends AbstractAnnotated {
    private final ApplicationId consumerId;
    private final TunnelEndPoint src;
    private final TunnelEndPoint dst;
    private final Type type;
    private final TunnelId tunnelId;
    private final TunnelName tunnelName;

    /**
     * Creates a TunnelSubscription.
     *
     * @param consumerId consumer identity
     * @param src source tunnel end point of tunnel
     * @param dst destination tunnel end point of tunnel
     * @param tunnelId tunnel identity
     * @param type tunnel type
     * @param tunnelName the name of a tunnel
     * @param annotations parameter
     */
    public TunnelSubscription(ApplicationId consumerId, TunnelEndPoint src,
                 TunnelEndPoint dst, TunnelId tunnelId, Type type,
                 TunnelName tunnelName, Annotations... annotations) {
        super(annotations);
        checkNotNull(consumerId, "consumerId cannot be null");
        this.consumerId = consumerId;
        this.src = src;
        this.dst = dst;
        this.type = type;
        this.tunnelId = tunnelId;
        this.tunnelName = tunnelName;
    }

    /**
     * Returns consumer identity.
     *
     * @return consumerId consumer id
     */
    public ApplicationId consumerId() {
        return consumerId;
    }

    /**
     * Returns source point of tunnel.
     *
     * @return source point
     */
    public TunnelEndPoint src() {
        return src;
    }

    /**
     * Returns destination point of tunnel.
     *
     * @return destination point
     */
    public TunnelEndPoint dst() {
        return dst;
    }

    /**
     * Returns tunnel type.
     *
     * @return tunnel type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns tunnel identity.
     *
     * @return tunnel id
     */
    public TunnelId tunnelId() {
        return tunnelId;
    }

    /**
     * Returns tunnel name.
     *
     * @return tunnel name
     */
    public TunnelName tunnelName() {
        return tunnelName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumerId, src, dst, type, tunnelId, tunnelName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TunnelSubscription) {
            final TunnelSubscription other = (TunnelSubscription) obj;
            return Objects.equals(this.src, other.src)
                    && Objects.equals(this.dst, other.dst)
                    && Objects.equals(this.consumerId, other.consumerId)
                    && Objects.equals(this.type, other.type)
                    && Objects.equals(this.tunnelId, other.tunnelId)
                    && Objects.equals(this.tunnelName, other.tunnelName);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("src", src)
                .add("dst", dst)
                .add("consumerId", consumerId)
                .add("type", type)
                .add("tunnelId", tunnelId)
                .add("tunnelName", tunnelName).toString();
    }
}
