/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;

/**
 * Debug command to start subscription on specified device.
 */
@Command(scope = "onos", name = "netconf-subscription-test",
         description = "Debug command to start subscription on specified device")
public class NetconfSubscriptionTestCommand extends AbstractShellCommand {

    // for OSGi
    DeviceIdCompleter uriCompleter;

    @Argument(index = 0, name = "deviceId", description = "Device ID",
            required = true, multiValued = false)
    String uri = null;

    @Option(name = "--end",
            description = "Ends subscription instead of starting",
            required = false)
    boolean end = false;


    @Override
    protected void execute() {
        NetconfController controller = get(NetconfController.class);
        DeviceId did = DeviceId.deviceId(uri);

        NetconfDevice netconfDevice = controller.getNetconfDevice(did);
        if (netconfDevice == null) {
            print("%s not found or not connected to this node", did);
            return;
        }

        if (!end) {
            try {
                netconfDevice.getSession().startSubscription();
            } catch (NetconfException e) {
                log.error("Exception thrown", e);
                print("starting subscription failed (see log for details)");
            }
        } else {
            try {
                netconfDevice.getSession().endSubscription();
            } catch (NetconfException e) {
                log.error("Exception thrown", e);
                print("ending subscription failed (see log for details)");
            }
        }
    }

}
