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

import java.util.List;
import java.util.Objects;

/**
 * Default implementation of router interface.
 */
public final class DefaultRouter implements Router {
    private final RouterId id;
    private final String name;
    private final boolean adminStateUp;
    private final Status status;
    private final boolean distributed;
    private final RouterGateway externalGatewayInfo;
    private final VirtualPortId gatewayPortId;
    private final TenantId tenantId;
    private final List<String> routes;

    /**
     * Creates router object.
     *
     * @param id  router identifier
     * @param routerName  the name of router
     * @param adminStateUp  the status of admin state
     * @param status  the status of router
     * @param distributed  the status of router distributed
     * @param externalGatewayInfo  the gateway info of router
     * @param gatewayPortId  the port identifier of router gateway
     * @param tenantId  the tenant identifier
     * @param routes  the routes configure
     */
    public DefaultRouter(RouterId id, String routerName, boolean adminStateUp,
                         Status status, boolean distributed,
                         RouterGateway externalGatewayInfo,
                         VirtualPortId gatewayPortId, TenantId tenantId,
                         List<String> routes) {
        this.id = checkNotNull(id, "id cannot be null");
        this.name = routerName;
        this.adminStateUp = checkNotNull(adminStateUp, "adminStateUp cannot be null");
        this.status = checkNotNull(status, "status cannot be null");
        this.distributed = checkNotNull(distributed, "distributed cannot be null");
        this.externalGatewayInfo = externalGatewayInfo;
        this.gatewayPortId = gatewayPortId;
        this.tenantId = checkNotNull(tenantId, "tenantId cannot be null");
        this.routes = routes;
    }

    @Override
    public RouterId id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean adminStateUp() {
        return adminStateUp;
    }

    @Override
    public Status status() {
        return status;
    }

    @Override
    public boolean distributed() {
        return distributed;
    }

    @Override
    public RouterGateway externalGatewayInfo() {
        return externalGatewayInfo;
    }

    @Override
    public VirtualPortId gatewayPortid() {
        return gatewayPortId;
    }

    @Override
    public TenantId tenantId() {
        return tenantId;
    }

    @Override
    public List<String> routes() {
        return routes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, adminStateUp, status, distributed,
                            externalGatewayInfo, gatewayPortId, routes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultRouter) {
            final DefaultRouter that = (DefaultRouter) obj;
            return Objects.equals(this.id, that.id)
                    && Objects.equals(this.name, that.name)
                    && Objects.equals(this.adminStateUp, that.adminStateUp)
                    && Objects.equals(this.status, that.status)
                    && Objects.equals(this.distributed, that.distributed)
                    && Objects.equals(this.externalGatewayInfo,
                                      that.externalGatewayInfo)
                    && Objects.equals(this.gatewayPortId, that.gatewayPortId)
                    && Objects.equals(this.routes, that.routes);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id).add("routerName", name)
                .add("adminStateUp", adminStateUp).add("status", status)
                .add("distributed", distributed)
                .add("externalGatewayInfo", externalGatewayInfo)
                .add("gatewayPortid", gatewayPortId).add("routes", routes).toString();
    }
}
