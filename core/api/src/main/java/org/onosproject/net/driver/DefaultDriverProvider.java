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
package org.onosproject.net.driver;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default driver provider implementation.
 */
public class DefaultDriverProvider implements DriverProvider {

    protected final Map<String, Driver> drivers = Maps.newConcurrentMap();

    @Override
    public Set<Driver> getDrivers() {
        return ImmutableSet.copyOf(drivers.values());
    }

    /**
     * Adds the specified drivers to the provider.
     *
     * @param drivers drivers to be added
     */
    public void addDrivers(Set<Driver> drivers) {
        drivers.forEach(this::addDriver);
    }

    /**
     * Adds the specified driver to the provider. If a driver with the same
     * does not exist yet, the specified one will be added. Otherwise, the
     * existing driver will be merged with the new one and the result will be
     * registered.
     *
     * @param driver driver to be provided
     * @return registered driver
     */
    public Driver addDriver(Driver driver) {
        return drivers.compute(driver.name(), (name, oldDriver) ->
                oldDriver == null ? driver : oldDriver.merge(driver));
    }

    /**
     * Removes the specified drivers from the provider.
     *
     * @param drivers drivers to be removed
     */
    public void removeDrivers(Set<Driver> drivers) {
        drivers.forEach(this::removeDriver);
    }

    /**
     * Removes the specified driver from the provider.
     *
     * @param driver driver to be removed
     */
    public void removeDriver(Driver driver) {
        // TODO: make selective if possible
        drivers.remove(driver.name());
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("drivers", drivers).toString();
    }
}
