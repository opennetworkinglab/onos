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
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceService;

/**
 * Administratively enables or disabled a port on a device.
 */
@Service
@Command(scope = "onos", name = "portstate",
         description = "Administratively enables or disabled a port on a device")
public class DevicePortStateCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String uri = null;

    @Argument(index = 1, name = "portNumber", description = "Port Number",
            required = true, multiValued = false)
    @Completion(PortNumberCompleter.class)
    String portNumber = null;

    @Argument(index = 2, name = "portState",
            description = "Desired State. Either \"enable\" or \"disable\".",
            required = true, multiValued = false)
    @Completion(PortStateCompleter.class)
    String portState = null;

    @Override
    protected void doExecute() {
        DeviceService deviceService = get(DeviceService.class);
        DeviceAdminService deviceAdminService = get(DeviceAdminService.class);
        Device dev = deviceService.getDevice(DeviceId.deviceId(uri));
        if (dev == null) {
            print(" %s", "Device does not exist");
            return;
        }
        PortNumber pnum = PortNumber.fromString(portNumber);
        Port p = deviceService.getPort(dev.id(), pnum);
        if (p == null) {
            print(" %s", "Port does not exist");
            return;
        }
        if ("enable".equals(portState)) {
            deviceAdminService.changePortState(dev.id(), p.number(), true);
        } else if ("disable".equals(portState)) {
            deviceAdminService.changePortState(dev.id(), p.number(), false);
        } else {
            print(" %s", "State must be enable or disable");
        }
    }
}
