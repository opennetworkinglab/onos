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

import java.util.Objects;

import org.onlab.packet.IpAddress;

/**
 * Default implementation of FloatingIp interface.
 */
public final class DefaultFloatingIp implements FloatingIp {

    private final FloatingIpId id;
    private final TenantId tenantId;
    private final TenantNetworkId networkId;
    private final VirtualPortId portId;
    private final RouterId routerId;
    private final IpAddress floatingIp;
    private final IpAddress fixedIp;
    private final Status status;

    /**
     *
     * Creates a floating Ip object.
     *
     * @param id  floatingIp identifier
     * @param tenantId  tenant identifier
     * @param networkId  the identifier of network associated with the floating Ip
     * @param portId  port identifier
     * @param routerId  router identifier
     * @param floatingIp  floatingIp address
     * @param fixedIp  the fixed Ip associated with the floating Ip
     * @param status  the floating Ip status
     */
    public DefaultFloatingIp(FloatingIpId id, TenantId tenantId,
                             TenantNetworkId networkId, VirtualPortId portId,
                             RouterId routerId, IpAddress floatingIp,
                             IpAddress fixedIp, Status status) {
        this.id = checkNotNull(id, "id cannot be null");
        this.tenantId = checkNotNull(tenantId, "tenantId cannot be null");
        this.networkId = checkNotNull(networkId, "networkId cannot be null");
        this.portId = portId;
        this.routerId = routerId;
        this.floatingIp = checkNotNull(floatingIp, "floatingIp cannot be null");
        this.fixedIp = fixedIp;
        this.status = checkNotNull(status, "status cannot be null");
    }

    @Override
    public FloatingIpId id() {
        return id;
    }

    @Override
    public TenantId tenantId() {
        return tenantId;
    }

    @Override
    public TenantNetworkId networkId() {
        return networkId;
    }

    @Override
    public VirtualPortId portId() {
        return portId;
    }

    @Override
    public RouterId routerId() {
        return routerId;
    }

    @Override
    public IpAddress floatingIp() {
        return floatingIp;
    }

    @Override
    public IpAddress fixedIp() {
        return fixedIp;
    }

    @Override
    public Status status() {
        return status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tenantId, networkId, portId, routerId,
                            floatingIp, fixedIp, status);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultFloatingIp) {
            final DefaultFloatingIp that = (DefaultFloatingIp) obj;
            return Objects.equals(this.id, that.id)
                    && Objects.equals(this.tenantId, that.tenantId)
                    && Objects.equals(this.networkId, that.networkId)
                    && Objects.equals(this.portId, that.portId)
                    && Objects.equals(this.routerId, that.routerId)
                    && Objects.equals(this.floatingIp, that.floatingIp)
                    && Objects.equals(this.fixedIp, that.fixedIp)
                    && Objects.equals(this.status, that.status);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id).add("tenantId", tenantId)
                .add("networkId", networkId).add("portId", portId)
                .add("routerId", routerId).add("floatingIp", floatingIp)
                .add("fixedIp", fixedIp).add("floatingIpStatus", status)
                .toString();
    }

}
