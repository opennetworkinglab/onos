/*
 * Copyright 2016-present Open Networking Laboratory
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

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProgrammable;
import org.onosproject.net.group.GroupProvider;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Driver-based Group rule provider.
 */
public class GroupDriverProvider extends AbstractProvider implements GroupProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // To be extracted for reuse as we deal with other.
    private static final String SCHEME = "default";
    private static final String PROVIDER_NAME = "org.onosproject.provider";
    protected DeviceService deviceService;

    public GroupDriverProvider() {
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
    public void performGroupOperation(DeviceId deviceId, GroupOperations groupOps) {
        GroupProgrammable programmable = getGroupProgrammable(deviceId);
        if (programmable != null) {
            programmable.performGroupOperation(deviceId, groupOps);
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
}
