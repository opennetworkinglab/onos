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
import org.onosproject.net.MutableAnnotations;

/**
 * Container for data about an entity, e.g. device, link. Data is stored using
 * {@link org.onosproject.net.MutableAnnotations}.
 *
 * Note that only derivatives of {@link HandlerBehaviour} can expect mutability
 * from the backing driver data instance; other behaviours must rely on
 * immutable {@link org.onosproject.net.Annotations} only.
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
     * Implementations are expected to defer to the backing driver for creation
     * of the requested behaviour.
     *
     * @param behaviourClass behaviour class
     * @param <T>            type of behaviour
     * @return requested behaviour or null if not supported
     */
    default <T extends Behaviour> T behaviour(Class<T> behaviourClass) {
        return driver().createBehaviour(this, behaviourClass);
    }

}
