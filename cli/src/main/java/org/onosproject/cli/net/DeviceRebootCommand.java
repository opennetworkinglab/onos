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
 */
package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.behaviour.BasicSystemOperations;

/**
 * Administratively reboots device.
 */
@Service
@Command(scope = "onos", name = "device-reboot",
         description = "Administratively reboots a device")
public class DeviceRebootCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId", description = "Device ID",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String deviceId = null;

    @Override
    protected void doExecute() {
        Device dev = get(DeviceService.class).getDevice(DeviceId.deviceId(deviceId));
        if (dev == null) {
            print(" %s", "Device does not exist");
            return;
        }

        if (dev.is(BasicSystemOperations.class)) {
            print("Reboot for the device %s issued", deviceId);
            dev.as(BasicSystemOperations.class)
                    .reboot().whenComplete((future, error) -> {
                        if (error == null) {
                            print("Reboot for the device %s succeed.", deviceId);
                        } else {
                            log.error("Exception while rebooting device " + deviceId, error);
                        }
                    });

        } else {
            log.error("Device does not support {} behaviour", BasicSystemOperations.class.getName());
        }
    }
}
