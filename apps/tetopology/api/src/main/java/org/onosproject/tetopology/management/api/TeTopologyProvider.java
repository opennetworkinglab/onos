/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api;

import org.onosproject.net.provider.Provider;

/**
 * Abstraction of an IETF network provider.
 * APIs for Manager to signal Provider.
 */
public interface TeTopologyProvider extends Provider {

    /**
     * TODO:
     *
     * Signals provider that abstract networks/network/link/node has been created/updated/deleted.
     * It's used by producers.
     *
     */

    // TODO: Need to look at the following functions are required for TE Nodes

    // TODO: consider how dirty the triggerProbe gets; if it costs too much, let's drop it

    /**
     * Triggers an asynchronous probe of the specified device, intended to
     * determine whether the device is present or not. An indirect result of this
     * should be invocation of
     * {@link org.onosproject.net.device.DeviceProviderService#deviceConnected} )} or
     * {@link org.onosproject.net.device.DeviceProviderService#deviceDisconnected}
     * at some later point in time.
     *
     * @param deviceId ID of device to be probed
     */
    //void triggerProbe(DeviceId deviceId);

    /**
     * Notifies the provider of a mastership role change for the specified
     * device as decided by the core.
     *
     * @param deviceId  device identifier
     * @param newRole newly determined mastership role
     */
    //void roleChanged(DeviceId deviceId, MastershipRole newRole);

    /**
     * Checks the reachability (connectivity) of a device from this provider.
     *
     * @param deviceId  device identifier
     * @return true if reachable, false otherwise
     */
    //boolean isReachable(DeviceId deviceId);

}
