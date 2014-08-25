package org.projectfloodlight.openflow.types;

import org.projectfloodlight.openflow.annotations.Immutable;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.UnsignedInts;

/**
 * Abstraction of a buffer id in OpenFlow. Immutable.
 *
 * @author Rob Vaterlaus <rob.vaterlaus@bigswitch.com>
 */
@Immutable
public class OFBufferId implements Comparable<OFBufferId>, PrimitiveSinkable {
    public static final OFBufferId NO_BUFFER = new OFBufferId(0xFFFFFFFF);

    private final int rawValue;

    private OFBufferId(int rawValue) {
        this.rawValue = rawValue;
    }

    public static OFBufferId of(final int rawValue) {
        if (rawValue == NO_BUFFER.getInt())
            return NO_BUFFER;
        return new OFBufferId(rawValue);
    }

    public int getInt() {
        return rawValue;
    }

    @Override
    public String toString() {
        return Long.toString(U32.f(rawValue));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + rawValue;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OFBufferId other = (OFBufferId) obj;
        if (rawValue != other.rawValue)
            return false;
        return true;
    }

    @Override
    public int compareTo(OFBufferId o) {
        return UnsignedInts.compare(rawValue, o.rawValue);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putInt(rawValue);
    }
}
