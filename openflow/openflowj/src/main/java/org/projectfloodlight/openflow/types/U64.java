/**
 *    Copyright (c) 2008 The Board of Trustees of The Leland Stanford Junior
 *    University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package org.projectfloodlight.openflow.types;

import java.math.BigInteger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFMessageReader;
import org.projectfloodlight.openflow.protocol.Writeable;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.UnsignedLongs;

public class U64 implements Writeable, OFValueType<U64>, HashValue<U64> {
    private static final long UNSIGNED_MASK = 0x7fffffffffffffffL;
    private final static long ZERO_VAL = 0;
    public final static U64 ZERO = new U64(ZERO_VAL);

    private static final long NO_MASK_VAL = 0xFFffFFffFFffFFffL;
    public final static U64 NO_MASK = new U64(NO_MASK_VAL);
    public static final U64 FULL_MASK = ZERO;

    private final long raw;

    protected U64(final long raw) {
        this.raw = raw;
    }

    public static U64 of(long raw) {
        return ofRaw(raw);
    }

    public static U64 ofRaw(final long raw) {
        if(raw == ZERO_VAL)
            return ZERO;
        return new U64(raw);
    }

    public static U64 parseHex(String hex) {
        return new U64(new BigInteger(hex, 16).longValue());
    }

    public long getValue() {
        return raw;
    }

    public BigInteger getBigInteger() {
        BigInteger bigInt = BigInteger.valueOf(raw & UNSIGNED_MASK);
        if (raw < 0) {
          bigInt = bigInt.setBit(Long.SIZE - 1);
        }
        return bigInt;
    }

    @Override
    public String toString() {
        return String.format("0x%016x", raw);
    }

    public static BigInteger f(final long value) {
        BigInteger bigInt = BigInteger.valueOf(value & UNSIGNED_MASK);
        if (value < 0) {
          bigInt = bigInt.setBit(Long.SIZE - 1);
        }
        return bigInt;
    }

    public static long t(final BigInteger l) {
        return l.longValue();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (raw ^ (raw >>> 32));
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
        U64 other = (U64) obj;
        if (raw != other.raw)
            return false;
        return true;
    }

    @Override
    public int getLength() {
        return 8;
    }

    @Override
    public U64 applyMask(U64 mask) {
        return and(mask);
    }

    @Override
    public void writeTo(ChannelBuffer bb) {
        bb.writeLong(raw);
    }

    @Override
    public int compareTo(U64 o) {
        return UnsignedLongs.compare(raw, o.raw);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putLong(raw);
    }

    @Override
    public U64 inverse() {
        return U64.of(~raw);
    }

    @Override
    public U64 or(U64 other) {
        return U64.of(raw | other.raw);
    }

    @Override
    public U64 and(U64 other) {
        return ofRaw(raw & other.raw);
    }
    @Override
    public U64 xor(U64 other) {
        return U64.of(raw ^ other.raw);
    }

    @Override
    public U64 add(U64 other) {
        return U64.of(this.raw + other.raw);
    }

    @Override
    public U64 subtract(U64 other) {
        return U64.of(this.raw - other.raw);
    }

    /** return the "numBits" highest-order bits of the hash.
     *  @param numBits number of higest-order bits to return [0-32].
     *  @return a numberic value of the 0-32 highest-order bits.
     */
    @Override
    public int prefixBits(int numBits) {
        return HashValueUtils.prefixBits(raw, numBits);
    }

    public final static Reader READER = new Reader();

    private static class Reader implements OFMessageReader<U64> {
        @Override
        public U64 readFrom(ChannelBuffer bb) throws OFParseError {
            return U64.ofRaw(bb.readLong());
        }
    }

    @Override
    public HashValue.Builder<U64> builder() {
        return new U64Builder(raw);
    }

    static class U64Builder implements Builder<U64> {
        long raw;

        public U64Builder(long raw) {
            this.raw = raw;
        }

        @Override
        public Builder<U64> add(U64 other) {
            raw += other.raw;
            return this;
        }

        @Override
        public Builder<U64> subtract(
                U64 other) {
            raw -= other.raw;
            return this;
        }

        @Override
        public Builder<U64> invert() {
            raw = ~raw;
            return this;
        }

        @Override
        public Builder<U64> or(U64 other) {
            raw |= other.raw;
            return this;
        }

        @Override
        public Builder<U64> and(U64 other) {
            raw &= other.raw;
            return this;
        }

        @Override
        public Builder<U64> xor(U64 other) {
            raw ^= other.raw;
            return this;
        }

        @Override
        public U64 build() {
            return U64.of(raw);
        }
    }

}
