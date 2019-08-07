/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Direction;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Get the target-output-power for specific optical-channel.
 * This is a command for PowerConfig.
 */
@Service
@Command(scope = "onos", name = "power-config",
        description = "Get/Edit the target-output-power for specific optical-channel")
public class PowerConfigCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(PowerConfigCommand.class);

    @Argument(index = 0, name = "operation", description = "Netconf Operation including get, edit-config, etc.",
            required = true, multiValued = false)
    @Completion(NetconfOperationCompleter.class)
    private String operation = null;

    @Argument(index = 1, name = "connection point", description = "{DeviceID}/{PortNumber}",
            required = true, multiValued = false)
    @Completion(OpticalConnectPointCompleter.class)
    private String connectPoint = null;

    @Argument(index = 2, name = "value", description = "target-output-power value. Unit: dBm",
            required = false, multiValued = false)
    private Double value = null;

    @Override
    protected void doExecute() throws Exception {
        DeviceService deviceService = get(DeviceService.class);
        ConnectPoint cp = ConnectPoint.deviceConnectPoint(connectPoint);
        Port port = deviceService.getPort(cp);
        if (port == null) {
            print("[ERROR] %s does not exist", cp);
            return;
        }
        if (!port.type().equals(Port.Type.OCH) &&
                !port.type().equals(Port.Type.OTU) &&
                !port.type().equals(Port.Type.OMS)) {
            log.warn("The power of selected port %s isn't editable.", port.number().toString());
            print("The power of selected port %s isn't editable.", port.number().toString());
            return;
        }
        Device device = deviceService.getDevice(cp.deviceId());
        PowerConfig powerConfig = device.as(PowerConfig.class);
        // FIXME the parameter "component" equals NULL now, because there is one-to-one mapping between
        //  <component> and <optical-channel>.
        if (operation.equals("get")) {
            Optional<Double> val = powerConfig.getTargetPower(cp.port(), Direction.ALL);
            if (val.isPresent()) {
                double power = val.orElse(Double.MIN_VALUE);
                print("The target-output-power value in port %s on device %s is %f.",
                        cp.port().toString(), cp.deviceId().toString(), power);
            }
            Optional<Double> currentPower = powerConfig.currentPower(cp.port(), Direction.ALL);
            if (currentPower.isPresent()) {
                double currentPowerVal = currentPower.orElse(Double.MIN_VALUE);
                print("The current-output-power value in port %s on device %s is %f.",
                        cp.port().toString(), cp.deviceId().toString(), currentPowerVal);
            }
            Optional<Double> currentInputPower = powerConfig.currentInputPower(cp.port(), Direction.ALL);
            if (currentInputPower.isPresent()) {
                double inputPowerVal = currentInputPower.orElse(Double.MIN_VALUE);
                print("The current-input-power value in port %s on device %s is %f.",
                        cp.port().toString(), cp.deviceId().toString(), inputPowerVal);
            }
        } else if (operation.equals("edit-config")) {
            checkNotNull(value);
            powerConfig.setTargetPower(cp.port(), Direction.ALL, value);
            print("Set %f power on port", value, connectPoint);
        } else {
            print("Operation %s are not supported now.", operation);
        }
    }
}
