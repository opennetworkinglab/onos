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

package org.onosproject.ofagent.impl;

import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.provider.of.flow.util.FlowEntryBuilder;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FlowEntryBuilder customized for OFAgent.  This builder will be used to build FlowEntry objects for
// virtual devices encountered by OFAgent.  The driver has been hardcoded to "ovs".
public class OFAgentVirtualFlowEntryBuilder extends FlowEntryBuilder {
    private static final Logger log = LoggerFactory.getLogger(OFAgentVirtualFlowEntryBuilder.class);
    private static final String DRIVER_NAME = "ovs";

    public OFAgentVirtualFlowEntryBuilder(DeviceId deviceId, OFFlowMod fm, DriverService driverService) {
        super(deviceId, fm, getDriver(deviceId, driverService));
    }

    protected static DriverHandler getDriver(DeviceId devId, DriverService driverService) {
        log.debug("calling getDriver for {}", devId);
        Driver driver = driverService.getDriver(DRIVER_NAME);
        DriverHandler handler = new DefaultDriverHandler(new DefaultDriverData(driver, devId));
        return handler;
    }
}
