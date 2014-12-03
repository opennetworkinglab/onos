/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.device.DeviceAdminService;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.host.HostAdminService;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.IntentState;

/**
 * Wipes-out the entire network information base, i.e. devices, links, hosts, intents.
 */
@Command(scope = "onos", name = "wipe-out",
         description = "Wipes-out the entire network information base, i.e. devices, links, hosts")
public class WipeOutCommand extends ClustersListCommand {

    private static final String PLEASE = "please";

    @Argument(index = 0, name = "please", description = "Confirmation phrase",
              required = false, multiValued = false)
    String please = null;

    @Override
    protected void execute() {
        if (please == null || !please.equals(PLEASE)) {
            print("I'm afraid I can't do that!\nSay: %s", PLEASE);
            return;
        }

        print("Wiping devices");
        DeviceAdminService deviceAdminService = get(DeviceAdminService.class);
        DeviceService deviceService = get(DeviceService.class);
        for (Device device : deviceService.getDevices()) {
            deviceAdminService.removeDevice(device.id());
        }

        print("Wiping hosts");
        HostAdminService hostAdminService = get(HostAdminService.class);
        HostService hostService = get(HostService.class);
        for (Host host : hostService.getHosts()) {
            hostAdminService.removeHost(host.id());
        }

        print("Wiping intents");
        IntentService intentService = get(IntentService.class);
        for (Intent intent : intentService.getIntents()) {
            if (intentService.getIntentState(intent.id()) == IntentState.INSTALLED) {
                intentService.withdraw(intent);
            }
        }
    }
}
