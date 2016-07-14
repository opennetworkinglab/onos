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

import com.google.common.base.MoreObjects;

/**
 * Provides BGPLSIdentifier Tlv which contains opaque value (32 Bit BGPLS-Identifier).
 */
public class BgpLSIdentifierTlv implements BgpValueType {

    /* Reference :draft-ietf-idr-ls-distribution-11
     *  0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |           Type= 513            |             Length=4         |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                    opaque value (32 Bit BGPLS-Identifier)     |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    public static final short TYPE = 513;
    public static final short LENGTH = 4;

    private final int bgpLsIdentifier;

    /**
     * Constructor to initialize bgpLsIdentifier.
     *
     * @param bgpLsIdentifier BGPLS-Identifier
     */
    public BgpLSIdentifierTlv(int bgpLsIdentifier) {
        this.bgpLsIdentifier = bgpLsIdentifier;
    }

    /**
     * Returns object of this class with specified bgpLsIdentifier.
     *
     * @param bgpLsIdentifier BGPLS-Identifier
     * @return BGPLS-Identifier
     */
    public static BgpLSIdentifierTlv of(final int bgpLsIdentifier) {
        return new BgpLSIdentifierTlv(bgpLsIdentifier);
    }

    /**
     * Returns opaque value of BGPLS-Identifier.
     *
     * @return opaque value of BGPLS-Identifier
     */
    public int getBgpLsIdentifier() {
        return bgpLsIdentifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bgpLsIdentifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpLSIdentifierTlv) {
            BgpLSIdentifierTlv other = (BgpLSIdentifierTlv) obj;
            return Objects.equals(bgpLsIdentifier, other.bgpLsIdentifier);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeInt(bgpLsIdentifier);
        return c.writerIndex() - iLenStartIndex;
    }

     /**
     * Reads the channel buffer and parses BGPLS Identifier TLV.
     *
     * @param cb ChannelBuffer
     * @return object of BGPLSIdentifierTlv
     */
    public static BgpLSIdentifierTlv read(ChannelBuffer cb) {
        return BgpLSIdentifierTlv.of(cb.readInt());
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public int compareTo(Object o) {
        if (this.equals(o)) {
            return 0;
        }
        return ((Integer) (this.bgpLsIdentifier)).compareTo((Integer) (((BgpLSIdentifierTlv) o).bgpLsIdentifier));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("Value", bgpLsIdentifier)
                .toString();
    }
}