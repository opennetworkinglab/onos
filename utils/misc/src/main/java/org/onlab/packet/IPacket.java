/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/**
 * Copyright 2011, Big Switch Networks, Inc.
 * Originally created by David Erickson, Stanford University
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 **/

package org.onlab.packet;

/**
 *
 */
public interface IPacket {
    /**
     *
     * @return
     */
    public IPacket getPayload();

    /**
     *
     * @param packet
     * @return
     */
    public IPacket setPayload(IPacket packet);

    /**
     *
     * @return
     */
    public IPacket getParent();

    /**
     *
     * @param packet
     * @return
     */
    public IPacket setParent(IPacket packet);

    /**
     * Reset any checksums as needed, and call resetChecksum on all parents.
     */
    public void resetChecksum();

    /**
     * Sets all payloads parent packet if applicable, then serializes this
     * packet and all payloads.
     *
     * @return a byte[] containing this packet and payloads
     */
    public byte[] serialize();

    /**
     * Deserializes this packet layer and all possible payloads.
     *
     * @param data
     * @param offset
     *            offset to start deserializing from
     * @param length
     *            length of the data to deserialize
     * @return the deserialized data
     */
    public IPacket deserialize(byte[] data, int offset, int length);

    /**
     * Clone this packet and its payload packet but not its parent.
     *
     * @return
     */
    public Object clone();
}
