package org.onlab.packet;

/**
 * Representation of a VLAN ID.
 */
public class VLANID {

    private final short value;
    private static final short NONE = 0;
    // A VLAN ID is actually 12 bits of a VLAN tag.
    private static final short MAX_VLAN = 4095;

    protected VLANID(short value) {
        this.value = value;
    }

    public static VLANID vlanId() {
        return new VLANID(NONE);
    }

    public static VLANID vlanId(short value) {
        if (value >= MAX_VLAN) {
            throw new IllegalArgumentException(
                    "value exceeds allowed maximum VLAN ID value (4095)");
        }
        return new VLANID(value);
    }

    public short toShort() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof VLANID) {

            VLANID other = (VLANID) obj;

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

