package org.onlab.onos.store.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import org.onlab.onos.store.Timestamp;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;

// If it is store specific, implement serializable interfaces?
/**
 * Default implementation of Timestamp.
 * TODO: Better documentation.
 */
public final class OnosTimestamp implements Timestamp {

    private final int termNumber;
    private final int sequenceNumber;

    /**
     * Default version tuple.
     *
     * @param termNumber the mastership termNumber
     * @param sequenceNumber  the sequenceNumber number within the termNumber
     */
    public OnosTimestamp(int termNumber, int sequenceNumber) {
        this.termNumber = termNumber;
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public int compareTo(Timestamp o) {
        checkArgument(o instanceof OnosTimestamp, "Must be OnosTimestamp", o);
        OnosTimestamp that = (OnosTimestamp) o;

        return ComparisonChain.start()
                .compare(this.termNumber, that.termNumber)
                .compare(this.sequenceNumber, that.sequenceNumber)
                .result();
    }

    @Override
    public int hashCode() {
        return Objects.hash(termNumber, sequenceNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OnosTimestamp)) {
            return false;
        }
        OnosTimestamp that = (OnosTimestamp) obj;
        return Objects.equals(this.termNumber, that.termNumber) &&
                Objects.equals(this.sequenceNumber, that.sequenceNumber);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                    .add("termNumber", termNumber)
                    .add("sequenceNumber", sequenceNumber)
                    .toString();
    }

    /**
     * Returns the termNumber.
     *
     * @return termNumber
     */
    public int termNumber() {
        return termNumber;
    }

    /**
     * Returns the sequenceNumber number.
     *
     * @return sequenceNumber
     */
    public int sequenceNumber() {
        return sequenceNumber;
    }
}