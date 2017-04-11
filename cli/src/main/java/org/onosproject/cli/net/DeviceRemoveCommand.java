/*
 * Copyright 2014-present Open Networking Laboratory
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
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceAdminService;

/**
 * Removes an infrastructure device.
 */
@Command(scope = "onos", name = "device-remove",
         description = "Removes an infrastructure device")
public class DeviceRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
              required = true, multiValued = false)
    String uri = null;

    @Override
    protected void execute() {
        try {
            get(DeviceAdminService.class).removeDevice(DeviceId.deviceId(uri));
        } catch (IllegalStateException e) {
            print("There was some issue in removing device, please try again");
        }
    }

}
