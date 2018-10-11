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
import org.onosproject.net.Device;
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.device.DeviceInterfaceDescription;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;

import java.util.List;

import static org.onosproject.net.DeviceId.deviceId;

/**
 * Lists all interfaces or interfaces of a device.
 */
@Service
@Command(scope = "onos", name = "device-interfaces",
        description = "Lists all interfaces or interfaces of a device.")
public class DeviceInterfacesListCommand extends DevicesListCommand {
    private static final String FORMAT = "%s";
    private static final String MODE_FORMAT = " mode=";
    private static final String ACCESS_MODE = "access";
    private static final String TRUNK_MODE = "trunk";
    private static final String VLAN_FORMAT = " vlan=";
    private static final String LIMIT_FORMAT = " rate-limit=";
    private static final String ERROR_RESULT = "Cannot retrieve interfaces for device";
    private static final String NO_INTERFACES = "No interfaces found";
    private static final String PERCENT = "%%";

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = false, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    private String uri = null;

    @Override
    protected void doExecute() {
        DeviceService deviceService = get(DeviceService.class);
        DriverService driverService = get(DriverService.class);

        if (uri == null) {
            // No specific device, so all devices will be examined.
            for (Device device : getSortedDevices(deviceService)) {
                printDevice(deviceService, driverService, device);
            }
        } else {
            Device device = deviceService.getDevice(deviceId(uri));
            printDevice(deviceService, driverService, device);
        }
    }

    private void printDevice(DeviceService deviceService,
                             DriverService driverService,
                             Device device) {
        super.printDevice(deviceService, device);
        if (!device.is(InterfaceConfig.class)) {
            // The relevant behavior is not supported by the device.
            print(ERROR_RESULT);
            return;
        }
        DriverHandler h = driverService.createHandler(device.id());
        InterfaceConfig interfaceConfig = h.behaviour(InterfaceConfig.class);

        List<DeviceInterfaceDescription> interfaces =
                interfaceConfig.getInterfaces();
        if (interfaces == null) {
            print(ERROR_RESULT);
        } else if (interfaces.isEmpty()) {
            print(NO_INTERFACES);
        } else {
            interfaces.forEach(this::printInterface);
        }
    }

    private void printInterface(DeviceInterfaceDescription intf) {
        StringBuilder formatStringBuilder = new StringBuilder(FORMAT);

        if (intf.mode().equals(DeviceInterfaceDescription.Mode.ACCESS)) {
            formatStringBuilder.append(MODE_FORMAT)
                    .append(ACCESS_MODE)
                    .append(VLAN_FORMAT);
            formatStringBuilder.append(intf.vlans().get(0).toString());
        } else if (intf.mode().equals(DeviceInterfaceDescription.Mode.TRUNK)) {
            formatStringBuilder.append(MODE_FORMAT)
                    .append(TRUNK_MODE)
                    .append(VLAN_FORMAT);
            for (int i = 0; i < intf.vlans().size(); i++) {
                formatStringBuilder.append(intf.vlans().get(i));
                if (i != intf.vlans().size() - 1) {
                    formatStringBuilder.append(",");
                }
            }
        }

        if (intf.isRateLimited()) {
            formatStringBuilder.append(LIMIT_FORMAT);
            formatStringBuilder.append(intf.rateLimit());
            formatStringBuilder.append(PERCENT);
        }

        print(formatStringBuilder.toString(), intf.name());
    }
}
