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
package org.onosproject.vtnrsc;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * Representation of a Router gateway.
 */
public final class RouterGateway {

    private final TenantNetworkId networkId;
    private final boolean enableSnat;
    private final Set<FixedIp> externalFixedIps;

    // Public construction is prohibited
    private RouterGateway(TenantNetworkId networkId, boolean enableSnat,
                         Set<FixedIp> externalFixedIps) {
        this.networkId = checkNotNull(networkId, "networkId cannot be null");
        this.enableSnat = checkNotNull(enableSnat, "enableSnat cannot be null");
        this.externalFixedIps = checkNotNull(externalFixedIps, "externalFixedIps cannot be null");
    }

    /**
     * Creates router gateway object.
     *
     * @param networkId network identifier
     * @param enableSnat SNAT enable or not
     * @param externalFixedIps external fixed IP
     * @return RouterGateway
     */
    public static RouterGateway routerGateway(TenantNetworkId networkId, boolean enableSnat,
                                              Set<FixedIp> externalFixedIps) {
        return new RouterGateway(networkId, enableSnat, externalFixedIps);
    }

    /**
     * Returns network identifier.
     *
     * @return networkId
     */
    public TenantNetworkId networkId() {
        return networkId;
    }

    /**
     * Return SNAT enable or not.
     *
     * @return enableSnat
     */
    public boolean enableSnat() {
        return enableSnat;
    }

    /**
     * Return external fixed Ip.
     *
     * @return externalFixedIps
     */
    public Collection<FixedIp> externalFixedIps() {
        return externalFixedIps;
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId, enableSnat, externalFixedIps);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RouterGateway) {
            final RouterGateway that = (RouterGateway) obj;
            return Objects.equals(this.networkId, that.networkId)
                    && Objects.equals(this.enableSnat, that.enableSnat)
                    && Objects.equals(this.externalFixedIps, that.externalFixedIps);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("networkId", networkId)
                .add("enableSnat", enableSnat)
                .add("externalFixedIps", externalFixedIps)
                .toString();
    }
}
