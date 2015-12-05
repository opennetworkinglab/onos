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
package org.onosproject.net.newresource.impl;

import com.google.common.annotations.Beta;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.newresource.ResourceAdminService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;

/**
 * A class registering resources when they are detected.
 */
@Component(immediate = true)
@Beta
public final class ResourceRegistrar {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceAdminService adminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private DeviceListener deviceListener;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(groupedThreads("onos/resource", "registrar"));

    @Activate
    public void activate() {
        deviceListener = new ResourceDeviceListener(adminService, deviceService, driverService, executor);
        deviceService.addListener(deviceListener);
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        executor.shutdownNow();
    }
}
