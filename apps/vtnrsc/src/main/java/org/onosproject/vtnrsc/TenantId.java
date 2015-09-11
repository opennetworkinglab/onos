/*
 * Copyright 2015 Open Networking Laboratory
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

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable representation of a tenant identifier.
 */
public final class TenantId {

    private final String tenantId;

    // Public construction is prohibited
    private TenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Creates a network id using the tenantid.
     *
     * @param tenantid network String
     * @return TenantId
     */
    public static TenantId tenantId(String tenantid) {
        checkNotNull(tenantid, "Tenantid can not be null");
        return new TenantId(tenantid);
    }

    /**
     * Returns the tenant identifier.
     *
     * @return the tenant identifier
     */
    public String tenantId() {
        return tenantId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TenantId) {
            final TenantId that = (TenantId) obj;
            return this.getClass() == that.getClass()
                    && Objects.equals(this.tenantId, that.tenantId);
        }
        return false;
    }

    @Override
    public String toString() {
        return tenantId;
    }

}
