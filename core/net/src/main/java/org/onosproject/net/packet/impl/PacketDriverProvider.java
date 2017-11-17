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

package org.onosproject.net.packet.impl;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketProgrammable;
import org.onosproject.net.packet.PacketProvider;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Driver-based packet rule provider.
 */
public class PacketDriverProvider extends AbstractProvider implements PacketProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // To be extracted for reuse as we deal with other.
    private static final String SCHEME = "default";
    private static final String PROVIDER_NAME = "org.onosproject.provider";
    protected DeviceService deviceService;

    public PacketDriverProvider() {
        super(new ProviderId(SCHEME, PROVIDER_NAME));
    }

    /**
     * Initializes the provider with the necessary device service.
     *
     * @param deviceService device service
     */
    void init(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    public void emit(OutboundPacket packet) {
        PacketProgrammable programmable = getPacketProgrammable(packet.sendThrough());
        if (programmable != null) {
            programmable.emit(packet);
        }
    }

    private PacketProgrammable getPacketProgrammable(DeviceId deviceId) {
        if (deviceService == null) {
            log.debug("Packet encountered but device service is not ready, dropping");
            return null;
        }
        Device device = deviceService.getDevice(deviceId);
        if (device.is(PacketProgrammable.class)) {
            return device.as(PacketProgrammable.class);
        } else {
            log.debug("Device {} is not packet programmable", deviceId);
            return null;
        }
    }
}
