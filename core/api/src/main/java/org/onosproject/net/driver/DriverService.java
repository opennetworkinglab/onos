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

import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Service for obtaining drivers and driver behaviour implementations.
 */
public interface DriverService extends DriverResolver {

    /**
     * Returns the overall set of drivers being provided.
     *
     * @return provided drivers
     */
    Set<Driver> getDrivers();

    /**
     * Returns the set of drivers which support the specified behaviour.
     *
     * @param withBehaviour behaviour class to query by
     * @return provided drivers
     */
    Set<Driver> getDrivers(Class<? extends Behaviour> withBehaviour);

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
     * Returns the driver for the specified device. If the device carries
     * {@code driver} annotation, its value is used to look-up the driver.
     * Otherwise, the device manufacturer, hardware and software version
     * attributes are used to look-up the driver. First using their literal
     * values and if no driver is found, using ERE matching against the
     * driver manufacturer, hardware and software version fields.
     *
     * @param deviceId device identifier
     * @return driver or null of no matching one is found
     * @throws org.onlab.util.ItemNotFoundException if device or driver for it
     *                                              are not found
     */
    Driver getDriver(DeviceId deviceId);

    /**
     * Creates a new driver handler for interacting with the specified device.
     * The driver is looked-up using the same semantics as
     * {@link #getDriver(DeviceId)} method.
     *
     * @param deviceId    device identifier
     * @param credentials optional login credentials in string form
     * @return driver handler
     * @throws org.onlab.util.ItemNotFoundException if device or driver for it
     *                                              are not found
     */
    DriverHandler createHandler(DeviceId deviceId, String... credentials);

    // TODO: Devise a mechanism for retaining DriverData for devices

}
