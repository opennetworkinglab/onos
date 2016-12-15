/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.drivers.microsemi.yang;

import java.util.List;
import java.util.Map;

import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.TargetConfig;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.MseaUniEvcService;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.MseaUniEvcServiceOpParam;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.UniSideInterfaceAssignmentEnum;

/**
 * Extension of mseaUniEvcServiceService to include NETCONF sessions.
 *
 * This is manually extended and should be revised if the msea-uni-evc-service.yang
 * file changes
 */
public interface MseaUniEvcServiceNetconfService {
    /**
     * Returns the configuration and state attributes of the running mseaUniEvcService.
     *
     * @param mseaUniEvcService value of mseaUniEvcService
     * @param session The NETCONF session
     * @return mseaUniEvcService
     * @throws NetconfException if the session has any error
     */
    MseaUniEvcService getMseaUniEvcService(
            MseaUniEvcServiceOpParam mseaUniEvcService, NetconfSession session)
            throws NetconfException;

    /**
     * Returns the configuration only attributes of mseaUniEvcService.
     *
     * @param mseaUniEvcService value of mseaUniEvcService
     * @param session The NETCONF session
     * @param targetDs one of running, candidate or startup
     * @return mseaUniEvcService
     * @throws NetconfException if the session has any error
     */
    MseaUniEvcService getConfigMseaUniEvcService(
            MseaUniEvcServiceOpParam mseaUniEvcService, NetconfSession session, TargetConfig targetDs)
            throws NetconfException;

    /**
     * Sets the value to attribute mseaUniEvcService.
     *
     * @param mseaUniEvcService value of mseaUniEvcService
     * @param session The NETCONF session
     * @param targetDs one of running, candidate or startup
     * @throws NetconfException if the session has any error
     */
    void setMseaUniEvcService(MseaUniEvcServiceOpParam mseaUniEvcService,
            NetconfSession session, TargetConfig targetDs)
            throws NetconfException;

    /**
     * Returns a list of the CeVlanMaps on both sides of the EVC.
     *
     * @param session A NETCONF Session
     * @param ncDs The datastore to affect - running, candidate or startup
     * @return The Object Model with the VLans
     * @throws NetconfException if the session has any error
     */
    MseaUniEvcService getmseaUniEvcCeVlanMaps(
            NetconfSession session, TargetConfig ncDs)
            throws NetconfException;

    /**
     * Replace ceVlans or delete EVCs from a device.
     *
     * It is necessary to have a custom command for this as the YCH cannot
     * handle the intricacies of putting a replace operation on the
     * ceVlanMap leaf at present
     *
     * @param ceVlanUpdates A Map of CeVlanMap entries to change, as flows are deleted
     * @param flowVlanIds The IDs of flows that are being removed
     * @param session A NETCONF Session
     * @param targetDs The datastore to affect - running, candidate or startup
     * @param portAssign The port assignment of the device
     * @throws NetconfException if the session has any error
     */
    void removeEvcUniFlowEntries(
            Map<Integer, String> ceVlanUpdates,
            Map<Integer, List<Short>> flowVlanIds,
            NetconfSession session,
            TargetConfig targetDs,
            UniSideInterfaceAssignmentEnum portAssign)
                    throws NetconfException;

}
