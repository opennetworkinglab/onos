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

import java.util.Collection;
import java.util.Optional;

import org.onosproject.event.ListenerService;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;

/**
 * For the management of Maintenance Domains and Maintenance Associations.
 */
public interface CfmMdService extends ListenerService<MdEvent, MdListener> {

    /**
     * Get a list of all of the Maintenance Domains on the system.
     * @return A collection Maintenance domains from the Distributed MD Store.
     */
    Collection<MaintenanceDomain> getAllMaintenanceDomain();

    /**
     * Get a specific Maintenance Domain by its identifier.
     * @param mdName An identifier for the Maintenance Domain
     * @return A Maintenance Domain from the Distributed MD Store. Empty if not found.
     */
    Optional<MaintenanceDomain> getMaintenanceDomain(MdId mdName);

    /**
     * Delete a specific Maintenance Domain by its identifier.
     * @param mdName An identifier for the Maintenance Domain to be deleted
     * @return True if the MD was found and deleted
     * @exception CfmConfigException If there were any Meps dependent on the MD or its MAs
     */
    boolean deleteMaintenanceDomain(MdId mdName) throws CfmConfigException;

    /**
     * Create or replace a Maintenance Domain.
     * @param md The Maintenance Domain to create or replace
     * @return true if an MD of this name already existed
     * @throws CfmConfigException If it is a replacement and there were any
     *                                      Meps dependent on the MD or its MAs
     */
    boolean createMaintenanceDomain(MaintenanceDomain md) throws CfmConfigException;

    /**
     * Get all of the Maintenance Associations for a Maintenance Domain.
     * @param mdName The identifier of the Maintenance Domain
     * @return A collection of Maintenance Associations
     */
    Collection<MaintenanceAssociation> getAllMaintenanceAssociation(MdId mdName);

    /**
     * Get a specific Maintenance Association of a specific Maintenance Domain.
     * @param mdName The identifier of the Maintenance Domain
     * @param maName The identifier of the Maintenance Association
     * @return A Maintenance Association from the Distributed MD Store. Empty if not found.
     * @throws IllegalArgumentException if MD is not found
     */
    Optional<MaintenanceAssociation> getMaintenanceAssociation(MdId mdName, MaIdShort maName);

    /**
     * Delete a specific Maintenance Association of a specific Maintenance Domain.
     * @param mdName The identifier of the Maintenance Domain
     * @param maName The identifier of the Maintenance Association
     * @return true if deleted
     * @throws CfmConfigException If there were any Meps dependent on the MD or its MAs
     * @throws IllegalArgumentException if MD is not found
     */
    boolean deleteMaintenanceAssociation(MdId mdName, MaIdShort maName) throws CfmConfigException;

    /**
     * Create or replace a Maintenance Domain of a specific Maintenance Domain.
     * @param mdName The identifier of the Maintenance Domain
     * @param ma A Maintenance Association
     * @return true if an MA of this name already existed in the MD
     * @throws CfmConfigException If it is a replacement and there were any
     *                                      Meps dependent on the MD or its MAs
     * @throws IllegalArgumentException if MD is not found
     */
    boolean createMaintenanceAssociation(MdId mdName, MaintenanceAssociation ma) throws CfmConfigException;
}
