/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.bgpmonitoring;

/**
 * Enum to Provide the Different types of BMP messages.
 */
public enum BmpType {

    ROUTE_MONITORING((byte) 0x0),

    STATISTICS_REPORT((byte) 0x1),

    PEER_DOWN_NOTIFICATION((byte) 0x2),

    PEER_UP_NOTIFICATION((byte) 0x3),

    INITIATION_MESSAGE((byte) 0x4),

    TERMINATION_MESSAGE((byte) 0x5),

    ROUTE_MIRRORING_MESSAGE((byte) 0x6);

    private final byte value;

    /**
     * Assign value with the value val as the types of BMP message.
     *
     * @param val type of BMP message
     */
    BmpType(byte val) {
        value = val;
    }


    /**
     * Returns value as type of BMP message.
     *
     * @return value type of BMP message
     */
    public byte getType() {
        return value;
    }
}
