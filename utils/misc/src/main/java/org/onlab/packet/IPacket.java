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

package org.onlab.packet;

/**
 * Packet Interface.
 */
public interface IPacket {

    /**
     * Obtain the packet payload.
     *
     * @return the payload
     */
    IPacket getPayload();

    /**
     * Assign the packet payload.
     *
     * @param packet new payload
     * @return self
     */
    IPacket setPayload(IPacket packet);

    /**
     * Obtain the parent packet.
     *
     * @return parent packet
     */
    IPacket getParent();

    /**
     * Configure a new parent packet.
     *
     * @param packet new parent
     * @return self
     */
    IPacket setParent(IPacket packet);

    /**
     * Reset any checksum as needed, and call resetChecksum on all parents.
     */
    void resetChecksum();

    /**
     * Sets all payloads parent packet if applicable, then serializes this
     * packet and all payloads.
     *
     * @return a byte[] containing this packet and payloads
     */
    byte[] serialize();


}
