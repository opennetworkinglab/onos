/*
 * Copyright 2014-present Open Networking Foundation
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

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;

import java.nio.ByteBuffer;

/**
 * Represents an outbound data packet that is to be emitted to network via
 * an infrastructure device.
 */
public interface OutboundPacket {

    /**
     * Returns the identity of a device through which this packet should be
     * sent.
     *
     * @return device identity
     */
    DeviceId sendThrough();

    /**
     * Returns how the outbound packet should be treated.
     *
     * @return output treatment
     */
    TrafficTreatment treatment();

    /**
     * Returns immutable view of the raw data to be sent.
     *
     * @return data to emit
     */
    ByteBuffer data();

    /**
     * Returns the input port of this packet.
     *
     * Defaults to controller port. This is useful for actions that involve the input port
     * such as ALL or FLOOD.
     *
     * @return the input port to be used for this packet.
     */
    default PortNumber inPort() {
        return PortNumber.CONTROLLER;
    }
}
