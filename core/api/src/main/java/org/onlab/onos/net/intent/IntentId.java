package org.onlab.onos.net.intent;

/**
 * Intent identifier suitable as an external key.
 * <p/>
 * This class is immutable.
 */
public final class IntentId implements BatchOperationTarget {

    private static final int DEC = 10;
    private static final int HEX = 16;

    private final long id;

    /**
     * Creates an intent identifier from the specified string representation.
     *
     * @param value long value
     * @return intent identifier
     */
    public static IntentId valueOf(String value) {
        long id = value.toLowerCase().startsWith("0x")
                ? Long.parseLong(value.substring(2), HEX)
                : Long.parseLong(value, DEC);
        return new IntentId(id);
    }

    /**
     * Constructor for serializer.
     */
    protected IntentId() {
        this.id = 0;
    }

    /**
     * Constructs the ID corresponding to a given long value.
     *
     * @param id the underlying value of this ID
     */
    public IntentId(long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof IntentId)) {
            return false;
        }

        IntentId that = (IntentId) obj;
        return this.id == that.id;
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(id);
    }

}
