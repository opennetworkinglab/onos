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

import org.onlab.onos.net.Device;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.provider.Provider;

/**
 * Abstraction of a device information provider.
 */
public interface DeviceProvider extends Provider {

    // TODO: consider how dirty the triggerProbe gets; if it costs too much, let's drop it

    /**
     * Triggers an asynchronous probe of the specified device, intended to
     * determine whether the device is present or not. An indirect result of this
     * should be invocation of
     * {@link org.onlab.onos.net.device.DeviceProviderService#deviceConnected} )} or
     * {@link org.onlab.onos.net.device.DeviceProviderService#deviceDisconnected}
     * at some later point in time.
     *
     * @param device device to be probed
     */
    void triggerProbe(Device device);

    /**
     * Notifies the provider of a mastership role change for the specified
     * device as decided by the core.
     *
     * @param device  affected device
     * @param newRole newly determined mastership role
     */
    void roleChanged(Device device, MastershipRole newRole);

}
