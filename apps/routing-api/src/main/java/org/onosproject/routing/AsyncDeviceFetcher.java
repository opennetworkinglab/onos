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

package org.onosproject.routing;

import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a means of asynchronously waiting on devices.
 */
public final class AsyncDeviceFetcher {

    private DeviceService deviceService;

    private DeviceListener listener = new InternalDeviceListener();

    private Map<DeviceId, CompletableFuture<DeviceId>> devices = new ConcurrentHashMap();

    private AsyncDeviceFetcher(DeviceService deviceService) {
        this.deviceService = deviceService;
        deviceService.addListener(listener);
    }

    /**
     * Shuts down.
     */
    public void shutdown() {
        deviceService.removeListener(listener);
        devices.clear();
    }

    /**
     * Returns a completable future that completes when the device is available
     * for the first time.
     *
     * @param deviceId ID of the device
     * @return completable future
     */
    public CompletableFuture<DeviceId> getDevice(DeviceId deviceId) {
        CompletableFuture<DeviceId> future = new CompletableFuture<>();
        return devices.computeIfAbsent(deviceId, deviceId1 -> {
            if (deviceService.isAvailable(deviceId)) {
                future.complete(deviceId);
            }
            return future;
        });
    }

    /**
     * Creates a device fetcher based on the device service.
     *
     * @param deviceService device service
     * @return device fetcher
     */
    public static AsyncDeviceFetcher create(DeviceService deviceService) {
        return new AsyncDeviceFetcher(deviceService);
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
            case DEVICE_ADDED:
            case DEVICE_AVAILABILITY_CHANGED:
                if (deviceService.isAvailable(event.subject().id())) {
                    DeviceId deviceId = event.subject().id();
                    CompletableFuture<DeviceId> future = devices.get(deviceId);
                    if (future != null) {
                        future.complete(deviceId);
                    }
                }
                break;
            case DEVICE_UPDATED:
            case DEVICE_REMOVED:
            case DEVICE_SUSPENDED:
            case PORT_ADDED:
            case PORT_UPDATED:
            case PORT_REMOVED:
            default:
                break;
            }
        }
    }
}
