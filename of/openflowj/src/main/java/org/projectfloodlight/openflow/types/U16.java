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
import com.google.common.primitives.Ints;

public class U16 implements Writeable, OFValueType<U16> {
    private final static short ZERO_VAL = 0;
    public final static U16 ZERO = new U16(ZERO_VAL);

    private static final short NO_MASK_VAL = (short)0xFFff;
    public final static U16 NO_MASK = new U16(NO_MASK_VAL);
    public static final U16 FULL_MASK = ZERO;

    public static int f(final short i) {
        return i & 0xffff;
    }

    public static short t(final int l) {
        return (short) l;
    }

    private final short raw;

    private U16(short raw) {
        this.raw = raw;
    }

    public static final U16 of(int value) {
        return ofRaw(t(value));
    }

    public static final U16 ofRaw(short raw) {
        if(raw == ZERO_VAL)
            return ZERO;
        return new U16(raw);
    }

    public int getValue() {
        return f(raw);
    }

    public short getRaw() {
        return raw;
    }

    @Override
    public String toString() {
        return String.format("0x%04x", raw);
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
        U16 other = (U16) obj;
        if (raw != other.raw)
            return false;
        return true;
    }


    @Override
    public void writeTo(ChannelBuffer bb) {
        bb.writeShort(raw);
    }


    public final static Reader READER = new Reader();

    private static class Reader implements OFMessageReader<U16> {
        @Override
        public U16 readFrom(ChannelBuffer bb) throws OFParseError {
            return ofRaw(bb.readShort());
        }
    }

    @Override
    public int getLength() {
        return 2;
    }

    @Override
    public U16 applyMask(U16 mask) {
        return ofRaw( (short) (raw & mask.raw));
    }

    @Override
    public int compareTo(U16 o) {
        return Ints.compare(f(raw), f(o.raw));
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putShort(raw);
    }
}
