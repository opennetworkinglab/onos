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
package org.onosproject.pcepio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PcepSetup type tlv.
 */
public class PathSetupTypeTlv implements PcepValueType {

    /*
       Reference : draft-sivabalan-pce-lsp-setup-type-02.

         0                   1                   2                     3
          0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         | Type                          | Length                        |
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         | Reserved                                      | PST           |
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                     Figure 1: PATH-SETUP-TYPE TLV

     */
    protected static final Logger log = LoggerFactory.getLogger(PathSetupTypeTlv.class);

    public static final short TYPE = 28;
    public static final short LENGTH = 4;

    private final byte pst;
    private final int rawValue;
    private final boolean isRawValueSet;

    /**
     * Constructor to initialize parameters for path setup type tlv.
     *
     * @param rawValue parameter for path setup type tlv
     */
    public PathSetupTypeTlv(final int rawValue) {
        this.rawValue = rawValue;
        this.isRawValueSet = true;
        this.pst = (byte) rawValue;
    }

    /**
     * Constructor to initialize pst.
     *
     * @param pst PST
     */
    public PathSetupTypeTlv(byte pst) {
        this.pst = pst;
        this.rawValue = 0;
        this.isRawValueSet = false;
    }

    /**
     * Returns Object of path setup type tlv.
     *
     * @param raw parameter for path setup type tlv
     * @return object of PathSetupTypeTlv
     */
    public static PathSetupTypeTlv of(final int raw) {
        return new PathSetupTypeTlv(raw);
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    /**
     * Returns parameters for path setup type tlv.
     *
     * @return parameters for path setup type tlv
     */
    public int getInt() {
        return rawValue;
    }

    /**
     * Returns the pst value.
     *
     * @return pst value
     */
    public byte getPst() {
        return pst;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public short getLength() {
        return LENGTH;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pst);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PathSetupTypeTlv) {
            PathSetupTypeTlv other = (PathSetupTypeTlv) obj;
            return Objects.equals(this.pst, other.pst);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeInt(pst);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Returns the object of type PathSetupTypeTlv.
     *
     * @param c is type Channel buffer
     * @return object of PathSetupTypeTlv
     */
    public static PathSetupTypeTlv read(ChannelBuffer c) {
        return PathSetupTypeTlv.of(c.readInt());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("PST", pst)
                .toString();
    }
}
