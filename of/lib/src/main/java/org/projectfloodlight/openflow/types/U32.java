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

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFMessageReader;
import org.projectfloodlight.openflow.protocol.Writeable;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.UnsignedInts;

public class U32 implements Writeable, OFValueType<U32> {
    private final static int ZERO_VAL = 0;
    public final static U32 ZERO = new U32(ZERO_VAL);

    private static final int NO_MASK_VAL = 0xFFffFFff;
    public final static U32 NO_MASK = new U32(NO_MASK_VAL);
    public static final U32 FULL_MASK = ZERO;

    private final int raw;

    private U32(int raw) {
        this.raw = raw;
    }

    public static U32 of(long value) {
        return ofRaw(U32.t(value));
    }

    public static U32 ofRaw(int raw) {
        if(raw == ZERO_VAL)
            return ZERO;
        if(raw == NO_MASK_VAL)
            return NO_MASK;
        return new U32(raw);
    }

    public long getValue() {
        return f(raw);
    }

    public int getRaw() {
        return raw;
    }

    @Override
    public String toString() {
        return String.format("0x%08x", raw);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + raw;
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
        U32 other = (U32) obj;
        if (raw != other.raw)
            return false;

        return true;
    }

    public static long f(final int i) {
        return i & 0xffffffffL;
    }

    public static int t(final long l) {
        return (int) l;
    }

    @Override
    public void writeTo(ChannelBuffer bb) {
        bb.writeInt(raw);
    }

    public final static Reader READER = new Reader();

    private static class Reader implements OFMessageReader<U32> {
        @Override
        public U32 readFrom(ChannelBuffer bb) throws OFParseError {
            return new U32(bb.readInt());
        }
    }

    @Override
    public int getLength() {
        return 4;
    }

    @Override
    public U32 applyMask(U32 mask) {
        return ofRaw(raw & mask.raw);
    }

    @Override
    public int compareTo(U32 o) {
        return UnsignedInts.compare(raw, o.raw);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putInt(raw);
    }}
