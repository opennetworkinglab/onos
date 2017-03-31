/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.onlab.util.Identifier;

/**
 * The class representing a network device chassisId.
 * This class is immutable.
 */
public final class ChassisId extends Identifier<Long> {

    private static final long UNKNOWN = 0;

    /**
     * Default constructor.
     */
    public ChassisId() {
        super(ChassisId.UNKNOWN);
    }

    /**
     * Constructor from a long value.
     *
     * @param value the value to use.
     */
    public ChassisId(long value) {
        super(value);
    }

    /**
     * Constructor from a string.
     *
     * @param value the value to use.
     */
    public ChassisId(String value) {
        super(Long.parseUnsignedLong(value, 16));
    }

    /**
     * Get the value of the chassis id.
     *
     * @return the value of the chassis id.
     */
    public long value() {
        return identifier;
    }

    /**
     * Convert the Chassis Id value to a ':' separated hexadecimal string.
     *
     * @return the Chassis Id value as a ':' separated hexadecimal string.
     */
    @Override
    public String toString() {
        return Long.toHexString(identifier);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(identifier);
    }

    @Override
    public boolean equals(Object that) {
        return super.equals(that);
    }
}
