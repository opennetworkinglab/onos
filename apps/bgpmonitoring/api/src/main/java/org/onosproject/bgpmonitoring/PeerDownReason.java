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
 * Enum to Provide the Different types of BMP peer down reasons.
 */
public enum PeerDownReason {

    LOCAL_SYSTEM_CLOSED_SESSION_WITH_NOTIFICATION((byte) 0x01),

    LOCAL_SYSTEM_CLOSED_SESSION_WITHOUT_NOTIFICATION((byte) 0x02),

    REMOTE_SYSTEM_CLOSED_SESSION_WITH_NOTIFICATION((byte) 0x03),

    REMOTE_SYSTEM_CLOSED_SESSION_WITHOUT_NOTIFICATION((byte) 0x04);


    private final byte value;

    /**
     * Assign value with the value val as the types of BMP peer down reasons.
     *
     * @param val type of BMP peer down reasons
     */
    PeerDownReason(byte val) {
        value = val;
    }

    /**
     * Returns value as type of BMP peer down reasons.
     *
     * @return value type of BMP peer down reasons
     */
    public byte getReason() {
        return value;
    }
}
