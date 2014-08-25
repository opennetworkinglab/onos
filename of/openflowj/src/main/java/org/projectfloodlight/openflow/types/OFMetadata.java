package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;

import com.google.common.hash.PrimitiveSink;

public class OFMetadata implements OFValueType<OFMetadata> {

    static int LENGTH = 8;

    private final U64 u64;

    public static final OFMetadata NONE = OFMetadata.of(U64.ZERO);

    public static final OFMetadata NO_MASK = OFMetadata.of(U64.ofRaw(0xFFFFFFFFFFFFFFFFl));
    public static final OFMetadata FULL_MASK = OFMetadata.of(U64.ofRaw(0x0));

    public OFMetadata(U64 ofRaw) {
        u64 = ofRaw;
    }

    public static OFMetadata of(U64 u64) {
        return new OFMetadata(u64);
    }

    public static OFMetadata ofRaw(long raw) {
        return new OFMetadata(U64.ofRaw(raw));
    }

    public U64 getValue() {
        return u64;
    }

    public static OFMetadata read8Bytes(ChannelBuffer cb) {
        return OFMetadata.ofRaw(cb.readLong());
    }

    public void write8Bytes(ChannelBuffer cb) {
        u64.writeTo(cb);
    }

    @Override
    public int getLength() {
        return u64.getLength();
    }

    @Override
    public OFMetadata applyMask(OFMetadata mask) {
        return OFMetadata.of(this.u64.applyMask(mask.u64));
    }

    @Override
    public boolean equals(Object arg0) {
        if (!(arg0 instanceof OFMetadata))
            return false;
        OFMetadata other = (OFMetadata)arg0;

        return this.u64.equals(other.u64);
    }

    @Override
    public int hashCode() {
        int prime = 53;
        return this.u64.hashCode() * prime;
    }

    @Override
    public String toString() {
        return "Metadata: " + u64.toString();
    }

    @Override
    public int compareTo(OFMetadata o) {
        return u64.compareTo(o.u64);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        u64.putTo(sink);
    }
}
