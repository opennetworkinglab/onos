/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.net.device;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.provider.ProviderService;

import java.util.List;

/**
 * Service through which device providers can inject device information into
 * the core.
 */
public interface DeviceProviderService extends ProviderService<DeviceProvider> {

    // TODO: define suspend and remove actions on the mezzanine administrative API

    /**
     * Signals the core that a device has connected or has been detected somehow.
     *
     * @param deviceDescription information about network device
     */
    void deviceConnected(DeviceId deviceId, DeviceDescription deviceDescription);

    /**
     * Signals the core that a device has disconnected or is no longer reachable.
     *
     * @param deviceId identity of the device to be removed
     */
    void deviceDisconnected(DeviceId deviceId);

    /**
     * Sends information about all ports of a device. It is up to the core to
     * determine what has changed.
     * <p/>
     *
     * @param deviceId         identity of the device
     * @param portDescriptions list of device ports
     */
    void updatePorts(DeviceId deviceId, List<PortDescription> portDescriptions);

    /**
     * Used to notify the core about port status change of a single port.
     *
     * @param deviceId        identity of the device
     * @param portDescription description of the port that changed
     */
    void portStatusChanged(DeviceId deviceId, PortDescription portDescription);

    /**
     * Notifies the core about the providers inability to assert the specified
     * mastership role on the device.
     *
     * @param deviceId identity of the device
     * @param role mastership role that was asserted but failed
     */
    void unableToAssertRole(DeviceId deviceId, MastershipRole role);

}
