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
package org.onosproject.provider.nil.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.UpDownCompleter;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.net.DeviceId;
import org.onosproject.provider.nil.NullProviders;

import static org.onosproject.cli.UpDownCompleter.DOWN;
import static org.onosproject.cli.UpDownCompleter.UP;

/**
 * Downs or repairs a simulated device.
 */
@Service
@Command(scope = "onos", name = "null-device",
        description = "Downs or repairs a simulated device")
public class NullDeviceCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "id", description = "Device identifier",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String id = null;

    @Argument(index = 1, name = "cmd", description = "up/down",
            required = true, multiValued = false)
    @Completion(UpDownCompleter.class)
    String cmd = null;


    @Override
    protected void doExecute() {
        NullProviders service = get(NullProviders.class);
        DeviceId deviceId = DeviceId.deviceId(id);

        if (cmd.equals(UP)) {
            service.repairDevice(deviceId);
        } else if (cmd.equals(DOWN)) {
            service.failDevice(deviceId);
        } else {
            error("Illegal command %s; must be up or down", cmd);
        }
    }

}
