/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.optical;

import java.util.Optional;

import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.driver.Behaviour;

import com.google.common.annotations.Beta;


// TODO consider more fine grained device type. e.g., Transponder, WSS, ROADM
/**
 * Representation of a optical network infrastructure device.
 */
@Beta
public interface OpticalDevice extends Device, Behaviour {

    /**
     * Returns true if {@code port} is capable of being projected as the
     * specified class.
     *
     * @param port Port instance to test
     * @param portClass requested projection class
     * @param <T> type of Port
     * @return true if the requested projection is supported
     */
    <T extends Port> boolean portIs(Port port, Class<T> portClass);

    /**
     * Returns the specified projection of the {@code port} if such projection
     * is supported.
     *
     * @param port Port instance to project
     * @param portClass requested projection class
     * @param <T> type of Port
     * @return projection instance or empty if not supported.
     */
    <T extends Port> Optional<T> portAs(Port port, Class<T> portClass);

    /**
     * Returns most specific projection of the {@code port} or the {@code port}
     * itself.
     *
     * @param port Port instance
     * @return projection instance or {@code port} itself
     */
    Port port(Port port);

}
