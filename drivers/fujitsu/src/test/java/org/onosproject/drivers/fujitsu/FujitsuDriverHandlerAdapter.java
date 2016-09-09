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

import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.DriverData;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.netconf.NetconfController;


/**
 * Mock DefaultDriverHandler.
 */
public class FujitsuDriverHandlerAdapter extends DefaultDriverHandler {

    private NetconfController controller;
    private final MastershipService mastershipService = new InternalMastershipServiceMock();;

    /**
     * Creates new driver handler with the attached driver data.
     *
     * @param data driver data to attach
     */
    public FujitsuDriverHandlerAdapter(DriverData data) {
        super(data);
    }

    @Override
    public boolean hasBehaviour(Class<? extends Behaviour> behaviourClass) {
        return true;
    }

    @Override
    public <T> T get(Class<T> serviceClass) {
        if (serviceClass == NetconfController.class) {
            return (T) this.controller;
        } else if (serviceClass == MastershipService.class) {
            return (T) this.mastershipService;
        }
        return null;
    }

    /**
     * Set up initial environment.
     *
     * @param controller NETCONF controller instance
     */
    public void setUp(NetconfController controller) {
        this.controller = controller;
    }

    /**
     * Mock MastershipServiceAdapter.
     */
    private class InternalMastershipServiceMock extends MastershipServiceAdapter {

        @Override
        public boolean isLocalMaster(DeviceId deviceId) {
            return true;
        }
    }

}
