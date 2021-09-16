/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.group.impl;

import com.google.common.collect.Sets;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProgrammable;
import org.onosproject.net.group.GroupProvider;
import org.onosproject.net.group.GroupProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED;

/**
 * Driver-based Group rule provider.
 */
public class GroupDriverProvider extends AbstractProvider implements GroupProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // To be extracted for reuse as we deal with other.
    private static final String SCHEME = "default";
    private static final String PROVIDER_NAME = "org.onosproject.provider";

    // potentially positive device event
    private static final Set<DeviceEvent.Type> POSITIVE_DEVICE_EVENT =
            Sets.immutableEnumSet(DEVICE_ADDED,
                    DEVICE_AVAILABILITY_CHANGED);

    protected DeviceService deviceService;
    protected GroupProviderService groupProviderService;
    protected MastershipService mastershipService;

    private InternalDeviceListener deviceListener = new InternalDeviceListener();
    private ScheduledExecutorService executor
            = newSingleThreadScheduledExecutor(groupedThreads("GroupDriverProvider", "%d", log));
    private ScheduledFuture<?> poller = null;

    public GroupDriverProvider() {
        super(new ProviderId(SCHEME, PROVIDER_NAME));
    }

    /**
     * Initializes the provider with the necessary device service, group provider service,
     * mastership service and poll frequency.
     *
     * @param deviceService        device service
     * @param groupProviderService group provider service
     * @param mastershipService    mastership service
     * @param pollFrequency        group entry poll frequency
     */
    void init(DeviceService deviceService, GroupProviderService groupProviderService,
              MastershipService mastershipService, int pollFrequency) {
        this.deviceService = deviceService;
        this.groupProviderService = groupProviderService;
        this.mastershipService = mastershipService;

        deviceService.addListener(deviceListener);

        if (poller != null && !poller.isCancelled()) {
            poller.cancel(false);
        }

        poller = executor.scheduleAtFixedRate(this::pollGroups, pollFrequency,
                pollFrequency, TimeUnit.SECONDS);

    }

    void terminate() {
        deviceService.removeListener(deviceListener);
        deviceService = null;
        groupProviderService = null;
        mastershipService = null;
        poller.cancel(true);
        executor.shutdown();
    }

    @Override
    public void performGroupOperation(DeviceId deviceId, GroupOperations groupOps) {
        GroupProgrammable programmable = getGroupProgrammable(deviceId);
        if (programmable != null) {
            programmable.performGroupOperation(deviceId, groupOps);
        }
    }

    private void pollGroups() {
        try {
            deviceService.getAvailableDevices().forEach(device -> {
                if (mastershipService.isLocalMaster(device.id()) &&
                        device.is(GroupProgrammable.class)) {
                    pollDeviceGroups(device.id());
                }
            });
        } catch (Exception e) {
            log.warn("Exception thrown while polling groups", e);
        }
    }

    private void pollDeviceGroups(DeviceId deviceId) {
        try {
            Collection<Group> groups = getGroupProgrammable(deviceId).getGroups();
            groupProviderService.pushGroupMetrics(deviceId, groups);
        } catch (Exception e) {
            log.warn("Exception thrown while polling groups from {}", deviceId, e);
        }
    }

    private GroupProgrammable getGroupProgrammable(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        if (device.is(GroupProgrammable.class)) {
            return device.as(GroupProgrammable.class);
        } else {
            log.debug("Device {} is not group programmable", deviceId);
            return null;
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            executor.execute(() -> handleEvent(event));
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            Device device = event.subject();
            return POSITIVE_DEVICE_EVENT.contains(event.type()) &&
                    device.is(GroupProgrammable.class);
        }

        private void handleEvent(DeviceEvent event) {
            Device device = event.subject();
            boolean isRelevant = mastershipService.isLocalMaster(device.id()) &&
                    deviceService.isAvailable(device.id());

            if (isRelevant) {
                pollDeviceGroups(device.id());
            }
        }
    }
}
