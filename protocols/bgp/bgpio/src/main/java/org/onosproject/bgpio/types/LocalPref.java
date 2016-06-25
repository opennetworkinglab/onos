/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.bgpio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.util.Constants;
import org.onosproject.bgpio.util.Validation;

import com.google.common.base.MoreObjects;

/**
 * Provides implementation of LocalPref BGP Path Attribute.
 */
public class LocalPref implements BgpValueType {
    public static final byte LOCAL_PREF_TYPE = 5;
    public static final byte LOCAL_PREF_MAX_LEN = 4;
    public static final byte FLAGS = (byte) 0x40;

    private int localPref;

    /**
     * Constructor to initialize LocalPref.
     *
     * @param localPref local preference
     */
    public LocalPref(int localPref) {
        this.localPref = localPref;
    }

    /**
     * Returns local preference value.
     *
     * @return local preference value
     */
    public int localPref() {
        return this.localPref;
    }

    /**
     * Reads the channel buffer and returns object of LocalPref.
     *
     * @param cb channelBuffer
     * @return object of LocalPref
     * @throws BgpParseException while parsing localPref attribute
     */
    public static LocalPref read(ChannelBuffer cb) throws BgpParseException {
        int localPref;
        ChannelBuffer tempCb = cb.copy();
        Validation parseFlags = Validation.parseAttributeHeader(cb);
        if ((parseFlags.getLength() > LOCAL_PREF_MAX_LEN) || cb.readableBytes() < parseFlags.getLength()) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                    parseFlags.getLength());
        }

        int len = parseFlags.isShort() ? parseFlags.getLength() +
                  Constants.TYPE_AND_LEN_AS_SHORT : parseFlags.getLength() + Constants.TYPE_AND_LEN_AS_BYTE;
        ChannelBuffer data = tempCb.readBytes(len);
        if (parseFlags.getFirstBit()) {
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_FLAGS_ERROR, data);
        }

        localPref = cb.readInt();
        return new LocalPref(localPref);
    }

    @Override
    public short getType() {
        return LOCAL_PREF_TYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(localPref);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LocalPref) {
            LocalPref other = (LocalPref) obj;
            return Objects.equals(localPref, other.localPref);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("localPref", localPref)
                .toString();
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeByte(FLAGS);
        cb.writeByte(getType());
        cb.writeByte(4);
        cb.writeInt(localPref());
        return cb.writerIndex() - iLenStartIndex;
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}