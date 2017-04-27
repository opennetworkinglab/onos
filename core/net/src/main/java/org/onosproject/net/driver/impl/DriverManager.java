/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.driver.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.net.AbstractProjectableModel;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverRegistry;
import org.onosproject.net.driver.DriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.nullIsNotFound;
import static org.onosproject.net.AnnotationKeys.DRIVER;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.DRIVER_READ;
import static org.onosproject.security.AppPermission.Type.DRIVER_WRITE;

/**
 * Manages inventory of device drivers.
 */
@Service
// Not enabled by default to allow the DriverRegistryManager to enable it only
// when all the required drivers are available.
@Component(immediate = true, enabled = false)
public class DriverManager implements DriverService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String NO_DRIVER = "Driver not found";
    private static final String NO_DEVICE = "Device not found";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Activate
    protected void activate() {
        AbstractProjectableModel.setDriverService(null, this);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        AbstractProjectableModel.setDriverService(this, null);
        log.info("Stopped");
    }

    @Override
    public Set<Driver> getDrivers() {
        checkPermission(DRIVER_READ);
        return registry.getDrivers();
    }

    @Override
    public Set<Driver> getDrivers(Class<? extends Behaviour> withBehaviour) {
        checkPermission(DRIVER_READ);
        return registry.getDrivers().stream()
                .filter(d -> d.hasBehaviour(withBehaviour))
                .collect(Collectors.toSet());
    }

    @Override
    public Driver getDriver(String driverName) {
        checkPermission(DRIVER_READ);
        return registry.getDriver(driverName);
    }

    @Override
    public Driver getDriver(String mfr, String hw, String sw) {
        checkPermission(DRIVER_READ);
        return registry.getDriver(mfr, hw, sw);
    }

    @Override
    public Driver getDriver(DeviceId deviceId) {
        checkPermission(DRIVER_READ);

        Device device = nullIsNotFound(deviceService.getDevice(deviceId), NO_DEVICE);
        String driverName = device.annotations().value(DRIVER);
        if (driverName != null) {
            try {
                return getDriver(driverName);
            } catch (ItemNotFoundException e) {
                log.warn("Specified driver {} not found, falling back.", driverName);
            }
        }

        return nullIsNotFound(getDriver(device.manufacturer(),
                                        device.hwVersion(), device.swVersion()),
                              NO_DRIVER);
    }

    @Override
    public DriverHandler createHandler(DeviceId deviceId, String... credentials) {
        checkPermission(DRIVER_WRITE);
        Driver driver = getDriver(deviceId);
        return new DefaultDriverHandler(new DefaultDriverData(driver, deviceId));
    }

}
