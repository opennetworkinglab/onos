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

import com.google.common.base.MoreObjects;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.util.Constants;
import org.onosproject.bgpio.util.Validation;

import java.util.Objects;

/**
 * Provides Implementation of mandatory BGP Origin path attribute.
 */
public class Origin implements BgpValueType {

    /**
     * Enum to provide ORIGIN types.
     */
    public enum OriginType {
        IGP(0), EGP(1), INCOMPLETE(2);
        int value;
        /**
         * Assign val with the value as the ORIGIN type.
         *
         * @param val ORIGIN type
         */
        OriginType(int val) {
            value = val;
        }

        /**
         * Returns value of ORIGIN type.
         *
         * @return ORIGIN type
         */
        public byte getType() {
            return (byte) value;
        }
    }

    public static final byte ORIGIN_TYPE = 1;
    public static final byte ORIGIN_VALUE_LEN = 1;
    public static final byte FLAGS = (byte) 0x40;

    private boolean isOrigin = false;
    private byte origin;

    /**
     * Constructor to initialize parameters.
     *
     * @param origin origin value
     */
    public Origin(byte origin) {
        this.origin = origin;
        this.isOrigin = true;
    }

    /**
     * Returns true if origin attribute is present otherwise false.
     *
     * @return whether origin is present or not
     */
    public boolean isOriginSet() {
        return this.isOrigin;
    }

    /**
     * Returns type of Origin in Enum values.
     *
     * @return type of Origin in Enum values
     */
    public OriginType origin() {
        if (this.origin == 0) {
            return OriginType.IGP;
        } else if (this.origin == 1) {
            return OriginType.EGP;
        } else {
            return OriginType.INCOMPLETE;
        }
    }

    /**
     * Reads from ChannelBuffer and parses Origin.
     *
     * @param cb ChannelBuffer
     * @return object of Origin
     * @throws BgpParseException while parsing Origin path attribute
     */
    public static Origin read(ChannelBuffer cb) throws BgpParseException {
        ChannelBuffer tempCb = cb.copy();
        Validation parseFlags = Validation.parseAttributeHeader(cb);

        int len = parseFlags.isShort() ? parseFlags.getLength() + Constants.TYPE_AND_LEN_AS_SHORT : parseFlags
                .getLength() + Constants.TYPE_AND_LEN_AS_BYTE;
        ChannelBuffer data = tempCb.readBytes(len);
        if ((parseFlags.getLength() > ORIGIN_VALUE_LEN) || (cb.readableBytes() < parseFlags.getLength())) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                    parseFlags.getLength());
        }
        if (parseFlags.getFirstBit() && !parseFlags.getSecondBit() && parseFlags.getThirdBit()) {
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_FLAGS_ERROR, data);
        }

        byte originValue;
        originValue = cb.readByte();
        if ((originValue != OriginType.INCOMPLETE.value) && (originValue != OriginType.IGP.value) &&
              (originValue != OriginType.EGP.value)) {
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.INVALID_ORIGIN_ATTRIBUTE, data);
        }
        return new Origin(originValue);
    }

    @Override
    public short getType() {
        return ORIGIN_TYPE;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeByte(FLAGS);
        cb.writeByte(ORIGIN_TYPE);
        cb.writeByte(1);
        cb.writeByte(origin().value);
        return cb.writerIndex() - iLenStartIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Origin) {
            Origin other = (Origin) obj;
            return Objects.equals(origin, other.origin);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("origin", origin)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
