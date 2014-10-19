package org.onlab.packet;

/**
 * The class representing a network device chassisId.
 * This class is immutable.
 */
// TODO: Move this to a reasonable place.
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
        this.value = Long.valueOf(value);
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
        int hash = 17;
        hash += 31 * hash + (int) (value ^ value >>> 32);
        return hash;
    }
}
