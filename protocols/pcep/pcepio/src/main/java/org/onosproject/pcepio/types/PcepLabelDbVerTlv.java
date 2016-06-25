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
 * Provides CEP LABEL DB VERSION TLV which contains LSP State DB Version  (32 Bit ).
 */
public class PcepLabelDbVerTlv implements PcepValueType {

    /*                  PCEP LABEL DB VERSION TLV format

    Reference : draft-ietf-pce-stateful-sync-optimizations-02, section 3.3.1
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |           Type=23             |            Length=8           |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                      LSP State DB Version                     |
    |                                                               |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     */
    protected static final Logger log = LoggerFactory.getLogger(PcepLabelDbVerTlv.class);

    public static final short TYPE = 34;
    public static final short LENGTH = 8;
    private final long rawValue;

    /**
     * constructor to initialize rawValue.
     *
     * @param rawValue of Pcep Label Db Version Tlv
     */
    public PcepLabelDbVerTlv(final long rawValue) {
        log.debug("PcepLabelDbVerTlv");
        this.rawValue = rawValue;
    }

    /**
     * Returns newly created PcepLabelDbVerTlv object.
     *
     * @param raw LSP State DB Version
     * @return object of PcepLabelDbVerTlv
     */
    public static PcepLabelDbVerTlv of(final long raw) {
        return new PcepLabelDbVerTlv(raw);
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    /**
     * Returns LSP State DB Version.
     * @return raw value
     */
    public long getLong() {
        return rawValue;
    }

    @Override
    public short getLength() {
        return LENGTH;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PcepLabelDbVerTlv) {
            PcepLabelDbVerTlv other = (PcepLabelDbVerTlv) obj;
            return Objects.equals(this.rawValue, other.rawValue);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeLong(rawValue);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of PcepLabelDbVerTlv.
     *
     * @param c input channel buffer
     * @return object of PcepLabelDbVerTlv
     */
    public static PcepLabelDbVerTlv read(ChannelBuffer c) {
        return PcepLabelDbVerTlv.of(c.readLong());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("Value", rawValue)
                .toString();
    }
}
