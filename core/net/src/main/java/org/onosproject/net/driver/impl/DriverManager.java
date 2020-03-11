/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onlab.util.ItemNotFoundException;
import org.onosproject.net.AbstractProjectableModel;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverListener;
import org.onosproject.net.driver.DriverRegistry;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
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

// Not enabled by default to allow the DriverRegistryManager to enable it only
// when all the required drivers are available.
@Component(immediate = true, enabled = false, service = DriverService.class)
public class DriverManager implements DriverService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String NO_DRIVER = "Driver not found";
    private static final String NO_DEVICE = "Device not found";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PiPipeconfService pipeconfService;

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

        Driver driver;

        // Special processing for devices with pipeconf.
        if (pipeconfService.ofDevice(deviceId).isPresent()) {
            // No fallback for pipeconf merged drivers.
            // Throws exception if pipeconf driver does not exist.
            return nullIsNotFound(
                    getPipeconfMergedDriver(deviceId),
                    "Device is pipeconf-capable but a " +
                            "pipeconf-merged driver was not found");
        }

        // Primary source of driver configuration is the network config.
        BasicDeviceConfig cfg = networkConfigService.getConfig(deviceId, BasicDeviceConfig.class);
        driver = lookupDriver(cfg != null ? cfg.driver() : null);
        if (driver != null) {
            return driver;
        }

        // Secondary source of the driver selection is driver annotation.
        Device device = nullIsNotFound(deviceService.getDevice(deviceId), NO_DEVICE);
        driver = lookupDriver(device.annotations().value(DRIVER));
        if (driver != null) {
            return driver;
        }

        // Tertiary source of the driver selection is the primordial information
        // obtained from the device.
        return nullIsNotFound(getDriver(device.manufacturer(),
                                        device.hwVersion(), device.swVersion()),
                              NO_DRIVER);
    }

    @Override
    public Map<DeviceId, String> getDeviceDrivers() {
        Map<DeviceId, String> deviceDriverNameMap = new HashMap<>();
        deviceService.getDevices().forEach(device -> {
            deviceDriverNameMap.put(device.id(), getDriver(device.id()).name());
        });
        return deviceDriverNameMap;
    }

    private Driver getPipeconfMergedDriver(DeviceId deviceId) {
        PiPipeconfId pipeconfId = pipeconfService.ofDevice(deviceId).orElse(null);
        if (pipeconfId == null) {
            log.warn("Missing pipeconf for {}, cannot produce a pipeconf merged driver",
                      deviceId);
            return null;
        }
        String mergedDriverName = pipeconfService.getMergedDriver(deviceId, pipeconfId);
        if (mergedDriverName == null) {
            log.warn("Unable to get pipeconf merged driver for {} and {}",
                     deviceId, pipeconfId);
            return null;
        }
        try {
            return getDriver(mergedDriverName);
        } catch (ItemNotFoundException e) {
            log.warn("Specified pipeconf merged driver {} for {} not found",
                     mergedDriverName, deviceId);
            return null;
        }
    }

    private Driver lookupDriver(String driverName) {
        if (driverName != null) {
            try {
                return getDriver(driverName);
            } catch (ItemNotFoundException e) {
                log.warn("Specified driver {} not found, falling back.", driverName);
            }
        }
        return null;
    }

    @Override
    public DriverHandler createHandler(DeviceId deviceId, String... credentials) {
        checkPermission(DRIVER_WRITE);
        Driver driver = getDriver(deviceId);
        return new DefaultDriverHandler(new DefaultDriverData(driver, deviceId));
    }

    @Override
    public void addListener(DriverListener listener) {
        registry.addListener(listener);
    }

    @Override
    public void removeListener(DriverListener listener) {
        registry.removeListener(listener);
    }
}
