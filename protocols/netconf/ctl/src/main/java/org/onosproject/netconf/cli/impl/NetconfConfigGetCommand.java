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
package org.onosproject.netconf.cli.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.netconf.DatastoreId.datastore;

import java.io.IOException;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfSession;

/**
 * Command that gets the configuration of the specified type from the specified
 * device. If configuration cannot be retrieved it prints an error string.
 */
@Command(scope = "onos", name = "netconf-get-config",
        description = "Gets the configuration of the specified type from the" +
                "specified device.")
public class NetconfConfigGetCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    String uri = null;

    @Argument(index = 1, name = "cfgType",
              description = "Configuration datastore name (running, etc.)",
              required = true, multiValued = false)
    String cfgType = null;


    private DeviceId deviceId;

    @Override
    protected void execute() {
        deviceId = DeviceId.deviceId(uri);

        NetconfController controller = get(NetconfController.class);
        checkNotNull(controller, "Netconf controller is null");

        NetconfDevice device = controller.getDevicesMap().get(deviceId);
        if (device == null) {
            print("Netconf device object not found for %s", deviceId);
            return;
        }

        NetconfSession session = device.getSession();
        if (session == null) {
            print("Netconf session not found for %s", deviceId);
            return;
        }

        try {
            String res = session.getConfig(datastore(cfgType.toLowerCase()));
            print("%s", res);
        } catch (IOException e) {
            log.error("Configuration could not be retrieved", e);
            print("Error occured retrieving configuration");
        }
    }

}
