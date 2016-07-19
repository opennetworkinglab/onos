/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.lisp.msg.types;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * LISP Canonical Address Formatted address class.
 *
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           AFI = 16387         |     Rsvd1     |     Flags     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    Type       |     Rsvd2     |            Length             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class LispLcafAddress extends LispAfiAddress {

    protected final LispCanonicalAddressFormatEnum lcafType;
    protected final byte reserved1;
    protected final byte reserved2;
    protected final byte flag;
    protected final byte length;

    /**
     * Initializes LCAF address.
     *
     * @param lcafType LCAF type
     * @param reserved1 reserved1 field
     * @param reserved2 reserved2 field
     * @param flag flag field
     * @param length length field
     */
    protected LispLcafAddress(LispCanonicalAddressFormatEnum lcafType,
                              byte reserved1, byte reserved2, byte flag, byte length) {
        super(AddressFamilyIdentifierEnum.LCAF);
        this.lcafType = lcafType;
        this.reserved1 = reserved1;
        this.reserved2 = reserved2;
        this.flag = flag;
        this.length = length;
    }

    /**
     * Initializes LCAF address.
     *
     * @param lcafType LCAF type
     * @param reserved2 reserved2 field
     * @param flag flag field
     * @param length length field
     */
    protected LispLcafAddress(LispCanonicalAddressFormatEnum lcafType,
                              byte reserved2, byte flag, byte length) {
        super(AddressFamilyIdentifierEnum.LCAF);
        this.lcafType = lcafType;
        this.reserved2 = reserved2;
        this.flag = flag;
        this.length = length;
        this.reserved1 = 0;
    }

    /**
     * Initializes LCAF address.
     *
     * @param lcafType LCAF type
     * @param reserved2 reserved2 field
     * @param length length field
     */
    protected LispLcafAddress(LispCanonicalAddressFormatEnum lcafType,
                              byte reserved2, byte length) {
        super(AddressFamilyIdentifierEnum.LCAF);
        this.lcafType = lcafType;
        this.reserved2 = reserved2;
        this.length = length;
        this.reserved1 = 0;
        this.flag = 0;
    }

    /**
     * Initializes LCAF address.
     *
     * @param lcafType LCAF type
     * @param reserved2 reserved2 field
     */
    protected LispLcafAddress(LispCanonicalAddressFormatEnum lcafType, byte reserved2) {
        super(AddressFamilyIdentifierEnum.LCAF);
        this.lcafType = lcafType;
        this.reserved2 = reserved2;
        this.reserved1 = 0;
        this.flag = 0;
        this.length = 0;
    }

    /**
     * Initializes LCAF address.
     *
     * @param lcafType LCAF type
     */
    protected LispLcafAddress(LispCanonicalAddressFormatEnum lcafType) {
        super(AddressFamilyIdentifierEnum.LCAF);
        this.lcafType = lcafType;
        this.reserved1 = 0;
        this.reserved2 = 0;
        this.flag = 0;
        this.length = 0;
    }

    /**
     * Obtains LCAF type.
     *
     * @return LCAF type
     */
    public LispCanonicalAddressFormatEnum getType() {
        return lcafType;
    }

    /**
     * Obtains LCAF reserved1 value.
     *
     * @return LCAF reserved1 value
     */
    public byte getReserved1() {
        return reserved1;
    }

    /**
     * Obtains LCAF reserved2 value.
     *
     * @return LCAF reserved2 value
     */
    public byte getReserved2() {
        return reserved2;
    }

    /**
     * Obtains LCAF flag value.
     *
     * @return LCAF flag value
     */
    public byte getFlag() {
        return flag;
    }

    /**
     * Obtains LCAF length value.
     *
     * @return LCAF length value
     */
    public byte getLength() {
        return length;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lcafType, reserved1, reserved2, flag, length);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispLcafAddress) {
            final LispLcafAddress other = (LispLcafAddress) obj;
            return Objects.equals(this.lcafType, other.lcafType) &&
                   Objects.equals(this.reserved1, other.reserved1) &&
                   Objects.equals(this.reserved2, other.reserved2) &&
                   Objects.equals(this.flag, other.flag) &&
                   Objects.equals(this.length, other.length);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("lcafType", lcafType)
                .add("reserved1", reserved1)
                .add("reserved2", reserved2)
                .add("flag", flag)
                .add("length", length)
                .toString();
    }
}
