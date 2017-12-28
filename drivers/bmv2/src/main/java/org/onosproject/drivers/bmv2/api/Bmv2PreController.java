/*
 * Copyright 2018-present Open Networking Foundation
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
 *
 */
package org.onosproject.drivers.bmv2.api;

import org.onosproject.net.DeviceId;

/**
 * PRE Controller for BMv2 devices.
 */
public interface Bmv2PreController {

    /**
     * Creates a client to communicate with the PRE layer of a BMv2 device
     * and associates it with the given device identifier.
     * Returns true if the client was created and the channel to the device is open.
     * If so, a {@link Bmv2DeviceAgent} can be later obtained by invoking {@link #getPreClient(DeviceId)}.
     * Otherwise, returns false.
     *
     * @param deviceId         device identifier
     * @param thriftServerIp   Thrift server address
     * @param thriftServerPort Thrift server port
     * @return true if the client was created and the channel to the device is open; false otherwise
     * @throws IllegalStateException if a client already exists for the given device identifier
     */
    boolean createPreClient(DeviceId deviceId, String thriftServerIp, Integer thriftServerPort);

    /**
     * Returns the PRE client associated with the given device identifier, if any.
     *
     * @param deviceId device identifier
     * @return client instance if a client has already been created; null otherwise
     */
    Bmv2DeviceAgent getPreClient(DeviceId deviceId);

    /**
     * Removes the PRE client associated with the given device identifier.
     *
     * @param deviceId device identifier
     */
    void removePreClient(DeviceId deviceId);

}
