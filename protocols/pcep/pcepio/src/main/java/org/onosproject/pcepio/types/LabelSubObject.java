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
 * LabelSubObject: Provides a LabelSubObject.
 */
public class LabelSubObject implements PcepValueType {

    /* Reference : RFC 3209
     * LABEL Sub Object
     *
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |     Type      |     Length    |    Flags      |   C-Type      |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |       Contents of Label Object                                |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    protected static final Logger log = LoggerFactory.getLogger(LabelSubObject.class);

    public static final short TYPE = 0x03;
    public static final short LENGTH = 8;
    private final byte flags;
    private final byte cType;
    private final int contents;

    /**
     * constructor to initialize parameters for LabelSubObject.
     *
     * @param flags flags
     * @param cType C-Type
     * @param contents Contents of label object
     */
    public LabelSubObject(byte flags, byte cType, int contents) {
        this.flags = flags;
        this.cType = cType;
        this.contents = contents;
    }

    /**
     * Return an object of LabelSubObject.
     *
     * @param flags flags
     * @param cType C-type
     * @param contents contents of label objects
     * @return object of LabelSubObject
     */
    public static LabelSubObject of(byte flags, byte cType, int contents) {
        return new LabelSubObject(flags, cType, contents);
    }

    /**
     * Returns Flags.
     *
     * @return flags
     */
    public byte getFlags() {
        return flags;
    }

    /**
     * Returns cType.
     *
     * @return cType
     */
    public byte getCtype() {
        return cType;
    }

    /**
     * Returns contents.
     *
     * @return contents
     */
    public int getContents() {
        return contents;
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
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
        return Objects.hash(flags, cType, contents);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LabelSubObject) {
            LabelSubObject other = (LabelSubObject) obj;
            return Objects.equals(this.flags, other.flags) && Objects.equals(this.cType, other.cType)
                    && Objects.equals(this.contents, other.contents);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeByte(flags);
        c.writeByte(cType);
        c.writeByte(contents);
        return c.writerIndex() - iStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of LabelSubObject.
     *
     * @param c type of channel buffer
     * @return object of LabelSubObject
     */
    public static PcepValueType read(ChannelBuffer c) {
        byte flags = c.readByte();
        byte cType = c.readByte();
        int contents = c.readInt();
        return new LabelSubObject(flags, cType, contents);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("type", TYPE)
                .add("Length", LENGTH)
                .add("flags", flags)
                .add("C-type", cType)
                .add("contents", contents)
                .toString();
    }
}
