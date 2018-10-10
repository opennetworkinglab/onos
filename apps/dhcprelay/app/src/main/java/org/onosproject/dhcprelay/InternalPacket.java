/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.dhcprelay;

import org.onlab.packet.Ethernet;
import org.onosproject.net.ConnectPoint;

/**
 * Container for Ethernet packet and destination port.
 */
final class InternalPacket {
    private Ethernet packet;
    private ConnectPoint destLocation;

    private InternalPacket(Ethernet newPacket, ConnectPoint newLocation) {
        packet = newPacket;
        destLocation = newLocation;
    }

    public Ethernet getPacket() {
        return packet;
    }

    public ConnectPoint getDestLocation() {
        return destLocation;
    }

    /**
     * Build {@link InternalPacket} object instance.
     *
     * @param newPacket {@link Ethernet} packet to be sent
     * @param newLocation {@link ConnectPoint} packet destination
     * @return new instance of {@link InternalPacket} class
     */
    public static InternalPacket internalPacket(Ethernet newPacket, ConnectPoint newLocation) {
        return new InternalPacket(newPacket, newLocation);
    }
}
