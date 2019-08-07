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

package org.onosproject.driver.optical.power;

import java.util.Optional;

import com.google.common.collect.Range;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;

/**
 * Port Power implementation for Oplink protection switch device.
 * <p>
 * Intruduction:
 * _____
 * _____________________    |     | 0  VIRTUAL
 * |                     |   |  1  |
 * CLIENT   |                     |---|-----|--- PRIMARY
 * -----------|         OPS         |   |     |                NETWORK
 * 3     |                     |---|-----|--- SECONDARY
 * |_____________________|   |  2  |
 * |_____|
 * Uses flow to set switch path.
 * network port = 0, client port = 3, AUTO mode.
 * network port = 1 or 2, client port = 3, MANUAL mode.
 */

public class OplinkSwitchPowerConfig extends AbstractHandlerBehaviour
        implements PowerConfig<Object> {

    // oplink power config utility
    private OplinkPowerConfigUtil oplinkUtil = new OplinkPowerConfigUtil(this);

    @Override
    public Optional<Double> getTargetPower(PortNumber port, Object component) {
        Long power = oplinkUtil.getTargetPower(port, component);
        if (power == null) {
            return Optional.empty();
        }
        return Optional.of(power.doubleValue());
    }

    @Override
    public Optional<Double> currentPower(PortNumber port, Object component) {
        Long power = oplinkUtil.getCurrentPower(port, component);
        if (power == null) {
            return Optional.empty();
        }
        return Optional.of(power.doubleValue());
    }

    @Override
    public void setTargetPower(PortNumber port, Object component, double power) {
        oplinkUtil.setTargetPower(port, component, (long) power);
    }

    @Override
    public Optional<Range<Double>> getTargetPowerRange(PortNumber port, Object component) {
        Range<Long> power = oplinkUtil.getTargetPowerRange(port, component);
        if (power == null) {
            return Optional.empty();
        }
        return Optional.of(Range.closed((double) power.lowerEndpoint(), (double) power.upperEndpoint()));
    }

    @Override
    public Optional<Range<Double>> getInputPowerRange(PortNumber port, Object component) {
        Range<Long> power = oplinkUtil.getInputPowerRange(port, component);
        if (power == null) {
            return Optional.empty();
        }
        return Optional.of(Range.closed((double) power.lowerEndpoint(), (double) power.upperEndpoint()));
    }
}
