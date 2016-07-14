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
package org.onosproject.bgpio.types.attr;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.util.Constants;
import org.onosproject.bgpio.util.Validation;

import com.google.common.base.MoreObjects;

/**
 * Implements BGP link protection type attribute.
 */
public final class BgpLinkAttrProtectionType implements BgpValueType {
    public static final int ATTRLINK_PROTECTIONTYPE = 1093;
    public static final int LINK_PROTECTION_LEN = 2;

    private byte linkProtectionType;

    /**
     * Enum to provide Link protection types.
     */
    public enum ProtectionType {
        EXTRA_TRAFFIC(1), UNPROTECTED(2), SHARED(4), DEDICATED_ONE_ISTO_ONE(8),
        DEDICATED_ONE_PLUS_ONE(0x10), ENHANCED(0x20), RESERVED(0x40);
        int value;

        /**
         * Assign val with the value as the link protection type.
         *
         * @param val link protection
         */
        ProtectionType(int val) {
            value = val;
        }

        /**
         * Returns value of link protection type.
         *
         * @return link protection type
         */
        public byte type() {
            return (byte) value;
        }
    }

    /**
     * Constructor to initialize the value.
     *
     * @param linkProtectionType link protection type
     */
    public BgpLinkAttrProtectionType(byte linkProtectionType) {
        this.linkProtectionType = linkProtectionType;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param linkProtectionType link protection type
     * @return object of BgpLinkAttrProtectionType
     */
    public static BgpLinkAttrProtectionType of(byte linkProtectionType) {
        return new BgpLinkAttrProtectionType(linkProtectionType);
    }

    /**
     * Reads the BGP link attributes protection type.
     *
     * @param cb Channel buffer
     * @return object of type BgpLinkAttrProtectionType
     * @throws BgpParseException while parsing BgpLinkAttrProtectionType
     */
    public static BgpLinkAttrProtectionType read(ChannelBuffer cb)
            throws BgpParseException {
        short lsAttrLength = cb.readShort();

        if ((lsAttrLength != LINK_PROTECTION_LEN) || (cb.readableBytes() < lsAttrLength)) {
            Validation
                    .validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR, lsAttrLength);
        }

        byte linkProtectionType = cb.readByte();
        byte reserved = cb.readByte();

        return BgpLinkAttrProtectionType.of(linkProtectionType);
    }

    /**
     * Returns Link Protection Type.
     *
     * @return Link Protection Type
     * @throws BgpParseException when failed to parse link protection type
     */
    public ProtectionType protectionType() throws BgpParseException {
        switch (linkProtectionType) {
        case Constants.EXTRA_TRAFFIC:
            return ProtectionType.EXTRA_TRAFFIC;
        case Constants.UNPROTECTED:
            return ProtectionType.UNPROTECTED;
        case Constants.SHARED:
            return ProtectionType.SHARED;
        case Constants.DEDICATED_ONE_ISTO_ONE:
            return ProtectionType.DEDICATED_ONE_ISTO_ONE;
        case Constants.DEDICATED_ONE_PLUS_ONE:
            return ProtectionType.DEDICATED_ONE_PLUS_ONE;
        case Constants.ENHANCED:
            return ProtectionType.ENHANCED;
        case Constants.RESERVED:
            return ProtectionType.RESERVED;
        default:
            throw new BgpParseException("Got another type " + linkProtectionType);
        }
    }

    @Override
    public short getType() {
        return ATTRLINK_PROTECTIONTYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(linkProtectionType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpLinkAttrProtectionType) {
            BgpLinkAttrProtectionType other = (BgpLinkAttrProtectionType) obj;
            return Objects.equals(linkProtectionType, other.linkProtectionType);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        // TODO This will be implemented in the next version
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("linkProtectionType", linkProtectionType)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
