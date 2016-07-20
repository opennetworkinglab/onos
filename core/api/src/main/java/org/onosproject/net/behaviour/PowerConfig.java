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
package org.onosproject.net.behaviour;

import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.Optional;

/**
 * Behavior for handling port power configurations.
 *
 * Power operations act on a network port and a component thereof.
 * Supported components are either the full directed port ({@link org.onosproject.net.Direction})
 * or a wavelength on a port ({@link org.onosproject.net.OchSignal}).
 *
 * Power levels are specified with a long and unit .01 dBm.
 */
public interface PowerConfig<T> extends HandlerBehaviour {

    /**
     * Get the target power on the component.
     *
     * @param port the port
     * @param component the port component
     * @return target power in .01 dBm
     */
    Optional<Long> getTargetPower(PortNumber port, T component);

    /**
     * Set the target power on the component.
     *
     *
     * @param port the port
     * @param component the port component
     * @param power target power in .01 dBm
     */
    void setTargetPower(PortNumber port, T component, long power);

    /**
     * Get the current power on the component.
     *
     * @param port the port
     * @param component the port component
     * @return power power in .01 dBm
     */
    Optional<Long> currentPower(PortNumber port, T component);
}
