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
package org.onosproject.drivers.microsemi;

import java.util.HashMap;
import java.util.Map;

import org.onosproject.core.CoreService;
import org.onosproject.drivers.microsemi.yang.MockMseaSaFilteringManager;
import org.onosproject.drivers.microsemi.yang.MockMseaUniEvcServiceManager;
import org.onosproject.drivers.microsemi.yang.MockNetconfSessionEa1000;
import org.onosproject.drivers.microsemi.yang.MseaSaFilteringNetconfService;
import org.onosproject.drivers.microsemi.yang.MseaUniEvcServiceNetconfService;
import org.onosproject.drivers.netconf.MockCoreService;
import org.onosproject.drivers.netconf.MockNetconfController;
import org.onosproject.drivers.netconf.MockNetconfDevice;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;

/**
 * A Mock implementation of the DriverHandler to facilitate unit tests.
 *
 * This brings in the implementations of MockMseaSaFilteringManager, MockMseaUniEvcServiceManager,
 * MockCoreService, MockNetconfDevice and MockNetconfSessionEa1000
 */
public class MockEa1000DriverHandler implements DriverHandler {

    private static final String MICROSEMI_DRIVERS = "com.microsemi.drivers";

    private DriverData mockDriverData;

    private NetconfController ncc;
    private MockMseaSaFilteringManager mseaSaFilteringService;
    private MockMseaUniEvcServiceManager mseaUniEvcService;
    private CoreService coreService;

    public MockEa1000DriverHandler() throws NetconfException {
        Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours =
                new HashMap<Class<? extends Behaviour>, Class<? extends Behaviour>>();
        behaviours.put(FlowRuleProgrammable.class, FlowRuleProgrammable.class);

        Map<String, String> properties = new HashMap<String, String>();

        Driver mockDriver =
                new DefaultDriver("mockDriver", null, "ONOSProject", "1.0.0", "1.0.0", behaviours, properties);
        DeviceId mockDeviceId = DeviceId.deviceId("netconf:1.2.3.4:830");
        mockDriverData = new DefaultDriverData(mockDriver, mockDeviceId);


        ncc = new MockNetconfController();
        MockNetconfDevice device = (MockNetconfDevice) ncc.connectDevice(mockDeviceId);
        device.setNcSessionImpl(MockNetconfSessionEa1000.class);

        mseaSaFilteringService = new MockMseaSaFilteringManager();
        mseaSaFilteringService.activate();

        mseaUniEvcService = new MockMseaUniEvcServiceManager();
        mseaUniEvcService.activate();

        coreService = new MockCoreService();
        coreService.registerApplication(MICROSEMI_DRIVERS);
    }

    @Override
    public Driver driver() {
        return mockDriverData.driver();
    }

    @Override
    public DriverData data() {
        return mockDriverData;
    }

    @Override
    public <T extends Behaviour> T behaviour(Class<T> behaviourClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T get(Class<T> serviceClass) {
        if (serviceClass.equals(NetconfController.class)) {
            return (T) ncc;

        } else if (serviceClass.equals(MseaSaFilteringNetconfService.class)) {
            return (T) mseaSaFilteringService;

        } else if (serviceClass.equals(MseaUniEvcServiceNetconfService.class)) {
            return (T) mseaUniEvcService;

        } else if (serviceClass.equals(CoreService.class)) {
            return (T) coreService;

        }

        return null;
    }

}
