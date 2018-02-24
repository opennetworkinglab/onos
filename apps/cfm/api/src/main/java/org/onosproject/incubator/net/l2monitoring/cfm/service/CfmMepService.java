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

import org.onosproject.event.ListenerService;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.net.DeviceId;

/**
 * For the management of Maintenance Association Endpoints.
 *
 * These are dependent on the Maintenance Domain service which maintains the
 * Maintenance Domain and Maintenance Associations
 */
public interface CfmMepService
        extends ListenerService<CfmMepEvent, CfmMepListener>, CfmMepServiceBase {
    /**
     * Retrieve all {@link MepEntry}(s) belonging to an MA.
     * @param mdName A Maintenance Domain
     * @param maName A Maintetance Association in the MD
     * @return A collection of MEP Entries
     * @throws CfmConfigException If there a problem with the MD or MA
     */
    Collection<MepEntry> getAllMeps(MdId mdName, MaIdShort maName)
            throws CfmConfigException;

    /**
     * Retrieve all {@link Mep}(s) belonging to an MA.
     * Note: This just returns the configuration part of the Mep, not the MepEntry
     * which contains config and state
     * @param deviceId A device id
     * @return A collection of MEP Entries
     * @throws CfmConfigException If there a problem with the MD or MA
     */
    Collection<Mep> getAllMepsByDevice(DeviceId deviceId)
            throws CfmConfigException;
}
