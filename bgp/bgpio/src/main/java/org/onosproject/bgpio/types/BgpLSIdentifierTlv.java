/*
 * Copyright 2015 Open Networking Laboratory
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    protected static final Logger log = LoggerFactory.getLogger(BgpLSIdentifierTlv.class);

    public static final short TYPE = 513;
    public static final short LENGTH = 4;

    private final int bgpLSIdentifier;

    /**
     * Constructor to initialize bgpLSIdentifier.
     *
     * @param bgpLSIdentifier BGPLS-Identifier
     */
    public BgpLSIdentifierTlv(int bgpLSIdentifier) {
        this.bgpLSIdentifier = bgpLSIdentifier;
    }

    /**
     * Returns object of this class with specified rbgpLSIdentifier.
     *
     * @param bgpLSIdentifier BGPLS-Identifier
     * @return BGPLS-Identifier
     */
    public static BgpLSIdentifierTlv of(final int bgpLSIdentifier) {
        return new BgpLSIdentifierTlv(bgpLSIdentifier);
    }

    /**
     * Returns opaque value of BGPLS-Identifier.
     *
     * @return opaque value of BGPLS-Identifier
     */
    public int getBgpLSIdentifier() {
        return bgpLSIdentifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bgpLSIdentifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpLSIdentifierTlv) {
            BgpLSIdentifierTlv other = (BgpLSIdentifierTlv) obj;
            return Objects.equals(bgpLSIdentifier, other.bgpLSIdentifier);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeInt(bgpLSIdentifier);
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
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("Value", bgpLSIdentifier)
                .toString();
    }
}