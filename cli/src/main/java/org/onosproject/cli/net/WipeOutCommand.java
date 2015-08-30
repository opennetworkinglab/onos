/*
 * Copyright 2014-2015 Open Networking Laboratory
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
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.host.HostAdminService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.link.LinkAdminService;

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

        wipeOutIntents();
        wipeOutHosts();
        wipeOutDevices();
        wipeOutLinks();
    }

    private void wipeOutIntents() {
        print("Wiping intents");
        IntentService intentService = get(IntentService.class);
        for (Intent intent : intentService.getIntents()) {
            if (intentService.getIntentState(intent.key()) != IntentState.WITHDRAWN) {
                intentService.withdraw(intent);
            }
            intentService.purge(intent);
        }
    }

    private void wipeOutHosts() {
        print("Wiping hosts");
        HostAdminService hostAdminService = get(HostAdminService.class);
        while (hostAdminService.getHostCount() > 0) {
            try {
                for (Host host : hostAdminService.getHosts()) {
                    hostAdminService.removeHost(host.id());
                }
            } catch (Exception e) {
                log.warn("Unable to wipe-out hosts", e);
            }
        }
    }

    private void wipeOutDevices() {
        print("Wiping devices");
        DeviceAdminService deviceAdminService = get(DeviceAdminService.class);
        while (deviceAdminService.getDeviceCount() > 0) {
            try {
                for (Device device : deviceAdminService.getDevices()) {
                    deviceAdminService.removeDevice(device.id());
                }
            } catch (Exception e) {
                log.warn("Unable to wipe-out devices", e);
            }
        }
    }

    private void wipeOutLinks() {
        print("Wiping links");
        LinkAdminService linkAdminService = get(LinkAdminService.class);
        while (linkAdminService.getLinkCount() > 0) {
            try {
                for (Link link : linkAdminService.getLinks()) {
                    linkAdminService.removeLinks(link.src());
                    linkAdminService.removeLinks(link.dst());
                }
            } catch (Exception e) {
                log.warn("Unable to wipe-out links", e);
            }
        }
    }
}
