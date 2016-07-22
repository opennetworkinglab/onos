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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import org.onosproject.net.driver.DefaultDriverProvider;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverAdminService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.nullIsNotFound;
import static org.onosproject.net.AnnotationKeys.DRIVER;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.*;



/**
 * Manages inventory of device drivers.
 */
@Component(immediate = true)
@Service
public class DriverManager extends DefaultDriverProvider implements DriverAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String NO_DRIVER = "Driver not found";
    private static final String NO_DEVICE = "Device not found";
    private static final String DEFAULT = "default";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private Set<DriverProvider> providers = Sets.newConcurrentHashSet();
    private Map<String, Driver> driverByKey = Maps.newConcurrentMap();
    private Map<String, Class<? extends Behaviour>> classes = Maps.newConcurrentMap();

    @Activate
    protected void activate() {
        AbstractProjectableModel.setDriverService(null, this);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        AbstractProjectableModel.setDriverService(this, null);
        providers.clear();
        driverByKey.clear();
        classes.clear();
        log.info("Stopped");
    }

    @Override
    public Set<DriverProvider> getProviders() {
        return ImmutableSet.copyOf(providers);
    }

    @Override
    public void registerProvider(DriverProvider provider) {
        provider.getDrivers().forEach(driver -> {
            Driver d = addDriver(driver);
            driverByKey.put(key(driver.manufacturer(),
                                driver.hwVersion(),
                                driver.swVersion()), d);
            d.behaviours().forEach(b -> {
                Class<? extends Behaviour> implementation = d.implementation(b);
                classes.put(b.getName(), b);
                classes.put(implementation.getName(), implementation);
            });
        });
        providers.add(provider);
    }

    @Override
    public void unregisterProvider(DriverProvider provider) {
        provider.getDrivers().forEach(driver -> {
            removeDriver(driver);
            driverByKey.remove(key(driver.manufacturer(),
                                   driver.hwVersion(),
                                   driver.swVersion()));
        });
        providers.remove(provider);
    }

    @Override
    public Class<? extends Behaviour> getBehaviourClass(String className) {
        return classes.get(className);
    }

    @Override
    public Set<Driver> getDrivers() {
        checkPermission(DRIVER_READ);
        ImmutableSet.Builder<Driver> builder = ImmutableSet.builder();
        drivers.values().forEach(builder::add);
        return builder.build();
    }

    @Override
    public Set<Driver> getDrivers(Class<? extends Behaviour> withBehaviour) {
        checkPermission(DRIVER_READ);
        return drivers.values().stream()
                .filter(d -> d.hasBehaviour(withBehaviour))
                .collect(Collectors.toSet());
    }

    @Override
    public Driver getDriver(String driverName) {
        checkPermission(DRIVER_READ);
        return nullIsNotFound(drivers.get(driverName), NO_DRIVER);
    }

    @Override
    public Driver getDriver(String mfr, String hw, String sw) {
        checkPermission(DRIVER_READ);

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
        return optional.orElse(drivers.get(DEFAULT));
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

    // Produces a composite driver key using the specified components.
    private String key(String mfr, String hw, String sw) {
        return String.format("%s-%s-%s", mfr, hw, sw);
    }
}
