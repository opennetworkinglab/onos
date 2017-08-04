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

package org.onosproject.net.device;

import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;

import java.util.Collection;
import java.util.List;

/**
 * Test Adapter for DeviceProviderService API.
 */
public class DeviceProviderServiceAdapter implements DeviceProviderService {
    @Override
    public void deviceConnected(DeviceId deviceId, DeviceDescription deviceDescription) {

    }

    @Override
    public void deviceDisconnected(DeviceId deviceId) {

    }

    @Override
    public void updatePorts(DeviceId deviceId, List<PortDescription> portDescriptions) {

    }

    @Override
    public void deletePort(DeviceId deviceId, PortDescription portDescription) {

    }

    @Override
    public void portStatusChanged(DeviceId deviceId, PortDescription portDescription) {

    }

    @Override
    public void receivedRoleReply(DeviceId deviceId, MastershipRole requested, MastershipRole response) {

    }

    @Override
    public void updatePortStatistics(DeviceId deviceId, Collection<PortStatistics> portStatistics) {

    }

    @Override
    public DeviceProvider provider() {
        return null;
    }
}
