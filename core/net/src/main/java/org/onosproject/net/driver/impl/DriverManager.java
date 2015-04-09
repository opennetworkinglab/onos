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
package org.onosproject.net.driver.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverAdminService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.onlab.util.Tools.nullIsNotFound;
import static org.onosproject.net.AnnotationKeys.DRIVER;

/**
 * Manages inventory of device drivers.
 */
@Component(immediate = true)
@Service
public class DriverManager implements DriverAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String NO_DRIVER = "Driver not found";
    private static final String NO_DEVICE = "Device not found";
    private static final String DEFAULT = "default";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private Set<DriverProvider> providers = Sets.newConcurrentHashSet();
    private Map<String, Driver> driverByName = Maps.newConcurrentMap();
    private Map<String, Driver> driverByKey = Maps.newConcurrentMap();

    @Activate
    protected void activate() {
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }


    @Override
    public Set<DriverProvider> getProviders() {
        return ImmutableSet.copyOf(providers);
    }

    @Override
    public void registerProvider(DriverProvider provider) {
        provider.getDrivers().forEach(driver -> {
            driverByName.put(driver.name(), driver);
            driverByKey.put(key(driver.manufacturer(),
                                driver.hwVersion(),
                                driver.swVersion()), driver);
        });
        providers.add(provider);
    }

    @Override
    public void unregisterProvider(DriverProvider provider) {
        provider.getDrivers().forEach(driver -> {
            driverByName.remove(driver.name());
            driverByKey.remove(key(driver.manufacturer(),
                                   driver.hwVersion(),
                                   driver.swVersion()));
        });
        providers.remove(provider);
    }

    @Override
    public Set<Driver> getDrivers(Class<? extends Behaviour>... withBehaviours) {
        ImmutableSet.Builder<Driver> builder = ImmutableSet.builder();
        for (Class<? extends Behaviour> behaviour : withBehaviours) {
            driverByName.forEach((name, driver) -> {
                if (driver.hasBehaviour(behaviour)) {
                    builder.add(driver);
                }
            });
        }
        return builder.build();
    }

    @Override
    public Driver getDriver(String driverName) {
        return nullIsNotFound(driverByName.get(driverName), NO_DRIVER);
    }

    @Override
    public Driver getDriver(String mfr, String hw, String sw) {
        // First attempt a literal search.
        Driver driver = driverByKey.get(key(mfr, hw, sw));
        if (driver != null) {
            return driver;
        }

        // Otherwise, sweep through the key space and attempt to match using
        // regular expression matching.
        Optional<Driver> optional = driverByKey.values().stream()
                .filter(d -> matches(d, mfr, hw, sw)).findFirst();

        // If no matching driver is found, return default.
        return optional.isPresent() ? optional.get() : driverByName.get(DEFAULT);
    }

    // Matches the given driver using ERE matching against the given criteria.
    private boolean matches(Driver d, String mfr, String hw, String sw) {
        // TODO: consider pre-compiling the expressions in the future
        return mfr.matches(d.manufacturer()) &&
                hw.matches(d.hwVersion()) &&
                sw.matches(d.swVersion());
    }

    @Override
    public Driver getDriver(DeviceId deviceId) {
        Device device = nullIsNotFound(deviceService.getDevice(deviceId), NO_DEVICE);
        String driverName = device.annotations().value(DRIVER);
        if (driverName != null) {
            return getDriver(driverName);
        }
        return nullIsNotFound(getDriver(device.manufacturer(),
                                        device.hwVersion(), device.swVersion()),
                              NO_DRIVER);
    }

    @Override
    public DriverHandler createHandler(DeviceId deviceId, String... credentials) {
        Driver driver = getDriver(deviceId);
        return new DefaultDriverHandler(new DefaultDriverData(driver));
    }

    private String key(String mfr, String hw, String sw) {
        return String.format("%s-%s-%s", mfr, hw, sw);
    }

}
