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
package org.onosproject.drivers.fujitsu.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.drivers.fujitsu.behaviour.VoltFwdlConfig;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;

/**
 * Requests manual firmware upgrade on a list of ONUs in vOLT.
 */
@Command(scope = "onos", name = "volt-ondemandfwdl",
        description = "Requests manual firmware upgrade on a list of ONUs in vOLT")
public class VoltOndemandFwdlCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    String uri = null;

    @Argument(index = 1, name = "target",
            description = "image name:PON link ID-ONU ID[,PON link ID-ONU ID,..:reboot-mode]",
            required = true, multiValued = false)
    String target = null;

    private DeviceId deviceId;

    @Override
    protected void execute() {
        DriverService service = get(DriverService.class);
        deviceId = DeviceId.deviceId(uri);
        DriverHandler h = service.createHandler(deviceId);
        VoltFwdlConfig volt = h.behaviour(VoltFwdlConfig.class);
        String reply = volt.upgradeFirmwareOndemand(target);
        if (reply != null) {
            print("%s", reply);
        } else {
            print("ONU firmware-upgrade failure %s", deviceId.toString());
        }
    }
}
