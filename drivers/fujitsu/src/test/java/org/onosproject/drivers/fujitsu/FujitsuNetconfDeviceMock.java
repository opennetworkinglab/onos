/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.drivers.fujitsu;

import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;


/**
* Mock DefaultNetconfDevice.
*/
class FujitsuNetconfDeviceMock implements NetconfDevice {

    private NetconfDeviceInfo netconfDeviceInfo;
    private boolean deviceState = false;
    private NetconfSession netconfSession;

    /**
     * Creates a new NETCONF device with the information provided.
     *
     * @param deviceInfo information about the device to be created.
     * @throws NetconfException if there are problems in creating or establishing
     * the underlying NETCONF connection and session.
     */
    public FujitsuNetconfDeviceMock(NetconfDeviceInfo deviceInfo) throws NetconfException {
        netconfDeviceInfo = deviceInfo;
        try {
            netconfSession = new FujitsuNetconfSessionMock();
            deviceState = true;
        } catch (Exception e) {
            throw new NetconfException("Cannot create Connection and Session");
        }
    }

    @Override
    public boolean isActive() {
        return deviceState;
    }

    @Override
    public NetconfSession getSession() {
        return netconfSession;
    }

    @Override
    public void disconnect() {
        deviceState = false;
        netconfSession = null;
    }

    @Override
    public NetconfDeviceInfo getDeviceInfo() {
        return netconfDeviceInfo;
    }

}
