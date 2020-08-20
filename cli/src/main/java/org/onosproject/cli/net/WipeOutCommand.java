/*
 * Copyright 2014-present Open Networking Foundation
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

import com.google.common.collect.Sets;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.util.Tools;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostAdminService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.link.LinkAdminService;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketRequest;
import org.onosproject.net.region.RegionAdminService;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiTopoLayoutService;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.onosproject.net.intent.IntentState.WITHDRAWN;

/**
 * Wipes-out the entire network information base, i.e. devices, links, hosts, intents.
 */
@Service
@Command(scope = "onos", name = "wipe-out",
        description = "Wipes-out the entire network information base, i.e. devices, links, hosts")
public class WipeOutCommand extends AbstractShellCommand {

    private static final String PLEASE = "please";
    @Argument(name = "please", description = "Confirmation phrase")
    String please = null;

    @Override
    protected void doExecute() {
        if (please == null || !please.equals(PLEASE)) {
            print("I'm afraid I can't do that!\nSay: %s", PLEASE);
            return;
        }

        wipeOutIntents();
        wipeOutHosts();
        wipeOutFlows();
        wipeOutGroups();
        wipeOutMeters();
        wipeOutDevices();
        wipeOutLinks();
        wipeOutNetworkConfig();
        wipeOutPacketRequests();

        wipeOutLayouts();
        wipeOutRegions();
        wipeOutUiCache();
    }

    private void wipeOutIntents() {
        print("Wiping intents");
        IntentService intentService = get(IntentService.class);
        Set<Key> keysToWithdrawn = Sets.newConcurrentHashSet();
        Set<Intent> intentsToWithdrawn = Tools.stream(intentService.getIntents())
                .filter(intent -> intentService.getIntentState(intent.key()) != WITHDRAWN)
                .collect(Collectors.toSet());
        intentsToWithdrawn.stream()
                .map(Intent::key)
                .forEach(keysToWithdrawn::add);
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        IntentListener listener = e -> {
            if (e.type() == IntentEvent.Type.WITHDRAWN) {
                keysToWithdrawn.remove(e.subject().key());
            }
            if (keysToWithdrawn.isEmpty()) {
                completableFuture.complete(null);
            }
        };
        intentService.addListener(listener);
        intentsToWithdrawn.forEach(intentService::withdraw);
        try {
            if (!intentsToWithdrawn.isEmpty()) {
                // Wait 1.5 seconds for each Intent
                completableFuture.get(intentsToWithdrawn.size() * 1500L, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            print("Encountered exception while withdrawing intents: " + e.toString());
        } finally {
            intentService.removeListener(listener);
        }
        intentsToWithdrawn.forEach(intentService::purge);
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

    private void wipeOutMeters() {
        print("Wiping meters");
        MeterService meterService = get(MeterService.class);
        DeviceAdminService deviceAdminService = get(DeviceAdminService.class);
        for (Device device : deviceAdminService.getDevices()) {
            meterService.purgeMeters(device.id());
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

    private void wipeOutPacketRequests() {
        print("Wiping packet requests");
        PacketService service = get(PacketService.class);
        for (PacketRequest r : service.getRequests()) {
            service.cancelPackets(r.selector(), r.priority(), r.appId(), r.deviceId());
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

    private void wipeOutNetworkConfig() {
        print("Wiping network configs");
        get(NetworkConfigService.class).removeConfig();
    }

    private void wipeOutUiCache() {
        print("Wiping ui model cache");
        get(UiExtensionService.class).refreshModel();
    }

}
