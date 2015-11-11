/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.newresource.impl;

import com.google.common.collect.Lists;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.OchPort;
import org.onosproject.net.TributarySlot;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.newresource.ResourceAdminService;
import org.onosproject.net.newresource.ResourcePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of DeviceListener registering devices as resources.
 */
final class ResourceDeviceListener implements DeviceListener {

    private static final Logger log = LoggerFactory.getLogger(ResourceDeviceListener.class);

    private static final int TOTAL_ODU2_TRIBUTARY_SLOTS = 8;
    private static final int TOTAL_ODU4_TRIBUTARY_SLOTS = 80;
    private static final List<TributarySlot> ENTIRE_ODU2_TRIBUTARY_SLOTS = getEntireOdu2TributarySlots();
    private static final List<TributarySlot> ENTIRE_ODU4_TRIBUTARY_SLOTS = getEntireOdu4TributarySlots();

    private final ResourceAdminService adminService;
    private final ExecutorService executor;

    /**
     * Creates an instance with the specified ResourceAdminService and ExecutorService.
     *
     * @param adminService instance invoked to register resources
     * @param executor executor used for processing resource registration
     */
    ResourceDeviceListener(ResourceAdminService adminService, ExecutorService executor) {
        this.adminService = checkNotNull(adminService);
        this.executor = checkNotNull(executor);
    }

    @Override
    public void event(DeviceEvent event) {
        Device device = event.subject();
        switch (event.type()) {
            case DEVICE_ADDED:
                registerDeviceResource(device);
                break;
            case DEVICE_REMOVED:
                unregisterDeviceResource(device);
                break;
            case PORT_ADDED:
                registerPortResource(device, event.port());
                break;
            case PORT_REMOVED:
                unregisterPortResource(device, event.port());
                break;
            default:
                break;
        }
    }

    private void registerDeviceResource(Device device) {
        executor.submit(() -> adminService.registerResources(ResourcePath.discrete(device.id())));
    }

    private void unregisterDeviceResource(Device device) {
        executor.submit(() -> adminService.unregisterResources(ResourcePath.discrete(device.id())));
    }

    private void registerPortResource(Device device, Port port) {
        ResourcePath portPath = ResourcePath.discrete(device.id(), port.number());
        executor.submit(() -> {
            adminService.registerResources(portPath);

            switch (port.type()) {
                case OCH:
                    // register ODU TributarySlots against the OCH port
                    registerTributarySlotsResources(((OchPort) port).signalType(), portPath);
                    break;
                default:
                    break;
            }
        });
    }

    private void registerTributarySlotsResources(OduSignalType oduSignalType, ResourcePath portPath) {
        switch (oduSignalType) {
            case ODU2:
                adminService.registerResources(Lists.transform(ENTIRE_ODU2_TRIBUTARY_SLOTS, portPath::child));
                break;
            case ODU4:
                adminService.registerResources(Lists.transform(ENTIRE_ODU4_TRIBUTARY_SLOTS, portPath::child));
                break;
            default:
                break;
        }
    }

    private void unregisterPortResource(Device device, Port port) {
        ResourcePath resource = ResourcePath.discrete(device.id(), port.number());
        executor.submit(() -> adminService.unregisterResources(resource));
    }

    private static List<TributarySlot> getEntireOdu2TributarySlots() {
        return IntStream.rangeClosed(1, TOTAL_ODU2_TRIBUTARY_SLOTS)
                .mapToObj(x -> TributarySlot.of(x))
                .collect(Collectors.toList());
    }
    private static List<TributarySlot> getEntireOdu4TributarySlots() {
        return IntStream.rangeClosed(1, TOTAL_ODU4_TRIBUTARY_SLOTS)
                .mapToObj(x -> TributarySlot.of(x))
                .collect(Collectors.toList());
    }

}
