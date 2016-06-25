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
package org.onosproject.incubator.net.virtual;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default implementation of the virtual network descriptor.
 */
public final class DefaultVirtualNetwork implements VirtualNetwork {

    private final NetworkId id;
    private final TenantId tenantId;

    /**
     * Creates a new virtual network descriptor.
     *
     * @param id       network identifier
     * @param tenantId tenant identifier
     */
    public DefaultVirtualNetwork(NetworkId id, TenantId tenantId) {
        this.id = id;
        this.tenantId = tenantId;
    }

    @Override
    public NetworkId id() {
        return id;
    }

    @Override
    public TenantId tenantId() {
        return tenantId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tenantId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultVirtualNetwork) {
            DefaultVirtualNetwork that = (DefaultVirtualNetwork) obj;
            return Objects.equals(this.id, that.id)
                    && Objects.equals(this.tenantId, that.tenantId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("tenantId", tenantId)
                .toString();
    }
}
