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
import org.onosproject.net.MutableAnnotations;

/**
 * Container for data about a device. Data is stored using
 * {@link org.onosproject.net.MutableAnnotations}.
 */
public interface DriverData extends MutableAnnotations {

    /**
     * Returns the parent device driver.
     *
     * @return device driver
     */
    Driver driver();

    /**
     * Returns the device identifier.
     *
     * @return device identifier
     */
    DeviceId deviceId();

    /**
     * Returns the specified facet of behaviour to access the device data.
     *
     * @param behaviourClass behaviour class
     * @param <T>            type of behaviour
     * @return requested behaviour or null if not supported
     */
    <T extends Behaviour> T behaviour(Class<T> behaviourClass);

}
