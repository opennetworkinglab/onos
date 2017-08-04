/*
 * Copyright 2017-present Open Networking Foundation
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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides a means of asynchronously waiting on devices.
 */
public final class AsyncDeviceFetcher {

    private DeviceService deviceService;

    private DeviceListener listener = new InternalDeviceListener();

    private Map<DeviceId, Runnable> onConnect = new ConcurrentHashMap<>();
    private Map<DeviceId, Runnable> onDisconnect = new ConcurrentHashMap<>();

    private AsyncDeviceFetcher(DeviceService deviceService) {
        this.deviceService = checkNotNull(deviceService);
        deviceService.addListener(listener);
    }

    /**
     * Shuts down.
     */
    public void shutdown() {
        deviceService.removeListener(listener);
        onConnect.clear();
        onDisconnect.clear();
    }

    /**
     * Executes provided callback when given device connects/disconnects.
     * @param deviceId device ID
     * @param onConnect callback that will be executed immediately if the device
     *                  is currently online, or when the device becomes online
     * @param onDisconnect callback that will be executed when the device becomes offline
     */
     void registerCallback(DeviceId deviceId, Runnable onConnect, Runnable onDisconnect) {
        if (onConnect != null) {
            if (deviceService.isAvailable(deviceId)) {
                onConnect.run();
            }
            this.onConnect.put(deviceId, onConnect);
        }
        if (onDisconnect != null) {
            this.onDisconnect.put(deviceId, onDisconnect);
        }
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
                    DeviceId deviceId = event.subject().id();
                    if (deviceService.isAvailable(deviceId)) {
                        Optional.ofNullable(onConnect.get(deviceId)).ifPresent(Runnable::run);
                    } else {
                        Optional.ofNullable(onDisconnect.get(deviceId)).ifPresent(Runnable::run);
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
