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
import com.google.common.primitives.UnsignedBytes;

public class U8 implements Writeable, OFValueType<U8> {
    private final static byte ZERO_VAL = 0;
    public final static U8 ZERO = new U8(ZERO_VAL);

    private static final byte NO_MASK_VAL = (byte) 0xFF;
    public static final U8 NO_MASK = new U8(NO_MASK_VAL);
    public static final U8 FULL_MASK = ZERO;

    private final byte raw;

    private U8(byte raw) {
        this.raw = raw;
    }

    public static final U8 of(short value) {
        if(value == ZERO_VAL)
            return ZERO;
        if(value == NO_MASK_VAL)
            return NO_MASK;

        return new U8(t(value));
    }

    public static final U8 ofRaw(byte value) {
        return new U8(value);
    }

    public short getValue() {
        return f(raw);
    }

    public byte getRaw() {
        return raw;
    }

    @Override
    public String toString() {
        return String.format("0x%02x", raw);
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
        U8 other = (U8) obj;
        if (raw != other.raw)
            return false;
        return true;
    }


    @Override
    public void writeTo(ChannelBuffer bb) {
        bb.writeByte(raw);
    }

    public static short f(final byte i) {
        return (short) (i & 0xff);
    }

    public static byte t(final short l) {
        return (byte) l;
    }


    public final static Reader READER = new Reader();

    private static class Reader implements OFMessageReader<U8> {
        @Override
        public U8 readFrom(ChannelBuffer bb) throws OFParseError {
            return new U8(bb.readByte());
        }
    }

    @Override
    public int getLength() {
        return 1;
    }

    @Override
    public U8 applyMask(U8 mask) {
        return ofRaw( (byte) (raw & mask.raw));
    }

    @Override
    public int compareTo(U8 o) {
        return UnsignedBytes.compare(raw, o.raw);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putByte(raw);
    }
 }
