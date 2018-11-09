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
package org.onosproject.net;

import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Representation of network tenant.
 */
public final class TenantId extends Identifier<String> {

    /**
     * Represents no tenant, or an unspecified tenant.
     */
    public static final TenantId NONE = new TenantId();

    // Public construction is prohibited
    private TenantId(String id) {
        super(id);
        checkArgument(id != null && id.length() > 0, "Tenant ID cannot be null or empty");
    }

    // Default constructor for serialization
    protected TenantId() {
        super("");
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
}
