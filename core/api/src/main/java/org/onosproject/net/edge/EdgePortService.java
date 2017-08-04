/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.edge;

import org.onosproject.event.ListenerService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.TrafficTreatment;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Service for interacting with an inventory of network edge ports. A port
 * is considered an edge port if it is an active port and does not have an
 * infrastructure link associated with it.
 */
public interface EdgePortService
        extends ListenerService<EdgePortEvent, EdgePortListener> {

    /**
     * Indicates whether or not the specified connection point is an edge point.
     *
     * @param point connection point
     * @return true if edge point
     */
    boolean isEdgePoint(ConnectPoint point);

    /**
     * Returns a collection of all edge point within the current topology.
     *
     * @return iterable collection of all edge points
     */
    Iterable<ConnectPoint> getEdgePoints();

    /**
     * Returns a collection of all edge point for the specified device.
     *
     * @param deviceId device identifier
     * @return iterable collection of all edge points for the device
     */
    Iterable<ConnectPoint> getEdgePoints(DeviceId deviceId);

    /**
     * Emits the specified packet, with optional treatment to all edge ports.
     *
     * @param data      packet data
     * @param treatment optional traffic treatment to apply to the packet
     */
    void emitPacket(ByteBuffer data, Optional<TrafficTreatment> treatment);

    /**
     * Emits the specified packet, with optional treatment to all edge ports.
     *
     * @param deviceId  device where to send the packet out
     * @param data      packet data
     * @param treatment optional traffic treatment to apply to the packet
     */
    void emitPacket(DeviceId deviceId, ByteBuffer data,
                    Optional<TrafficTreatment> treatment);

}
