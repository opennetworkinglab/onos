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
package org.onosproject.net.driver.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.component.ComponentService;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriverProvider;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverAdminService;
import org.onosproject.net.driver.DriverEvent;
import org.onosproject.net.driver.DriverListener;
import org.onosproject.net.driver.DriverProvider;
import org.onosproject.net.driver.DriverRegistry;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.nullIsNotFound;
import static org.onosproject.net.driver.DriverEvent.Type.DRIVER_ENHANCED;
import static org.onosproject.net.driver.DriverEvent.Type.DRIVER_REDUCED;
import static org.onosproject.net.driver.impl.OsgiPropertyConstants.REQUIRED_DRIVERS;
import static org.onosproject.net.driver.impl.OsgiPropertyConstants.REQUIRED_DRIVERS_DEFAULT;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.DRIVER_READ;


/**
 * Manages inventory of device drivers.
 */
@Component(
    immediate = true,
    service = {
        DriverAdminService.class,
        DriverRegistry.class
    },
    property = {
        REQUIRED_DRIVERS + "=" + REQUIRED_DRIVERS_DEFAULT,
    })

public class DriverRegistryManager extends DefaultDriverProvider implements DriverAdminService {

    private static final String DRIVER_COMPONENT = "org.onosproject.net.driver.impl.DriverManager";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String FORMAT = "Modified, Required drivers: {}";
    private static final String COMMA = ",";
    private static final String NO_DRIVER = "Driver not found";
    private static final String DEFAULT = "default";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentService componentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EventDeliveryService eventDispatcher;

    /** Comma-separated list of drivers that must be registered before starting driver subsystem. */
    private static String requiredDrivers = REQUIRED_DRIVERS_DEFAULT;
    private Set<String> requiredDriverSet;

    private Set<DriverProvider> providers = Sets.newConcurrentHashSet();
    private Map<String, Driver> driverByKey = Maps.newConcurrentMap();
    private Map<String, Class<? extends Behaviour>> classes = Maps.newConcurrentMap();

    private ListenerRegistry<DriverEvent, DriverListener> listenerRegistry;


    private boolean isStarted = false;

    @Activate
    protected void activate(ComponentContext context) {
        componentConfigService.registerProperties(getClass());
        listenerRegistry = new ListenerRegistry<>();
        eventDispatcher.addSink(DriverEvent.class, listenerRegistry);
        modified(context);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        componentConfigService.unregisterProperties(getClass(), false);
        eventDispatcher.removeSink(DriverEvent.class);
        providers.clear();
        driverByKey.clear();
        classes.clear();
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
        if (context != null) {
            requiredDrivers = get(properties, REQUIRED_DRIVERS);
        }
        requiredDriverSet = isNullOrEmpty(requiredDrivers) ?
                ImmutableSet.of() : ImmutableSet.copyOf(requiredDrivers.split(COMMA));
        log.info(FORMAT, requiredDrivers);
        checkRequiredDrivers();
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
            post(new DriverEvent(DRIVER_ENHANCED, driver));
        });
        providers.add(provider);
        checkRequiredDrivers();
    }

    @Override
    public void unregisterProvider(DriverProvider provider) {
        provider.getDrivers().forEach(driver -> {
            removeDriver(driver);
            driverByKey.remove(key(driver.manufacturer(),
                                   driver.hwVersion(),
                                   driver.swVersion()));
            post(new DriverEvent(DRIVER_REDUCED, driver));
        });
        providers.remove(provider);
        checkRequiredDrivers();
    }

    // Checks for the minimum required drivers and when available, activate
    // the driver manager components; deactivate otherwise.
    private synchronized void checkRequiredDrivers() {
        Set<String> driverSet = registeredDrivers();
        boolean isReady = driverSet.containsAll(requiredDriverSet);
        log.debug("RequiredDriverSet {}, isReady {}, isStarted {}",
                  requiredDriverSet, isReady, isStarted);
        if (isReady && !isStarted) {
            log.info("Starting driver subsystem");
            componentService.activate(null, DRIVER_COMPONENT);
            isStarted = true;
        } else if (!isReady && isStarted) {
            log.info("Stopping driver subsystem");
            componentService.deactivate(null, DRIVER_COMPONENT);
            isStarted = false;
        }
    }

    private Set<String> registeredDrivers() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (DriverProvider dp : providers) {
            dp.getDrivers().stream().map(Driver::name).forEach(builder::add);
        }
        return builder.build();
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

    @Override
    public Driver getDriver(String driverName) {
        checkPermission(DRIVER_READ);
        return nullIsNotFound(drivers.get(driverName), NO_DRIVER);
    }

    // Matches the given driver using ERE matching against the given criteria.
    private boolean matches(Driver d, String mfr, String hw, String sw) {
        // TODO: consider pre-compiling the expressions in the future
        return mfr.matches(d.manufacturer()) &&
                hw.matches(d.hwVersion()) &&
                sw.matches(d.swVersion());
    }

    // Produces a composite driver key using the specified components.
    static String key(String mfr, String hw, String sw) {
        return String.format("%s-%s-%s", mfr, hw, sw);
    }

    @Override
    public void addListener(DriverListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(DriverListener listener) {
        listenerRegistry.removeListener(listener);
    }

    // Safely posts the specified event to the local event dispatcher.
    private void post(DriverEvent event) {
        if (eventDispatcher != null) {
            eventDispatcher.post(event);
        }
    }

}
