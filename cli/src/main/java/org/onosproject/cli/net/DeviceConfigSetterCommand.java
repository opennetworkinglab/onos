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
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ConfigSetter;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Command that sets the configuration included in the specified file to the
 * specified device. It prints the response of the device.
 *
 * This is a temporary development tool for use until yang integration is complete.
 * This uses a not properly specified behavior. DO NOT USE AS AN EXAMPLE.
 *
 * @deprecated in 1.10.0
 */
//Temporary Developer tool, NOT TO BE USED in production or as example for
// future commands.
//FIXME this should eventually be removed.
@Deprecated
@Command(scope = "onos", name = "device-setconfiguration",
        description = "[Deprecated]Sets the configuration of the specified file to the " +
                "specified device.")
public class DeviceConfigSetterCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    private String uri = null;
    @Argument(index = 1, name = "cfgFile", description = "Configuration file",
            required = true, multiValued = false)
    private String cfgFile = null;
    private DeviceId deviceId;

    @Override
    protected void execute() {
        print("[WARN] This command was marked deprecated in 1.10.0");
        DriverService service = get(DriverService.class);
        deviceId = DeviceId.deviceId(uri);
        DriverHandler h = service.createHandler(deviceId);
        ConfigSetter config = h.behaviour(ConfigSetter.class);
        checkNotNull(cfgFile, "Configuration file cannot be null");
        print(config.setConfiguration(cfgFile));
    }

}
