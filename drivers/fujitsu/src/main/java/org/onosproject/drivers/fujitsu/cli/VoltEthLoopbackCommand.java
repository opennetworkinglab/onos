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
import org.onosproject.net.DeviceId;
import org.onosproject.drivers.fujitsu.behaviour.VoltOnuOperConfig;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;

/**
 * Operates/releases loopback on Ethernet port of an ONU in vOLT.
 */
@Command(scope = "onos", name = "volt-ethloopback",
        description = "Operates/releases loopback on Ethernet port of an ONU in vOLT")
public class VoltEthLoopbackCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    String uri = null;

    @Argument(index = 1, name = "target", description = "PON link ID-ONU ID-Eth port ID[:(operate:release)]",
            required = true, multiValued = false)
    String target = null;

    private DeviceId deviceId;

    @Override
    protected void execute() {
        DriverService service = get(DriverService.class);
        deviceId = DeviceId.deviceId(uri);
        DriverHandler h = service.createHandler(deviceId);
        VoltOnuOperConfig volt = h.behaviour(VoltOnuOperConfig.class);
        String reply = volt.loopbackEthOnu(target);
        if (reply != null) {
            print("%s", reply);
        } else {
            print("No reply from %s", deviceId.toString());
        }
    }

}
