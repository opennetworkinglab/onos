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
package org.onosproject.net.driver;

import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default driver provider implementation.
 */
public class DefaultDriverProvider implements DriverProvider {

    private final Map<String, DefaultDriver> drivers = new HashMap<>();

    @Override
    public Set<Driver> getDrivers() {
        return ImmutableSet.copyOf(drivers.values());
    }

    /**
     * Adds the specified driver to be provided.
     *
     * @param driverClasses driver to be provided
     */
    public void addDrivers(Set<DefaultDriver> driverClasses) {
        for (DefaultDriver driverClass : driverClasses) {
            addDriver(driverClass);
        }
    }

    /**
     * Adds the specified driver to be provided.
     *
     * @param driverClass driver to be provided
     */
    public void addDriver(DefaultDriver driverClass) {
        DefaultDriver ddc = drivers.get(driverClass.name());
        if (ddc == null) {
            // If we don't have the driver yet, just use the new one.
            drivers.put(driverClass.name(), driverClass);
        } else {
            // Otherwise merge the existing driver with the new one and rebind.
            drivers.put(driverClass.name(), ddc.merge(driverClass));
        }
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("drivers", drivers).toString();
    }
}
