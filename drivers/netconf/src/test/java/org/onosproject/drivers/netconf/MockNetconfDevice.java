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
package org.onosproject.drivers.netconf;

import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfSession;

public class MockNetconfDevice implements NetconfDevice {

    private boolean active = false;
    private NetconfSession mockSession = null;
    private NetconfDeviceInfo mockNetconfDeviceInfo;
    private Class<? extends NetconfSession> sessionImplClass = MockNetconfSession.class;

    public MockNetconfDevice(NetconfDeviceInfo netconfDeviceInfo) {
        mockNetconfDeviceInfo = netconfDeviceInfo;
    }

    //Allows a different implementation of MockNetconfSession to be used.
    public void setNcSessionImpl(Class<? extends NetconfSession> sessionImplClass) {
        this.sessionImplClass = sessionImplClass;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public NetconfSession getSession() {
        if (mockSession != null) {
            return mockSession;
        }

        try {
            mockSession =
                   sessionImplClass.getDeclaredConstructor(NetconfDeviceInfo.class).newInstance(mockNetconfDeviceInfo);
            active = true;
            return mockSession;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void disconnect() {
        // TODO Auto-generated method stub
        mockSession = null;
        active = false;
    }

    @Override
    public NetconfDeviceInfo getDeviceInfo() {
        // TODO Auto-generated method stub
        return mockNetconfDeviceInfo;
    }

}
