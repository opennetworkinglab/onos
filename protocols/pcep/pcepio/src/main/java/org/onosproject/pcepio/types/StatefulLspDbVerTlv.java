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
 * Provides StatefulLspDbVerTlv.
 */
public class StatefulLspDbVerTlv implements PcepValueType {

    /*                  LSP-DB-VERSION TLV format
     *
     * Reference : Optimizations of Label Switched Path State Synchronization Procedures
                           for a Stateful PCE draft-ietf-pce-stateful-sync-optimizations-02
     *
     *

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |           Type=23             |            Length=8           |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                      LSP State DB Version                     |
    |                                                               |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     */
    protected static final Logger log = LoggerFactory.getLogger(StatefulLspDbVerTlv.class);

    public static final short TYPE = 23;
    public static final short LENGTH = 8;
    private final long rawValue;

    /**
     * Constructor to initialize rawValue.
     *
     * @param rawValue value
     */
    public StatefulLspDbVerTlv(final long rawValue) {
        this.rawValue = rawValue;
    }

    /**
     * Returns object of StatefulLspDbVerTlv.
     *
     * @param raw is LSP State DB Version
     * @return object of StatefulLspDbVerTlv
     */
    public static StatefulLspDbVerTlv of(final long raw) {
        return new StatefulLspDbVerTlv(raw);
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    /**
     * Returns LSP State DB Version.
     *
     * @return rawValue value
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
        if (obj instanceof StatefulLspDbVerTlv) {
            StatefulLspDbVerTlv other = (StatefulLspDbVerTlv) obj;
            return Objects.equals(this.rawValue, other.rawValue);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeLong(rawValue);
        return c.writerIndex();
    }

    /**
     * Reads the channel buffer and returns object of StatefulLspDbVerTlv.
     *
     * @param c input channel buffer
     * @return object of StatefulLspDbVerTlv
     */
    public static StatefulLspDbVerTlv read(ChannelBuffer c) {
        return StatefulLspDbVerTlv.of(c.readLong());
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
