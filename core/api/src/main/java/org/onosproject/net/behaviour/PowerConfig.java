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
package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import com.google.common.collect.Range;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.ArrayList;
import java.util.List;
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
@Beta
public interface PowerConfig<T> extends HandlerBehaviour {

    /**
     * Get the target power on the component.
     *
     * @param port the port
     * @param component the port component
     * @return target power in .01 dBm
     */
    Optional<Double> getTargetPower(PortNumber port, T component);

    /**
     * Set the target power on the component.
     *
     * @param port the port
     * @param component the port component
     * @param power target power in .01 dBm
     */
    void setTargetPower(PortNumber port, T component, double power);

    /**
     * Get the current output power on the component.
     *
     * @param port the port
     * @param component the port component
     * @return power power in .01 dBm
     */
    Optional<Double> currentPower(PortNumber port, T component);

    /**
     * Get the current input power on the component.
     * @param port the port
     * @param component the port component
     * @return power in .01 dBm
     */
    default Optional<Double> currentInputPower(PortNumber port, T component)  {
        return Optional.empty();
    }

    /**
     * Get the acceptable target power range for setTargetPower,
     * as optical components have different configurable output power ranges.
     *
     * @param port the port
     * @param component the port component
     * @return the accepted target power range, null if the component's power is
     * not configurable. For example the port target power can only be set on TX ports.
     */
    default Optional<Range<Double>> getTargetPowerRange(PortNumber port, T component) {
        return Optional.empty();
    }

    /**
     * Get the expected input power range for the component,
     * as optical components have different working input power ranges.
     *
     * @param port the port
     * @param component the port component
     * @return the expected input power range, null if the component does not have
     * a specified input power range. For example input power range only applies
     * to RX ports.
     */
    default Optional<Range<Double>> getInputPowerRange(PortNumber port, T component) {
        return Optional.empty();
    }

    /**
     * Get the ports, which support {@code PowerConfig} operations for the specified
     * {@code component}.
     *
     * @param component the port component
     * @return a set of power config ports
     */
    default List<PortNumber> getPorts(T component) {
        return new ArrayList<PortNumber>();
    }
}
