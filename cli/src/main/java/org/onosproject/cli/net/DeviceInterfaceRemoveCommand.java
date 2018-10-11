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
package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;

/**
 * Removes an interface configurion from a device.
 */
@Service
@Command(scope = "onos", name = "device-remove-interface",
         description = "Removes an interface configuration from a device")
public class DeviceInterfaceRemoveCommand extends AbstractShellCommand {

    private static final String ONE_ACTION_ALLOWED =
            "One configuration removal allowed at a time";
    private static final String REMOVE_ACCESS_SUCCESS =
            "Access mode removed from device %s interface %s.";
    private static final String REMOVE_ACCESS_FAILURE =
            "Failed to remove access mode from device %s interface %s.";
    private static final String REMOVE_TRUNK_SUCCESS =
            "Trunk mode removed from device %s interface %s.";
    private static final String REMOVE_TRUNK_FAILURE =
            "Failed to remove trunk mode from device %s interface %s.";
    private static final String REMOVE_RATE_SUCCESS =
            "Rate limit removed from device %s interface %s.";
    private static final String REMOVE_RATE_FAILURE =
            "Failed to remove rate limit from device %s interface %s.";

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    private String uri = null;

    @Argument(index = 1, name = "interface",
            description = "Interface name",
            required = true, multiValued = false)
    private String portName = null;

    @Option(name = "-r", aliases = "--rate-limit",
            description = "Percentage for egress bandwidth limit",
            required = false, multiValued = false)
    private boolean rateLimit = false;

    @Option(name = "-t", aliases = "--trunk",
            description = "Remove trunk mode for VLAN(s)",
            required = false, multiValued = false)
    private boolean trunkMode = false;

    @Option(name = "-a", aliases = "--access",
            description = "Remove access mode for VLAN",
            required = false, multiValued = false)
    private boolean accessMode = false;

    @Override
    protected void doExecute() {
        DriverService service = get(DriverService.class);
        DeviceId deviceId = DeviceId.deviceId(uri);
        DriverHandler h = service.createHandler(deviceId);
        InterfaceConfig interfaceConfig = h.behaviour(InterfaceConfig.class);

        if (trunkMode && !accessMode && !rateLimit) {
            // Trunk mode for VLAN to be removed.
            removeTrunkModeFromIntf(interfaceConfig);
        } else if (accessMode && !trunkMode && !rateLimit) {
            // Access mode for VLAN to be removed.
            removeAccessModeFromIntf(interfaceConfig);
        } else if (rateLimit && !trunkMode && !accessMode) {
            // Rate limit to be removed.
            removeRateLimitFromIntf(interfaceConfig);
        } else {
            // Option has not been correctly set.
            print(ONE_ACTION_ALLOWED);
        }
    }

    private void removeAccessModeFromIntf(InterfaceConfig interfaceConfig) {
        if (interfaceConfig.removeAccessMode(portName)) {
            print(REMOVE_ACCESS_SUCCESS, uri, portName);
        } else {
            print(REMOVE_ACCESS_FAILURE, uri, portName);
        }
    }

    private void removeTrunkModeFromIntf(InterfaceConfig interfaceConfig) {
        if (interfaceConfig.removeTrunkMode(portName)) {
            print(REMOVE_TRUNK_SUCCESS, uri, portName);
        } else {
            print(REMOVE_TRUNK_FAILURE, uri, portName);
        }
    }

    private void removeRateLimitFromIntf(InterfaceConfig config) {
        if (config.removeRateLimit(portName)) {
            print(REMOVE_RATE_SUCCESS, uri, portName);
        } else {
            print(REMOVE_RATE_FAILURE, uri, portName);
        }
    }

}
