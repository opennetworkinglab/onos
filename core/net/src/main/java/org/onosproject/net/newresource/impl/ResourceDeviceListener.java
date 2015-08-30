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

import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.newresource.ResourceAdminService;
import org.onosproject.net.newresource.ResourcePath;

import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of DeviceListener registering devices as resources.
 */
final class ResourceDeviceListener implements DeviceListener {

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
        executor.submit(() -> adminService.registerResources(ResourcePath.ROOT, device.id()));
    }

    private void unregisterDeviceResource(Device device) {
        executor.submit(() -> adminService.unregisterResources(ResourcePath.ROOT, device.id()));
    }

    private void registerPortResource(Device device, Port port) {
        ResourcePath parent = new ResourcePath(device.id());
        executor.submit(() -> adminService.registerResources(parent, port.number()));
    }

    private void unregisterPortResource(Device device, Port port) {
        ResourcePath parent = new ResourcePath(device.id());
        executor.submit(() -> adminService.unregisterResources(parent, port.number()));
    }
}
