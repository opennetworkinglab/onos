/*
 * Copyright 2014-2015 Open Networking Laboratory
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
 * Representation of a VLAN ID.
 */
public class VlanId {

    private final short value;

    // Based on convention used elsewhere? Check and change if needed
    public static final short UNTAGGED = (short) 0xffff;

    // In a traffic selector, this means that a VLAN ID must be present, but
    // can have any value. We use the same value as OpenFlow, but this is not
    // required.
    public static final short ANY_VALUE = (short) 0x1000;

    public static final VlanId NONE = VlanId.vlanId(UNTAGGED);
    public static final VlanId ANY = VlanId.vlanId(ANY_VALUE);

    // A VLAN ID is actually 12 bits of a VLAN tag.
    public static final short MAX_VLAN = 4095;

    protected VlanId() {
        this.value = UNTAGGED;
    }

    protected VlanId(short value) {
        this.value = value;
    }

    public static VlanId vlanId() {
        return new VlanId(UNTAGGED);
    }

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

    public short toShort() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof VlanId) {

            VlanId other = (VlanId) obj;

             if (this.value == other.value) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.value;
    }

    @Override
    public String toString() {
        if (this.value == ANY_VALUE) {
            return "Any";
        }
        return String.valueOf(this.value);
    }
}

