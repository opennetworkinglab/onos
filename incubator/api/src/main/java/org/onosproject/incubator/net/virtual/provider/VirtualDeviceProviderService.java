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

package org.onosproject.incubator.net.virtual.provider;

import org.onosproject.incubator.net.virtual.VirtualPortDescription;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.PortStatistics;

import java.util.Collection;
import java.util.List;

/**
 * Service through which virtual device providers can inject virtual device
 * information into the core.
 */
public interface VirtualDeviceProviderService
        extends VirtualProviderService<VirtualDeviceProvider> {

    /**
     * Updates information about all ports of a device. It is up to the core to
     * determine what has changed.
     *
     * @param deviceId         identity of the device
     * @param portDescs list of virtual device ports
     */
    void updatePorts(DeviceId deviceId, List<VirtualPortDescription> portDescs);

    /**
     * Notifies the core about port status change of a single port.
     *
     * @param deviceId        identity of the device
     * @param portDesc description of the virtual port that changed
     */
    void portStatusChanged(DeviceId deviceId, VirtualPortDescription portDesc);

    /**
     * Notifies the core about the result of a RoleRequest sent to a device.
     *
     * @param deviceId identity of the device
     * @param requested mastership role that was requested by the node
     * @param response mastership role the switch accepted
     */
    void receivedRoleReply(DeviceId deviceId, MastershipRole requested,
                           MastershipRole response);

    /**
     * Updates statistics about all ports of a device.
     *
     * @param deviceId          identity of the device
     * @param portStatistics  list of device port statistics
     */
    void updatePortStatistics(DeviceId deviceId,
                              Collection<PortStatistics> portStatistics);
}
