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
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverAdminService;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;


@Component(immediate = true)
@Service
public class DriverManager implements DriverAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

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
        //TODO
        return null;
    }

    @Override
    public Driver getDriver(String driverName) {
        //TODO: replace with fallback driver.
        return driverByName.getOrDefault(driverName, driverByName.get("default"));
    }

    @Override
    public Driver getDriver(String mfr, String hw, String sw) {
        return driverByKey.getOrDefault(key(mfr, hw, sw), driverByName.get("default"));
    }

    @Override
    public DriverHandler createHandler(String driverName, DeviceId deviceId, String... credentials) {
        Driver driver = driverByName.get(driverName);
        return new DefaultDriverHandler(new DefaultDriverData(driver));
    }

    @Override
    public DriverHandler createHandler(DriverData data, DeviceId deviceId, String... credentials) {
        return null;
    }

    private String key(String mfr, String hw, String sw) {
        return String.format("%s-%s-%s", mfr, hw, sw);
    }

}
