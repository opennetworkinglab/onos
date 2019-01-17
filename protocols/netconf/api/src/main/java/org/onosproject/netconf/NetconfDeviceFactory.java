/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.netconf;

import com.google.common.annotations.Beta;

/**
 * Abstract interface for the creation of a NETCONF device.
 */
@FunctionalInterface
public interface NetconfDeviceFactory {

    /**
     * Creates a new NETCONF device based on the supplied information.
     * @param netconfDeviceInfo information of the device to create.
     * @return Instance of NetconfDevice.
     * @throws NetconfException when problems arise creating the device and establishing
     * the connection.
     */
    NetconfDevice createNetconfDevice(NetconfDeviceInfo netconfDeviceInfo)
            throws NetconfException;

    /**
     * Creates a new NETCONF device based on the supplied information.
     * @param netconfDeviceInfo information of the device to create.
     * @param isMaster if true create secure transport session with the device,
     *                 else just create a proxy session
     * @return Instance of NetconfDevice.
     * @throws NetconfException when problems arise creating the device and establishing
     * the connection.
     */
    @Beta
    default NetconfDevice createNetconfDevice(NetconfDeviceInfo netconfDeviceInfo,
                                              boolean isMaster) throws NetconfException {
        return createNetconfDevice(netconfDeviceInfo);
    }
}
