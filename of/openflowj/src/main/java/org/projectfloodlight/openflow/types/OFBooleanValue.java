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

public class OFBooleanValue implements Writeable, OFValueType<OFBooleanValue> {
    public final static OFBooleanValue TRUE = new OFBooleanValue(true);
    public final static OFBooleanValue FALSE = new OFBooleanValue(false);

    public final static OFBooleanValue NO_MASK = TRUE;
    public final static OFBooleanValue FULL_MASK = FALSE;

    private final boolean value;

    private OFBooleanValue(boolean value) {
      this.value = value;
    }

    public static OFBooleanValue of(boolean value) {
      return value ? TRUE : FALSE;
    }

    public boolean getValue() {
      return value;
    }

    public int getInt() {
      return value ? 1 : 0;
    }

    @Override
    public String toString() {
        return "" + value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getInt();
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
        OFBooleanValue other = (OFBooleanValue) obj;
        if (value != other.value)
            return false;
        return true;
    }

    @Override
    public void writeTo(ChannelBuffer bb) {
        bb.writeByte(getInt());
    }

    private static class Reader implements OFMessageReader<OFBooleanValue> {
        @Override
        public OFBooleanValue readFrom(ChannelBuffer bb) throws OFParseError {
            return of(bb.readByte() != 0);
        }
    }

    @Override
    public int getLength() {
        return 1;
    }

    @Override
    public OFBooleanValue applyMask(OFBooleanValue mask) {
        return of(value && mask.value);
    }

    @Override
    public int compareTo(OFBooleanValue o) {
        return getInt() - o.getInt();
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putByte((byte)getInt());
    }
}
