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

package org.onosproject.netconf.ctl;

import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfSession;

import java.io.IOException;

/**
 * Implementation of a NETCONF device.
 */
public class NetconfDeviceImpl implements NetconfDevice {

    private NetconfDeviceInfo netconfDeviceInfo;
    private boolean deviceState = false;
    private NetconfSession netconfSession;
    //private String config;

    public NetconfDeviceImpl(NetconfDeviceInfo deviceInfo) throws IOException {
        netconfDeviceInfo = deviceInfo;
        try {
            netconfSession = new NetconfSessionImpl(netconfDeviceInfo);
        } catch (IOException e) {
            throw new IOException("Cannot create connection and session", e);
        }
        deviceState = true;
        //config = netconfSession.getConfig("running");
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
        netconfSession.close();
    }

    @Override
    public NetconfDeviceInfo getDeviceInfo() {
        return netconfDeviceInfo;
    }
}
