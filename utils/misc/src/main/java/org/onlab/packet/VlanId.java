package org.onlab.packet;

/**
 * Representation of a VLAN ID.
 */
public class VlanId {

    private final short value;

    // Based on convention used elsewhere? Check and change if needed
    public static final short UNTAGGED = (short) 0xffff;

    public static final VlanId NONE = VlanId.vlanId(UNTAGGED);

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
        return String.valueOf(this.value);
    }
}

