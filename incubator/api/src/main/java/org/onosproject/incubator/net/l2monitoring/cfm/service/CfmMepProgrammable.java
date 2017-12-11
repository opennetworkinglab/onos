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
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.Optional;

/**
 * Behaviour that allows Layer 2 Monitoring as defined in IEEE 802.1Q be implemented by devices.
 *
 * Has all of the same methods as
 * {@link CfmMepServiceBase} so reuse that
 */
public interface CfmMepProgrammable extends HandlerBehaviour, CfmMepServiceBase {


    /**
     * Allows an MD and children MAs to be created on a device.
     * A convenience method that allows an MD to be created on a device
     * This is a preparation mechanism. If this was not called the MD would be created
     * at the time of provisioning a MEP anyway
     * @param mdId The ID of the MD to create - the details will be got from the CfmMdService
     * @return true when created. false if it already existed
     * @throws CfmConfigException If the MD already exists
     */
    boolean createMdOnDevice(MdId mdId) throws CfmConfigException;

    /**
     * Allows an MA to be created on an existing MD on a device.
     * A convenience method that allows an MA to be created
     * This is a preparation mechanism. If this was not called the MA would be created
     * at the time of provisioning a MEP anyway. Also MAs can be created when they
     * are contained in an MD using the createMdOnDevice method
     * @param mdId The identifier of the MD to create the MA in
     * @param maId The identifier of the MA to create - the details will be retrieved from the CfmMdService
     * @return true when created. false if it already existed
     * @throws CfmConfigException If the MD already exists
     */
    boolean createMaOnDevice(MdId mdId, MaIdShort maId) throws CfmConfigException;

    /**
     * Allows a MD and its MAs to be deleted from a device.
     * A convenience method that allows an MD that has been provisioned on a
     * device to be removed.
     * All Meps must be removed first unless they are Meps unknown to ONOS.
     * This is a cleanup mechanism. Deleting Meps in the normal way does not celan
     * up MDs and MAs
     * @param mdId The identifier of the MD
     * @param oldMd The MaintenanceDomain that is being deleted
     * @return true when deleted. false if it did not exist
     * @throws CfmConfigException If the MD has any MAs that have MEPs then this will fail
     */
    boolean deleteMdOnDevice(MdId mdId, Optional<MaintenanceDomain> oldMd) throws CfmConfigException;

    /**
     * Allows an MA to be deleted from an MD on a device.
     * A convenience method that allows an MA that has been provisioned on a
     * device to be removed.
     * All Meps must be removed first unless they are Meps unknown to ONOS.
     * This is a cleanup mechanism. Deleting Meps in the normal way does not celan
     * up MDs and MAs
     * @param mdId The identifier of the MD
     * @param maId The identifier of the MA
     * @param oldMd The MaintenanceDomain from which the MA is being deleted
     * @return true when deleted. false if it did not exist
     * @throws CfmConfigException If the MA has any MEPs then this will fail
     */
    boolean deleteMaOnDevice(MdId mdId, MaIdShort maId,
                             Optional<MaintenanceDomain> oldMd) throws CfmConfigException;

    /**
     * Creates a Remote Mep entry on an MA.
     * @param mdId The identifier of the MD
     * @param maId The identifier of the MA
     * @param remoteMep The remote Mep Id to remove from the MA
     * @return true when deleted. false if it did not exist
     * @throws CfmConfigException If the MA does not exist this will fail
     */
    boolean createMaRemoteMepOnDevice(MdId mdId, MaIdShort maId, MepId remoteMep) throws CfmConfigException;

    /**
     * Deletes a Remote Mep entry on an MA.
     * @param mdId The identifier of the MD
     * @param maId The identifier of the MA
     * @param remoteMep The remote Mep Id to remove from the MA
     * @return true when deleted. false if it did not exist
     * @throws CfmConfigException If the MA does not exist this will fail
     */
    boolean deleteMaRemoteMepOnDevice(MdId mdId, MaIdShort maId, MepId remoteMep) throws CfmConfigException;

}
