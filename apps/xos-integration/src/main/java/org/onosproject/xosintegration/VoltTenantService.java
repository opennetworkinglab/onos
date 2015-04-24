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
package org.onosproject.xosintegration;

import java.util.Set;

public interface VoltTenantService {

    /**
     * Queries all the tenants.
     *
     * @return Set of all of the tenants
     */
    Set<VoltTenant> getAllTenants();

    /**
     * Removes a tenant given its ID.
     *
     * @param id if od tenant to remove.
     */
    void removeTenant(long id);

    /**
     * Creates a new tenant and adds it to the XOS instance.
     *
     * @param newTenant tenant to add
     * @return the added tenant
     */
    VoltTenant addTenant(VoltTenant newTenant);

    /**
     * Gets a single tenant for the given ID.
     *
     * @param id ID of the tenant to fetch
     * @return tenant that was fetched
     */
    VoltTenant getTenant(long id);
}
