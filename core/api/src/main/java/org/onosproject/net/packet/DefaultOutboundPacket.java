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

import com.google.common.base.MoreObjects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Default implementation of an immutable outbound packet.
 */
public final class DefaultOutboundPacket implements OutboundPacket {
    private final DeviceId sendThrough;
    private final TrafficTreatment treatment;
    private final ByteBuffer data;
    private final PortNumber inPort;

    /**
     * Creates an immutable outbound packet.
     *
     * @param sendThrough identifier through which to send the packet
     * @param treatment   list of packet treatments
     * @param data        raw packet data
     */
    public DefaultOutboundPacket(DeviceId sendThrough,
            TrafficTreatment treatment, ByteBuffer data) {
        this.sendThrough = sendThrough;
        this.treatment = treatment;
        this.data = data;
        this.inPort = null;
    }

    /**
     * Creates an immutable outbound packet.
     *
     * @param sendThrough identifier through which to send the packet
     * @param treatment   list of packet treatments
     * @param data        raw packet data
     * @param inPort      input port to be used for the packet
     */
    public DefaultOutboundPacket(DeviceId sendThrough,
                                 TrafficTreatment treatment, ByteBuffer data,
                                 PortNumber inPort) {
        this.sendThrough = sendThrough;
        this.treatment = treatment;
        this.data = data;
        this.inPort = inPort;
    }

    @Override
    public DeviceId sendThrough() {
        return sendThrough;
    }

    @Override
    public TrafficTreatment treatment() {
        return treatment;
    }

    @Override
    public ByteBuffer data() {
        // FIXME: figure out immutability here
        return data;
    }

    @Override
    public PortNumber inPort() {
        return inPort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sendThrough, treatment, data, inPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OutboundPacket) {
            final DefaultOutboundPacket other = (DefaultOutboundPacket) obj;
            return Objects.equals(this.sendThrough, other.sendThrough) &&
                    Objects.equals(this.treatment, other.treatment) &&
                    Objects.equals(this.data, other.data) &&
                    Objects.equals(this.inPort, other.inPort);
        }
        return false;
    }
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("sendThrough", sendThrough)
                .add("treatment", treatment)
                .add("inPort", inPort)
                .toString();
    }
}
