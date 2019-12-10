/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.impl.behaviour.bng;

import com.google.common.collect.Maps;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import java.util.Map;

import static java.lang.String.format;

/**
 * Service that handles state necessary for the operations of {@link
 * FabricBngProgrammable}.
 */
@Component(immediate = true, service = FabricBngProgrammableService.class)
public final class FabricBngProgrammableService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    private final DeviceListener deviceListener = new InternalDeviceListener();
    private Map<DeviceId, SimpleBngLineIdAllocator> allocators;

    @Activate
    public void activate() {
        allocators = Maps.newHashMap();
        deviceService.addListener(deviceListener);
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        allocators.clear();
        allocators = null;
    }

    /**
     * Returns a {@link FabricBngLineIdAllocator} for the given device and
     * size.
     *
     * @param deviceId device ID
     * @param size     size of the allocator
     * @return allocator instance
     * @throws IllegalArgumentException if an existing allocator is found for
     *                                  the given device ID but with different
     *                                  size.
     */
    FabricBngLineIdAllocator getLineIdAllocator(DeviceId deviceId, long size) {
        return allocators.compute(deviceId, (d, allocator) -> {
            if (allocator != null) {
                if (allocator.size() == size) {
                    return allocator;
                } else {
                    throw new IllegalArgumentException(format(
                            "An allocator already exists for %s with size %d, " +
                                    "but one was requested with different size (%d)",
                            deviceId, allocators.get(deviceId).size(), size));
                }
            }
            return new SimpleBngLineIdAllocator(size);
        });
    }

    /**
     * Internal device event listener.
     */
    public class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            if (event.type() == DeviceEvent.Type.DEVICE_REMOVED) {
                allocators.remove(event.subject().id());
            }
        }
    }
}

