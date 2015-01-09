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

import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Service for obtaining drivers and driver behaviour implementations.
 */
public interface DriverService {

    /**
     * Returns the overall set of drivers being provided, optionally
     * filtered to only those that support all specified behaviours.
     *
     * @param withBehaviours optional behaviour classes to query by
     * @return provided drivers
     */
    Set<Driver> getDrivers(Class<? extends Behaviour>... withBehaviours);

    /**
     * Returns the specified driver.
     *
     * @param driverName driver name
     * @return driver
     */
    Driver getDriver(String driverName);

    /**
     * Returns the driver that matches the specified primordial device
     * discovery information.
     *
     * @param mfr device manufacturer
     * @param hw  device hardware name/version
     * @param sw  device software version
     * @return driver or null of no matching one is found
     */
    Driver getDriver(String mfr, String hw, String sw);

    /**
     * Creates a new driver handler for the specified driver.
     *
     * @param driverName  driver name
     * @param deviceId    device identifier
     * @param credentials optional login credentials in string form
     * @return driver handler
     */
    DriverHandler createHandler(String driverName, DeviceId deviceId,
                                String... credentials);

    /**
     * Creates a new driver handler for the specified driver data.
     *
     * @param data        driver data
     * @param deviceId    device identifier
     * @param credentials optional login credentials in string form
     * @return driver handler
     */
    DriverHandler createHandler(DriverData data, DeviceId deviceId,
                                String... credentials);

}
