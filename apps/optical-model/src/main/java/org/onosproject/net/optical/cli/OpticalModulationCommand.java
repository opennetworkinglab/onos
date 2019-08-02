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
package org.onosproject.net.optical.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.NetconfOperationCompleter;
import org.onosproject.cli.net.OpticalConnectPointCompleter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Direction;
import org.onosproject.net.ModulationScheme;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.ModulationConfig;
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
@Command(scope = "onos", name = "modulation-config",
        description = "Get/Edit the modulation for specific optical-channel")
public class OpticalModulationCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(OpticalModulationCommand.class);

    @Argument(index = 0, name = "operation", description = "set modulation",
            required = true, multiValued = false)
    @Completion(NetconfOperationCompleter.class)
    private String operation = null;

    @Argument(index = 1, name = "connection point", description = "{DeviceID}/{PortNumber}",
            required = true, multiValued = false)
    @Completion(OpticalConnectPointCompleter.class)
    private String connectPoint = null;

    @Argument(index = 2, name = "value", description = "example: dp_qpsk, dp_8qam, dp_16qam",
            required = false, multiValued = false)
    private String value = null;

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
            log.warn("The modulation of selected port {} isn't editable.", port.number().toString());
            print("The modulation of selected port %s isn't editable.", port.number().toString());
            return;
        }
        Device device = deviceService.getDevice(cp.deviceId());
        if (device.is(ModulationConfig.class)) {
            ModulationConfig<Object> modulationConfig = device.as(ModulationConfig.class);
            // FIXME the parameter "component" equals NULL now, because there is one-to-one mapping between
            //  <component> and <optical-channel>.
            if (operation.equals("get")) {
                Direction component = Direction.ALL;
                Optional<ModulationScheme> scheme = modulationConfig.getModulationScheme(cp.port(), component);
                if (scheme.isPresent()) {
                    print("The modulation value in port %s on device %s is %s.",
                            cp.port().toString(), cp.deviceId().toString(), scheme.get().name());
                } else {
                    print("Can't get modulation for port %s on device %s.",
                            cp.port().toString(), cp.deviceId().toString());
                }
            } else if (operation.equals("edit-config")) {
                long bitRate = 0;
                if (value.equalsIgnoreCase(ModulationScheme.DP_QPSK.name())) {
                    bitRate = 100;
                } else {
                    bitRate = 200;
                }
                checkNotNull(value);
                Direction component = Direction.ALL;
                modulationConfig.setModulationScheme(cp.port(), component, bitRate);
                print("Set modulation for " + value + " for port %s on device %s.",
                        cp.port().toString(), cp.deviceId().toString());
            } else {
                log.warn("Operation {} are not supported now.", operation);
            }
        } else {
            print("Device is not capable of handling modulation");
        }
    }
}
