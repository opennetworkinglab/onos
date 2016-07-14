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
 * Provides StatefulLspErrorCodeTlv.
 */
public class StatefulLspErrorCodeTlv implements PcepValueType {

    /*                  LSP-ERROR-CODE TLV format
     *
     * Reference :PCEP Extensions for Stateful PCE draft-ietf-pce-stateful-pce-10
     *

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |           Type=20             |            Length=4           |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                          LSP Error Code                       |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     */

    protected static final Logger log = LoggerFactory.getLogger(StatefulLspErrorCodeTlv.class);

    public static final short TYPE = 20;
    public static final short LENGTH = 4;
    private final int rawValue;

    /**
     * Constructor to initialize raw Value.
     *
     * @param rawValue lsp error code value
     */
    public StatefulLspErrorCodeTlv(int rawValue) {
        this.rawValue = rawValue;
    }

    /**
     * Creates object of StatefulLspErrorCodeTlv.
     *
     * @param raw lsp error code value
     * @return object of StatefulLspErrorCodeTlv
     */
    public static StatefulLspErrorCodeTlv of(int raw) {
        return new StatefulLspErrorCodeTlv(raw);
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    /**
     * Returns lsp error code value.
     *
     * @return lsp error code value
     */
    public int getInt() {
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
        if (obj instanceof StatefulLspErrorCodeTlv) {
            StatefulLspErrorCodeTlv other = (StatefulLspErrorCodeTlv) obj;
            return Objects.equals(this.rawValue, other.rawValue);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeInt(rawValue);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of StatefulLspErrorCodeTlv.
     *
     * @param c of type channel buffer
     * @return object of StatefulLspErrorCodeTlv
     */
    public static StatefulLspErrorCodeTlv read(ChannelBuffer c) {
        return StatefulLspErrorCodeTlv.of(c.readInt());
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
