/*
 * Copyright 2019-present Open Networking Foundation
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
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.drivers.odtn;

import com.google.common.collect.Range;
import org.onosproject.drivers.odtn.openconfig.TerminalDevicePowerConfig;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;

import java.util.Optional;

/**
 * Driver Implementation of the PowerConfig for OpenConfig terminal devices.
 */
public class CassiniTerminalDevicePowerConfig<T>
        extends TerminalDevicePowerConfig<T> implements PowerConfig<T> {
    /**
     * Getting target value of output power.
     * @param port port
     * @param component the component
     * @return target output power range
     */
    @Override
    public Optional<Range<Double>> getTargetPowerRange(PortNumber port, Object component) {
        double targetMin = -30;
        double targetMax = 1;
        return Optional.of(Range.open(targetMin, targetMax));
    }

    @Override
    public Optional<Range<Double>> getInputPowerRange(PortNumber port, Object component) {
        double targetMin = -30;
        double targetMax = 1;
        return Optional.of(Range.open(targetMin, targetMax));
    }

}