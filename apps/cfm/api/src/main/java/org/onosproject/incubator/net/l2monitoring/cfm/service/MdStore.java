/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.net.l2monitoring.cfm.service;

import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.store.Store;

import java.util.Collection;
import java.util.Optional;

/**
 * {@link MaintenanceDomain Maintenance Domain's} storage interface.
 * Note: because the MaintenanceDomain is immutable if anything needs to be
 * changed in it, then it must be replaced in the store. This includes adding
 * and deleting Maintenance Associations from an MD.
 */
public interface MdStore extends Store<MdEvent, MdStoreDelegate> {
    /**
     * Get a list of all of the Maintenance Domains on the system.
     * @return A collection Maintenance domains from the MD Store.
     */
    Collection<MaintenanceDomain> getAllMaintenanceDomain();

    /**
     * Get a specific Maintenance Domain by its identifier.
     * @param mdName An identifier for the Maintenance Domain
     * @return A Maintenance Domain from the MD Store. Empty if not found.
     */
    Optional<MaintenanceDomain> getMaintenanceDomain(MdId mdName);

    /**
     * Delete a specific Maintenance Domain by its identifier.
     * @param mdName An identifier for the Maintenance Domain to be deleted
     * @return True if the MD was found and deleted
     */
    boolean deleteMaintenanceDomain(MdId mdName);

    /**
     * Create or replace a Maintenance Domain.
     * @param md The Maintenance Domain to create or replace
     * @return true if an MD of this name already existed
     */
    boolean createUpdateMaintenanceDomain(MaintenanceDomain md);
}
