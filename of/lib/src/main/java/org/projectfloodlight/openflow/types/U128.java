package org.projectfloodlight.openflow.types;

import javax.annotation.Nonnull;

import org.jboss.netty.buffer.ChannelBuffer;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.UnsignedLongs;

public class U128 implements OFValueType<U128>, HashValue<U128> {

    static final int LENGTH = 16;

    private final long raw1; // MSBs
    private final long raw2; // LSBs

    public static final U128 ZERO = new U128(0, 0);

    private U128(long raw1, long raw2) {
        this.raw1 = raw1;
        this.raw2 = raw2;
    }

    public static U128 of(long raw1, long raw2) {
        if (raw1 == 0 && raw2 == 0)
            return ZERO;
        return new U128(raw1, raw2);
    }

    @Override
    public int getLength() {
        return LENGTH;
    }


    public long getMsb() {
        return raw1;
    }

    public long getLsb() {
        return raw2;
    }

    @Override
    public U128 applyMask(U128 mask) {
        return and(mask);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (raw1 ^ (raw1 >>> 32));
        result = prime * result + (int) (raw2 ^ (raw2 >>> 32));
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
        U128 other = (U128) obj;
        if (raw1 != other.raw1)
            return false;
        if (raw2 != other.raw2)
            return false;
        return true;
    }

    public void write16Bytes(ChannelBuffer cb) {
        cb.writeLong(raw1);
        cb.writeLong(raw2);
    }

    public static U128 read16Bytes(ChannelBuffer cb) {
        long raw1 = cb.readLong();
        long raw2 = cb.readLong();
        return of(raw1, raw2);
    }

    @Override
    public String toString() {
        return String.format("0x%016x%016x", raw1, raw2);
    }

    @Override
    public int compareTo(@Nonnull U128 o) {
        int msb = UnsignedLongs.compare(this.raw1, o.raw1);
        if(msb != 0)
            return msb;
        else
            return UnsignedLongs.compare(this.raw2, o.raw2);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putLong(raw1);
        sink.putLong(raw2);
    }

    @Override
    public U128 inverse() {
        return U128.of(~raw1, ~raw2);
    }

    @Override
    public U128 or(U128 other) {
        return U128.of(raw1 | other.raw1, raw2 | other.raw2);
    }

    @Override
    public U128 and(U128 other) {
        return U128.of(raw1 & other.raw1, raw2 & other.raw2);
    }

    @Override
    public U128 xor(U128 other) {
        return U128.of(raw1 ^ other.raw1, raw2 ^ other.raw2);
    }

    @Override
    public U128 add(U128 other) {
        long newRaw2 = this.raw2 + other.raw2;
        long newRaw1 = this.raw1 + other.raw1;
        if(UnsignedLongs.compare(newRaw2, this.raw2) < 0) {
            // raw2 overflow
            newRaw1+=1;
        }
        return U128.of(newRaw1, newRaw2);
    }

    @Override
    public U128 subtract(U128 other) {
        long newRaw2 = this.raw2 - other.raw2;
        long newRaw1 = this.raw1 - other.raw1;
        if(UnsignedLongs.compare(this.raw2, other.raw2) < 0) {
            newRaw1 -= 1;
        }
        return U128.of(newRaw1, newRaw2);
    }
    @Override
    public int prefixBits(int numBits) {
        return HashValueUtils.prefixBits(this.raw1, numBits);
    }

    @Override
    public HashValue.Builder<U128> builder() {
        return new U128Builder(raw1, raw2);
    }

    static class U128Builder implements HashValue.Builder<U128> {
        private long raw1, raw2;

        public U128Builder(long raw1, long raw2) {
            this.raw1 = raw1;
            this.raw2 = raw2;
        }

        @Override
        public Builder<U128> add(U128 other) {
            raw2 += other.raw2;
            raw1 += other.raw1;
            if(UnsignedLongs.compare(raw2, other.raw2) < 0) {
                // raw2 overflow
                raw1+=1;
            }
            return this;
        }

        @Override
        public Builder<U128> subtract(
                U128 other) {
            if(UnsignedLongs.compare(this.raw2, other.raw2) >= 0) {
                raw2 -= other.raw2;
                raw1 -= other.raw1;
            } else {
                // raw2 overflow
                raw2 -= other.raw2;
                raw1 = this.raw1 - other.raw1 - 1;
            }
            return this;
        }

        @Override
        public Builder<U128> invert() {
            raw1 = ~raw1;
            raw2 = ~raw2;
            return this;
        }

        @Override
        public Builder<U128> or(U128 other) {
            raw1 |= other.raw1;
            raw2 |= other.raw2;
            return this;
        }

        @Override
        public Builder<U128> and(U128 other) {
            raw1 &= other.raw1;
            raw2 &= other.raw2;
            return this;
        }

        @Override
        public Builder<U128> xor(U128 other) {
            raw1 ^= other.raw1;
            raw2 ^= other.raw2;
            return this;
        }

        @Override
        public U128 build() {
            return U128.of(raw1, raw2);
        }
    }

}
