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

package org.onosproject.olt.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cordconfig.access.AccessDeviceData;
import org.onosproject.net.DeviceId;
import org.onosproject.olt.AccessDeviceService;

import java.util.Map;

/**
 * Shows configured OLTs.
 */
@Command(scope = "onos", name = "olts",
        description = "Shows configured OLTs")
public class ShowOltCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "deviceId", description = "Access device ID",
            required = false, multiValued = false)
    private String strDeviceId = null;

    @Override
    protected void execute() {
        AccessDeviceService service = AbstractShellCommand.get(AccessDeviceService.class);
        Map<DeviceId, AccessDeviceData> data = service.fetchOlts();
        if (strDeviceId != null) {
            DeviceId deviceId = DeviceId.deviceId(strDeviceId);
            print("OLT %s:", deviceId);
            display(data.get(deviceId));
        } else {
            data.keySet().forEach(did -> {
                print("OLT %s:", did);
                display(data.get(did));
            });
        }
    }

    private void display(AccessDeviceData accessDeviceData) {
        print("\tvlan : %s", accessDeviceData.vlan());
        print("\tuplink : %s", accessDeviceData.uplink());
    }
}
