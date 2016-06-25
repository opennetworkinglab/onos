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
package org.onosproject.vtnrsc;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

public final class TenantRouter {
    private final TenantId tenantId;
    private final RouterId routerId;

    /**
     * Construct a TenantRouter object.
     *
     * @param tenantId  the tenant identifier
     * @param routerId  router identifier
     */
    private TenantRouter(TenantId tenantId, RouterId routerId) {
        this.tenantId = checkNotNull(tenantId, "tenantId cannot be null");
        this.routerId = checkNotNull(routerId, "routerId cannot be null");
    }

    /**
     * Create a TenantRouter object.
     *
     * @param tenantId  the tenant identifier
     * @param routerId  router identifier
     * @return TenantRouter
     */
    public static TenantRouter tenantRouter(TenantId tenantId, RouterId routerId) {
        return new TenantRouter(tenantId, routerId);
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public RouterId routerId() {
        return routerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, routerId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TenantRouter) {
            final TenantRouter that = (TenantRouter) obj;
            return this.getClass() == that.getClass()
                    && Objects.equals(this.tenantId, that.tenantId)
                    && Objects.equals(this.routerId, that.routerId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("tenantId", tenantId)
                .add("routerId", routerId)
                .toString();
    }
}
