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
package org.onosproject.incubator.net.virtual;

import com.google.common.annotations.Beta;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Representation of network tenant.
 */
@Beta
public final class TenantId {

    /**
     * Represents no tenant, or an unspecified tenant.
     */
    public static final TenantId NONE = new TenantId();


    private final String id;

    // Public construction is prohibited
    private TenantId(String id) {
        checkArgument(id != null && id.length() > 0, "Tenant ID cannot be null or empty");
        this.id = id;
    }


    // Default constructor for serialization
    protected TenantId() {
        this.id = "";
    }

    /**
     * Creates a tenant id using the supplied backing id.
     *
     * @param id network id
     * @return network identifier
     */
    public static TenantId tenantId(String id) {
        return new TenantId(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TenantId) {
            final TenantId that = (TenantId) obj;
            return this.getClass() == that.getClass() &&
                    Objects.equals(this.id, that.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return id;
    }

}
