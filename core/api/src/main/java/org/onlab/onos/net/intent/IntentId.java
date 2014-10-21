package org.onlab.onos.net.intent;

/**
 * Intent identifier suitable as an external key.
 * <p/>
 * This class is immutable.
 */
public final class IntentId implements BatchOperationTarget {

    private final long fingerprint;

    /**
     * Creates an intent identifier from the specified string representation.
     *
     * @param fingerprint long value
     * @return intent identifier
     */
    static IntentId valueOf(long fingerprint) {
        return new IntentId(fingerprint);
    }

    /**
     * Constructor for serializer.
     */
    IntentId() {
        this.fingerprint = 0;
    }

    /**
     * Constructs the ID corresponding to a given long value.
     *
     * @param fingerprint the underlying value of this ID
     */
    IntentId(long fingerprint) {
        this.fingerprint = fingerprint;
    }

    @Override
    public int hashCode() {
        return (int) (fingerprint ^ (fingerprint >>> 32));
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
        return this.fingerprint == that.fingerprint;
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(fingerprint);
    }

}
