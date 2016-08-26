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
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostAdminService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.link.LinkAdminService;
import org.onosproject.net.region.RegionAdminService;
import org.onosproject.ui.UiTopoLayoutService;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.onosproject.net.intent.IntentState.FAILED;
import static org.onosproject.net.intent.IntentState.WITHDRAWN;

/**
 * Wipes-out the entire network information base, i.e. devices, links, hosts, intents.
 */
@Command(scope = "onos", name = "wipe-out",
        description = "Wipes-out the entire network information base, i.e. devices, links, hosts")
public class WipeOutCommand extends ClustersListCommand {

    private static final String PLEASE = "please";
    private static final EnumSet<IntentState> CAN_PURGE = EnumSet.of(WITHDRAWN, FAILED);
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
        wipeOutFlows();
        wipeOutGroups();
        wipeOutDevices();
        wipeOutLinks();

        wipeOutLayouts();
        wipeOutRegions();
    }

    private void wipeOutIntents() {
        print("Wiping intents");
        IntentService intentService = get(IntentService.class);
        final CountDownLatch withdrawLatch;
        withdrawLatch = new CountDownLatch(1);
        for (Intent intent : intentService.getIntents()) {
            if (intentService.getIntentState(intent.key()) != IntentState.WITHDRAWN) {
                intentService.withdraw(intent);
                try { // wait for withdraw event
                    withdrawLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    print("Timed out waiting for intent {} withdraw");
                }
            }
            if (CAN_PURGE.contains(intentService.getIntentState(intent.key()))) {
                intentService.purge(intent);
            }
        }
    }

    private void wipeOutFlows() {
        print("Wiping Flows");
        FlowRuleService flowRuleService = get(FlowRuleService.class);
        DeviceAdminService deviceAdminService = get(DeviceAdminService.class);
        for (Device device : deviceAdminService.getDevices()) {
            flowRuleService.purgeFlowRules(device.id());
        }
    }

    private void wipeOutGroups() {
        print("Wiping groups");
        GroupService groupService = get(GroupService.class);
        DeviceAdminService deviceAdminService = get(DeviceAdminService.class);
        for (Device device : deviceAdminService.getDevices()) {
            groupService.purgeGroupEntries(device.id());
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
                log.info("Unable to wipe-out hosts", e);
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
                log.info("Unable to wipe-out devices", e);
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
                log.info("Unable to wipe-out links", e);
            }
        }
    }

    private void wipeOutLayouts() {
        print("Wiping UI layouts");
        UiTopoLayoutService service = get(UiTopoLayoutService.class);
        // wipe out all layouts except the default, which should always be there
        service.getLayouts().forEach(l -> {
            if (!l.id().isDefault()) {
                service.removeLayout(l);
            }
        });
    }

    private void wipeOutRegions() {
        print("Wiping regions");
        RegionAdminService service = get(RegionAdminService.class);
        service.getRegions().forEach(r -> service.removeRegion(r.id()));
    }
}
