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

package org.onosproject.bgpio.protocol;

/**
 * Enum to provide BGP Message Version.
 */
public enum BgpVersion {

    BGP_4(4);

    public final int packetVersion;

    /**
     * Assign BGP PacketVersion with specified packetVersion.
     *
     * @param packetVersion version of BGP
     */
    BgpVersion(final int packetVersion) {
        this.packetVersion = packetVersion;
    }

    /**
     * Returns Packet version of BGP Message.
     *
     * @return packetVersion
     */
    public int getPacketVersion() {
        return packetVersion;
    }
}