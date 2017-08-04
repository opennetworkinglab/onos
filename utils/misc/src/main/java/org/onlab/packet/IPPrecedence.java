/*
 * Copyright 2016-present Open Networking Foundation
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
 * Represents the deprecated IPv4 IP precedence.
 * IP precedence occupied the 3 most-significant bits of the IPv4 ToS field
 */
public enum IPPrecedence {

    BEST_EFFORT((short) 0b000),
    PRIORITY((short) 0b001),
    IMMEDIATE((short) 0b010),
    FLASH((short) 0b011),
    FLASH_OVERRIDE((short) 0b100),
    CRITICAL((short) 0b101),
    INTERNETWORK_CONTROL((short) 0b110),
    NETWORK_CONTROL((short) 0b111);

    private short value;

    IPPrecedence(short value) {
        this.value = value;
    }

    /**
     * Returns the IP precedence Enum corresponding to the specified short.
     *
     * @param value the short value of the IP precedence
     * @return the IP precedence Enum corresponding to the specified short
     * @throws IllegalArgumentException if the short provided does not correspond to an IP precedence Enum value
     */
    public static IPPrecedence fromShort(short value) {
        for (IPPrecedence b : IPPrecedence.values()) {
            if (value == b.value) {
                return b;
            }
        }
        throw new IllegalArgumentException("IP precedence " + value + " is not valid");
    }

    /**
     * Returns the short value of this IP precedence Enum.
     *
     * @return the short value of this IP precedence Enum
     */
    public short getValue() {
        return value;
    }
}
