package net.onrc.onos.of.ctl.util;

import org.projectfloodlight.openflow.util.HexString;

/**
 * The class representing a network switch DPID.
 * This class is immutable.
 */
public final class Dpid {
    private static final long UNKNOWN = 0;
    private final long value;

    /**
     * Default constructor.
     */
    public Dpid() {
        this.value = Dpid.UNKNOWN;
    }

    /**
     * Constructor from a long value.
     *
     * @param value the value to use.
     */
    public Dpid(long value) {
        this.value = value;
    }

    /**
     * Constructor from a string.
     *
     * @param value the value to use.
     */
    public Dpid(String value) {
        this.value = HexString.toLong(value);
    }

    /**
     * Get the value of the DPID.
     *
     * @return the value of the DPID.
     */
    public long value() {
        return value;
    }

    /**
     * Convert the DPID value to a ':' separated hexadecimal string.
     *
     * @return the DPID value as a ':' separated hexadecimal string.
     */
    @Override
    public String toString() {
        return HexString.toHexString(this.value);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Dpid)) {
            return false;
        }

        Dpid otherDpid = (Dpid) other;

        return value == otherDpid.value;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash += 31 * hash + (int) (value ^ value >>> 32);
        return hash;
    }
}
