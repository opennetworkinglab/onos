/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.packet;

import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.TrafficSelector;

import java.util.Optional;

/**
 * Represents a packet request made to devices.
 */
public interface PacketRequest {

    /**
     * Obtains the traffic selector.
     *
     * @return a traffic selector
     */
    TrafficSelector selector();

    /**
     * Obtains the priority.
     *
     * @return a PacketPriority
     */
    PacketPriority priority();

    /**
     * Obtains the application id.
     *
     * @return an application id
     */
    ApplicationId appId();

    /**
     * Obtains the node id.
     *
     * @return an node id
     */
    NodeId nodeId();

    /**
     * Obtains the optional device id.
     *
     * @return an optional containing a device id
     */
    Optional<DeviceId> deviceId();

}
