/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;

/**
 * Removes configured interface from a device.
 */
@Command(scope = "onos", name = "device-remove-interface",
         description = "Removes an interface configuration from a device")
public class DeviceInterfaceRemoveCommand extends AbstractShellCommand {

    private static final String REMOVE_ACCESS_SUCCESS =
            "Access mode deleted from device %s interface %s.";
    private static final String REMOVE_ACCESS_FAILURE =
            "Failed to delete access mode from device %s interface %s.";

    private static final String REMOVE_TRUNK_SUCCESS =
            "Trunk mode deleted from device %s interface %s.";
    private static final String REMOVE_TRUNK_FAILURE =
            "Failed to delete trunk mode from device %s interface %s.";

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    private String uri = null;

    @Argument(index = 1, name = "interface",
              description = "Interface name",
              required = true, multiValued = false)
    private String portName = null;

    @Option(name = "-t", aliases = "--trunk",
            description = "Remove trunk mode for VLAN(s)",
            required = false, multiValued = false)
    private boolean trunkMode = false;

    @Override
    protected void execute() {
        DriverService service = get(DriverService.class);
        DeviceId deviceId = DeviceId.deviceId(uri);
        DriverHandler h = service.createHandler(deviceId);
        InterfaceConfig interfaceConfig = h.behaviour(InterfaceConfig.class);

        if (trunkMode) {
            // Trunk mode for VLAN to be removed.
            if (interfaceConfig.removeTrunkInterface(deviceId, portName)) {
                print(REMOVE_TRUNK_SUCCESS, deviceId, portName);
            } else {
                print(REMOVE_TRUNK_FAILURE, deviceId, portName);
            }
            return;
        }

        // Access mode for VLAN to be removed.
        if (interfaceConfig.removeAccessInterface(deviceId, portName)) {
            print(REMOVE_ACCESS_SUCCESS, deviceId, portName);
        } else {
            print(REMOVE_ACCESS_FAILURE, deviceId, portName);
        }
    }

}
