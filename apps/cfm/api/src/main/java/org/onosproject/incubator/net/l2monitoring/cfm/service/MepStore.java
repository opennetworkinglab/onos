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
package org.onosproject.incubator.net.l2monitoring.cfm.service;

import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepKeyId;
import org.onosproject.net.DeviceId;
import org.onosproject.store.Store;

import java.util.Collection;
import java.util.Optional;

/**
 * {@link Mep Maintenance Association Endpoint's} storage interface.
 * Note: because the Mep is immutable if anything needs to be
 * changed in it, then it must be replaced in the store.
 */
public interface MepStore extends Store<CfmMepEvent, MepStoreDelegate> {
    /**
     * Get a list of all of the Meps on the system.
     * @return A collection Meps from the Mep Store.
     */
    Collection<Mep> getAllMeps();

    /**
     * Get all Meps by MD.
     * @param mdName An identifier for the Maintenance Domain
     * @return MEPs from the MEP Store. Empty if not found.
     */
    Collection<Mep> getMepsByMd(MdId mdName);

    /**
     * Get all Meps by MD, MA.
     * @param mdName An identifier for the Maintenance Domain
     * @param maName An identifier for the Maintenance Association
     * @return MEPs from the MEP Store. Empty if not found.
     */
    Collection<Mep> getMepsByMdMa(MdId mdName, MaIdShort maName);

    /**
     * Get all Meps by DeviceId.
     * @param deviceId An identifier for the Device
     * @return MEPs from the MEP Store. Empty if not found.
     */
    Collection<Mep> getMepsByDeviceId(DeviceId deviceId);

    /**
     * Get a specific Mep by its Mep key id.
     * @param mepKeyId An unique identifier for the MEP
     * @return A MEP from the MEP Store. Empty if not found.
     */
    Optional<Mep> getMep(MepKeyId mepKeyId);

    /**
     * Delete a specific Mep by its identifier.
     * @param mepKeyId An unique identifier for the MEP
     * @return True if the Mep was found and deleted
     */
    boolean deleteMep(MepKeyId mepKeyId);

    /**
     * Create or replace a Mep.
     * @param mepKeyId An unique identifier for the MEP
     * @param mep The new MEP
     * @return true if an Mep of this name already existed
     */
    boolean createUpdateMep(MepKeyId mepKeyId, Mep mep);

}
