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

import org.onlab.util.Identifier;

/**
 * Representation of a VLAN identifier.
 */
public final class VlanId extends Identifier<Short> {
    // Based on convention used elsewhere? Check and change if needed
    public static final short UNTAGGED = (short) 0xffff;

    // In a traffic selector, this means that a VLAN ID must be present, but
    // can have any value. We use the same value as OpenFlow, but this is not
    // required.
    public static final short ANY_VALUE = (short) 0x1000;

    public static final short NO_VID = 0;       // 0 is not used for VLAN ID
    public static final short RESERVED = 4095;  // represents all tagged traffic

    public static final VlanId NONE = VlanId.vlanId(UNTAGGED);
    public static final VlanId ANY = VlanId.vlanId(ANY_VALUE);

    private static final String STRING_NONE = "None";
    private static final String STRING_NUMERIC_NONE = "-1";
    private static final String STRING_ANY = "Any";

    // A VLAN ID is actually 12 bits of a VLAN tag.
    public static final short MAX_VLAN = 4095;

    // Constructor for serialization.
    private VlanId() {
        super(UNTAGGED);
    }

    // Creates a VLAN identifier for the specified VLAN number.
    private VlanId(short value) {
        super(value);
    }

    /**
     * Creates a VLAN identifier for untagged VLAN.
     *
     * @return VLAN identifier
     */
    public static VlanId vlanId() {
        return new VlanId(UNTAGGED);
    }

    /**
     * Creates a VLAN identifier using the supplied VLAN identifier.
     *
     * @param value VLAN identifier expressed as short
     * @return VLAN identifier
     */
    public static VlanId vlanId(short value) {
        if (value == UNTAGGED) {
            return new VlanId();
        }
        if (value == ANY_VALUE) {
            return new VlanId(ANY_VALUE);
        }
        if (value > MAX_VLAN) {
            throw new IllegalArgumentException(
                    "value exceeds allowed maximum VLAN ID value (4095)");
        }
        return new VlanId(value);
    }

    /**
     * Creates a VLAN identifier Object using the supplied VLAN identifier.
     *
     * @param value VLAN identifier expressed as string
     * @return VLAN identifier
     */
    public static VlanId vlanId(String value) {
        if (value.equals(STRING_NONE) || value.equals(STRING_NUMERIC_NONE)) {
            return new VlanId();
        }
        if (value.equals(STRING_ANY)) {
            return new VlanId(ANY_VALUE);
        }
        try {
            return VlanId.vlanId(Short.parseShort(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns the backing VLAN number.
     *
     * @return VLAN number
     */
    public short toShort() {
        return this.identifier;
    }

    @Override
    public String toString() {
        if (this.identifier == ANY_VALUE) {
            return "Any";
        }
        if (this.identifier == UNTAGGED) {
            return "None";
        }
        return String.valueOf(this.identifier);
    }
}

