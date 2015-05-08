/*
 * Copyright 2014 Open Networking Laboratory
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
 * The class representing a network device chassisId.
 * This class is immutable.
 */
public final class ChassisId {

    private static final long UNKNOWN = 0;
    private final long value;

    /**
     * Default constructor.
     */
    public ChassisId() {
        this.value = ChassisId.UNKNOWN;
    }

    /**
     * Constructor from a long value.
     *
     * @param value the value to use.
     */
    public ChassisId(long value) {
        this.value = value;
    }

    /**
     * Constructor from a string.
     *
     * @param value the value to use.
     */
    public ChassisId(String value) {
        this.value = Long.parseLong(value, 16);
    }

    /**
     * Get the value of the chassis id.
     *
     * @return the value of the chassis id.
     */
    public long value() {
        return value;
    }

    /**
     * Convert the Chassis Id value to a ':' separated hexadecimal string.
     *
     * @return the Chassis Id value as a ':' separated hexadecimal string.
     */
    @Override
    public String toString() {
        return Long.toHexString(this.value);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ChassisId)) {
            return false;
        }

        ChassisId otherChassisId = (ChassisId) other;

        return value == otherChassisId.value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }
}
