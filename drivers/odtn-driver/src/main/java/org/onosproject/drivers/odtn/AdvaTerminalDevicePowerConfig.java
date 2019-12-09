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
import org.slf4j.Logger;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Driver Implementation of the PowerConfig for OpenConfig terminal devices.
 */
public class AdvaTerminalDevicePowerConfig<T>
        extends TerminalDevicePowerConfig<T> implements PowerConfig<T> {

    private static final Logger log = getLogger(AdvaTerminalDevicePowerConfig.class);

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

    /**
     * Construct a rpc target power message.
     *
     * @param name for optical channel name
     * @param power to build rpc for target output power configuration
     * @return RPC payload
     */
    @Override
    public StringBuilder parsePortRequestRpc(Double power, String name) {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<component>");
        if (power != null) {
            // This is an edit-config operation.
            rpc.append("<config>")
                    .append("<name>")
                    .append(name)
                    .append("</name>")
                    .append("</config>")
                    .append("<optical-channel xmlns=\"http://openconfig.net/yang/terminal-device\">")
                    .append("<config>")
                    .append("<target-output-power>")
                    .append(power)
                    .append("</target-output-power>")
                    .append("</config>")
                    .append("</optical-channel>");
        } else {
            rpc.append("<name>")
                    .append(name)
                    .append("</name>");
        }
        return rpc;
    }
}
