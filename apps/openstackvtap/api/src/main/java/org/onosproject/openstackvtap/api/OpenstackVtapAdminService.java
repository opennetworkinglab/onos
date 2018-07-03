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
package org.onosproject.openstackvtap.api;

import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

/**
 * Service for administering the inventory of vTap.
 */
public interface OpenstackVtapAdminService extends OpenstackVtapService {

    /**
     * Creates a new vTap based on the specified description.
     *
     * @param type                  vTap type
     * @param vTapCriterion         criterion of a vTap
     * @return created vTap object or null if error occurred
     */
    OpenstackVtap createVtap(OpenstackVtap.Type type, OpenstackVtapCriterion vTapCriterion);

    /**
     * Updates the existing vTap based on the given vTap instance.
     *
     * @param vTapId             vTap identifier
     * @param vTap               vTap instance to be modified
     * @return updated vTap object or null if error occurred
     */
    OpenstackVtap updateVtap(OpenstackVtapId vTapId, OpenstackVtap vTap);

    /**
     * Removes the specified vTap with given vTap identifier.
     *
     * @param vTapId             vTap identifier
     * @return removed vTap object or null if error occurred
     */
    OpenstackVtap removeVtap(OpenstackVtapId vTapId);

    /**
     * Sets output port and VLAN tag for vTap.
     *
     * @param deviceId           device identifier
     * @param type               vTap type
     * @param portNumber         port number
     * @param vlanId             VLAN tag
     */
    void setVtapOutput(DeviceId deviceId, OpenstackVtap.Type type,
                       PortNumber portNumber, VlanId vlanId);

    /**
     * Sets output port and VNI for vTap.
     *
     * @param deviceId          device identifier
     * @param type              vTap type
     * @param portNumber        port number
     * @param vni               virtual network index (VNI) of VxLAN
     */
    void setVtapOutput(DeviceId deviceId, OpenstackVtap.Type type,
                       PortNumber portNumber, int vni);
}
