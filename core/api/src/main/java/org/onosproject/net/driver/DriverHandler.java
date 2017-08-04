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
package org.onosproject.net.driver;

/**
 * Representation of context for interacting with a device.
 */
public interface DriverHandler {

    /**
     * Returns the parent device driver.
     *
     * @return device driver
     */
    Driver driver();

    /**
     * Returns the device driver data.
     *
     * @return device driver data
     */
    DriverData data();

    /**
     * Returns the specified facet of behaviour to interact with the device.
     *
     * @param behaviourClass behaviour class
     * @param <T>            type of behaviour
     * @return behaviour
     */
    <T extends Behaviour> T behaviour(Class<T> behaviourClass);

    /**
     * Indicates whether or not the driver, or any of its parents, support
     * the specified class of behaviour.
     *
     * @param behaviourClass behaviour class
     * @return true if behaviour is supported
     */
    default boolean hasBehaviour(Class<? extends Behaviour> behaviourClass) {
        return driver().hasBehaviour(behaviourClass);
    }

    /**
     * Returns the reference to the implementation of the specified service.
     * Provides access to run-time context.
     *
     * @param serviceClass service class
     * @param <T>          type of service
     * @return service implementation
     * @throws org.onlab.osgi.ServiceNotFoundException if service is unavailable
     */
    <T> T get(Class<T> serviceClass);

}
