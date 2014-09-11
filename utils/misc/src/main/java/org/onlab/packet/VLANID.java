package org.onlab.packet;

/**
 * Representation of a VLAN ID.
 */
public class VLANID {
    // A VLAN ID is 12 bits, short is close
    private final short value;

    public VLANID(short value) {
        this.value = value;
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
            return true;
        }

        VLANID other = (VLANID) obj;
        if (this.value == other.value) {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.value;
    }
}

